package com.livebroadcast.common.security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 로그아웃된 Access Token 의 jti 를 남은 만료시간 동안 Redis 에 보관한다.
 * 모든 서비스가 같은 Redis 를 바라보므로 로그아웃 즉시 전 서비스에서 토큰이 무효화된다.
 */
public class TokenBlacklist {

    private static final String PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void add(String jti, Duration ttl) {
        if (jti == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redis.opsForValue().set(PREFIX + jti, "1", ttl);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
