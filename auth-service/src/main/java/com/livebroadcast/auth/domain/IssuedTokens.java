package com.livebroadcast.auth.domain;

import java.time.Duration;

public record IssuedTokens(
        String accessToken,
        Duration accessTokenTtl,
        String refreshToken,
        String refreshTokenId,
        Duration refreshTokenTtl
) {
}
