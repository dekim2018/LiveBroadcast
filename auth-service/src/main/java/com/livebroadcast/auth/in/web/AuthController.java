package com.livebroadcast.auth.in.web;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.auth.app.port.in.LoginUseCase;
import com.livebroadcast.auth.app.port.in.LoginUseCase.LoginResult;
import com.livebroadcast.auth.app.port.in.SignUpUseCase;
import com.livebroadcast.auth.app.port.in.TokenUseCase;
import com.livebroadcast.auth.app.port.in.UserQueryUseCase;
import com.livebroadcast.auth.in.web.dto.LoginRequest;
import com.livebroadcast.auth.in.web.dto.SignUpRequest;
import com.livebroadcast.auth.in.web.dto.TestLoginRequest;
import com.livebroadcast.auth.in.web.dto.TokenResponse;
import com.livebroadcast.auth.in.web.dto.UserResponse;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final SignUpUseCase signUpUseCase;
    private final LoginUseCase loginUseCase;
    private final TokenUseCase tokenUseCase;
    private final UserQueryUseCase userQueryUseCase;
    private final boolean secureCookie;

    public AuthController(SignUpUseCase signUpUseCase, LoginUseCase loginUseCase, TokenUseCase tokenUseCase,
                          UserQueryUseCase userQueryUseCase,
                          @Value("${app.cookie.secure:false}") boolean secureCookie) {
        this.signUpUseCase = signUpUseCase;
        this.loginUseCase = loginUseCase;
        this.tokenUseCase = tokenUseCase;
        this.userQueryUseCase = userQueryUseCase;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        var user = signUpUseCase.signUp(
                new SignUpUseCase.SignUpCommand(request.username(), request.password(), request.nickname()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(loginUseCase.login(request.username(), request.password()));
    }

    @PostMapping("/test-login")
    public ResponseEntity<TokenResponse> testLogin(@Valid @RequestBody TestLoginRequest request) {
        return tokenResponse(loginUseCase.testLogin(request.username()));
    }

    @GetMapping("/test-accounts")
    public List<UserResponse> testAccounts() {
        return userQueryUseCase.getTestAccounts().stream().map(UserResponse::from).toList();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return tokenResponse(tokenUseCase.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt,
                                       @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        tokenUseCase.logout(
                jwt == null ? null : jwt.getId(),
                jwt == null ? null : jwt.getExpiresAt(),
                refreshToken
        );
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public UserResponse me(@LoginUser AuthUser authUser) {
        return UserResponse.from(userQueryUseCase.getUser(authUser.id()));
    }

    private ResponseEntity<TokenResponse> tokenResponse(LoginResult result) {
        var tokens = result.tokens();
        TokenResponse body = new TokenResponse(
                tokens.accessToken(),
                tokens.accessTokenTtl().toSeconds(),
                UserResponse.from(result.user())
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), tokens.refreshTokenTtl()).toString())
                .body(body);
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
