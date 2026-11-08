package com.livebroadcast.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 모든 서비스가 같은 값을 공유해야 하는 JWT 설정.
 * secret 은 HS256 서명용이며 최소 32바이트 이상이어야 한다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret 은 32바이트 이상이어야 합니다.");
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "live-broadcast";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(30);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(14);
        }
    }
}
