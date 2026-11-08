package com.livebroadcast.common.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

public final class JwtKeys {

    private JwtKeys() {
    }

    public static SecretKey secretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * 지정한 토큰 타입(access/refresh)만 통과시키고, 블랙리스트에 오른 토큰은 거부하는 디코더를 만든다.
     */
    public static NimbusJwtDecoder decoder(JwtProperties properties, TokenBlacklist blacklist, String expectedType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> typeValidator = jwt -> expectedType.equals(jwt.getClaimAsString(JwtClaims.TOKEN_TYPE))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "토큰 종류가 올바르지 않습니다.", null));
        OAuth2TokenValidator<Jwt> blacklistValidator = jwt -> jwt.getId() != null && blacklist.isBlacklisted(jwt.getId())
                ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "로그아웃된 토큰입니다.", null))
                : OAuth2TokenValidatorResult.success();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                typeValidator,
                blacklistValidator
        ));
        return decoder;
    }
}
