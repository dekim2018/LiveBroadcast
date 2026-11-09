package com.livebroadcast.auth.out.security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.auth.app.port.out.TokenStorePort;
import com.livebroadcast.common.security.TokenBlacklist;

@Component
public class RedisTokenStore implements TokenStorePort {

    private static final String REFRESH_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redis;
    private final TokenBlacklist blacklist;

    public RedisTokenStore(StringRedisTemplate redis, TokenBlacklist blacklist) {
        this.redis = redis;
        this.blacklist = blacklist;
    }

    @Override
    public void saveRefreshToken(Long userId, String tokenId, Duration ttl) {
        redis.opsForValue().set(key(userId, tokenId), "1", ttl);
    }

    @Override
    public boolean consumeRefreshToken(Long userId, String tokenId) {
        // DELETE 는 원자적이므로 동시에 같은 토큰으로 두 번 갱신 요청이 와도 한 번만 성공한다.
        return Boolean.TRUE.equals(redis.delete(key(userId, tokenId)));
    }

    @Override
    public void deleteRefreshToken(Long userId, String tokenId) {
        redis.delete(key(userId, tokenId));
    }

    @Override
    public void blacklistAccessToken(String tokenId, Duration remainingTtl) {
        blacklist.add(tokenId, remainingTtl);
    }

    private String key(Long userId, String tokenId) {
        return REFRESH_PREFIX + userId + ":" + tokenId;
    }
}
