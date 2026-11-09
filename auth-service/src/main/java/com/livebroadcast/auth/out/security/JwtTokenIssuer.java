package com.livebroadcast.auth.out.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.livebroadcast.auth.app.port.out.TokenIssuerPort;
import com.livebroadcast.auth.domain.IssuedTokens;
import com.livebroadcast.auth.domain.RefreshTokenClaims;
import com.livebroadcast.auth.domain.User;
import com.livebroadcast.common.security.JwtClaims;
import com.livebroadcast.common.security.JwtKeys;
import com.livebroadcast.common.security.JwtProperties;
import com.livebroadcast.common.security.TokenBlacklist;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Component
public class JwtTokenIssuer implements TokenIssuerPort {

    private final JwtProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder refreshDecoder;
    private final Clock clock;

    public JwtTokenIssuer(JwtProperties properties, TokenBlacklist blacklist, Clock clock) {
        this.properties = properties;
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(JwtKeys.secretKey(properties)));
        this.refreshDecoder = JwtKeys.decoder(properties, blacklist, JwtClaims.TYPE_REFRESH);
        this.clock = clock;
    }

    @Override
    public IssuedTokens issue(User user) {
        Instant now = clock.instant();
        String accessId = UUID.randomUUID().toString();
        String refreshId = UUID.randomUUID().toString();

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .id(accessId)
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim(JwtClaims.NICKNAME, user.getNickname())
                .claim(JwtClaims.TOKEN_TYPE, JwtClaims.TYPE_ACCESS)
                .build();

        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .id(refreshId)
                .issuedAt(now)
                .expiresAt(now.plus(properties.refreshTokenTtl()))
                .claim(JwtClaims.TOKEN_TYPE, JwtClaims.TYPE_REFRESH)
                .build();

        return new IssuedTokens(
                encode(accessClaims),
                properties.accessTokenTtl(),
                encode(refreshClaims),
                refreshId,
                properties.refreshTokenTtl()
        );
    }

    @Override
    public Optional<RefreshTokenClaims> parseRefreshToken(String refreshToken) {
        try {
            Jwt jwt = refreshDecoder.decode(refreshToken);
            return Optional.of(new RefreshTokenClaims(Long.valueOf(jwt.getSubject()), jwt.getId(), jwt.getExpiresAt()));
        } catch (JwtException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
