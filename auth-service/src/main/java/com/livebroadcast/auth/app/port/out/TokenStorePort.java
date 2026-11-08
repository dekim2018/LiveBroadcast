package com.livebroadcast.auth.app.port.out;

import java.time.Duration;

/**
 * Refresh Token 화이트리스트와 Access Token 블랙리스트를 관리하는 저장소(Redis).
 */
public interface TokenStorePort {

    void saveRefreshToken(Long userId, String tokenId, Duration ttl);

    /** 존재하면 삭제하고 true. 이미 사용됐거나 없으면 false (Refresh Token 재사용 방지). */
    boolean consumeRefreshToken(Long userId, String tokenId);

    void deleteRefreshToken(Long userId, String tokenId);

    void blacklistAccessToken(String tokenId, Duration remainingTtl);
}
