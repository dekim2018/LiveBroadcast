package com.livebroadcast.auth.app.port.out;

import java.util.Optional;

import com.livebroadcast.auth.domain.IssuedTokens;
import com.livebroadcast.auth.domain.RefreshTokenClaims;
import com.livebroadcast.auth.domain.User;

public interface TokenIssuerPort {

    IssuedTokens issue(User user);

    /** 서명, 만료, 토큰 종류를 검증하고 실패하면 빈 값을 돌려준다. */
    Optional<RefreshTokenClaims> parseRefreshToken(String refreshToken);
}
