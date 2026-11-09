package com.livebroadcast.auth.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.auth.app.TestAccountProperties;
import com.livebroadcast.auth.app.port.in.LoginUseCase;
import com.livebroadcast.auth.app.port.in.SignUpUseCase;
import com.livebroadcast.auth.app.port.in.TokenUseCase;
import com.livebroadcast.auth.app.port.in.UserQueryUseCase;
import com.livebroadcast.auth.app.port.out.PasswordHasherPort;
import com.livebroadcast.auth.app.port.out.TokenIssuerPort;
import com.livebroadcast.auth.app.port.out.TokenStorePort;
import com.livebroadcast.auth.app.port.out.UserRepositoryPort;
import com.livebroadcast.auth.domain.AuthErrors;
import com.livebroadcast.auth.domain.IssuedTokens;
import com.livebroadcast.auth.domain.RefreshTokenClaims;
import com.livebroadcast.auth.domain.User;
import com.livebroadcast.common.error.DomainException;

@Service
public class AuthService implements SignUpUseCase, LoginUseCase, TokenUseCase, UserQueryUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;
    private final TokenStorePort tokenStore;
    private final TestAccountProperties testAccounts;
    private final Clock clock;

    public AuthService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher,
                       TokenIssuerPort tokenIssuer, TokenStorePort tokenStore,
                       TestAccountProperties testAccounts, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.tokenStore = tokenStore;
        this.testAccounts = testAccounts;
        this.clock = clock;
    }

    @Override
    @Transactional
    public User signUp(SignUpCommand command) {
        validatePassword(command.password());
        if (userRepository.existsByUsername(command.username())) {
            throw AuthErrors.duplicateUsername();
        }
        if (command.nickname() != null && userRepository.existsByNickname(command.nickname().strip())) {
            throw AuthErrors.duplicateNickname();
        }
        User user = User.register(
                command.username(),
                passwordHasher.hash(command.password()),
                command.nickname(),
                clock.instant()
        );
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .filter(u -> passwordHasher.matches(password, u.getPasswordHash()))
                .orElseThrow(AuthErrors::invalidCredentials);
        return issueFor(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult testLogin(String username) {
        if (!testAccounts.isTestAccount(username)) {
            throw AuthErrors.testLoginDisabled();
        }
        User user = userRepository.findByUsername(username).orElseThrow(AuthErrors::userNotFound);
        return issueFor(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AuthErrors.invalidRefreshToken();
        }
        RefreshTokenClaims claims = tokenIssuer.parseRefreshToken(refreshToken)
                .orElseThrow(AuthErrors::invalidRefreshToken);
        // 한 번 사용한 Refresh Token 은 즉시 폐기한다(Rotation). 탈취된 토큰의 재사용을 막는다.
        if (!tokenStore.consumeRefreshToken(claims.userId(), claims.tokenId())) {
            throw AuthErrors.invalidRefreshToken();
        }
        User user = userRepository.findById(claims.userId()).orElseThrow(AuthErrors::invalidRefreshToken);
        return issueFor(user);
    }

    @Override
    public void logout(String accessTokenId, Instant accessExpiresAt, String refreshToken) {
        if (accessTokenId != null && accessExpiresAt != null) {
            Duration remaining = Duration.between(clock.instant(), accessExpiresAt);
            tokenStore.blacklistAccessToken(accessTokenId, remaining);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenIssuer.parseRefreshToken(refreshToken)
                    .ifPresent(c -> tokenStore.deleteRefreshToken(c.userId(), c.tokenId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(AuthErrors::userNotFound);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getTestAccounts() {
        if (!testAccounts.enabled()) {
            return List.of();
        }
        return testAccounts.accounts().stream()
                .map(a -> userRepository.findByUsername(a.username()))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    /** 서버 기동 시 테스트 계정이 없으면 만든다. */
    @Transactional
    public void ensureTestAccounts() {
        if (!testAccounts.enabled()) {
            return;
        }
        for (TestAccountProperties.Account account : testAccounts.accounts()) {
            if (!userRepository.existsByUsername(account.username())) {
                userRepository.save(User.register(
                        account.username(),
                        passwordHasher.hash(testAccounts.password()),
                        account.nickname(),
                        clock.instant()
                ));
            }
        }
    }

    private LoginResult issueFor(User user) {
        IssuedTokens tokens = tokenIssuer.issue(user);
        tokenStore.saveRefreshToken(user.getId(), tokens.refreshTokenId(), tokens.refreshTokenTtl());
        return new LoginResult(user, tokens);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw DomainException.badRequest("INVALID_PASSWORD", "비밀번호는 8~64자여야 합니다.");
        }
    }
}
