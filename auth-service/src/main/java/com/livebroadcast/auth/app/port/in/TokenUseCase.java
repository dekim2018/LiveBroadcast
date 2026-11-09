package com.livebroadcast.auth.app.port.in;

import java.time.Instant;

public interface TokenUseCase {

    LoginUseCase.LoginResult refresh(String refreshToken);

    /**
     * @param accessTokenId  현재 Access Token 의 jti (없으면 null)
     * @param accessExpiresAt 현재 Access Token 의 만료 시각 (없으면 null)
     * @param refreshToken   쿠키의 Refresh Token (없으면 null)
     */
    void logout(String accessTokenId, Instant accessExpiresAt, String refreshToken);
}
