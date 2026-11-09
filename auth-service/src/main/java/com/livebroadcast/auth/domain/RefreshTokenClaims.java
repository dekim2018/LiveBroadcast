package com.livebroadcast.auth.domain;

import java.time.Instant;

public record RefreshTokenClaims(Long userId, String tokenId, Instant expiresAt) {
}
