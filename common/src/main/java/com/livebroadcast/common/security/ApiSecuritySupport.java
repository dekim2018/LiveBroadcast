package com.livebroadcast.common.security;

import java.util.List;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

/**
 * 서비스마다 반복되는 무상태(Stateless) REST API 보안 설정을 한 곳에 모은다.
 * 각 서비스는 이 설정 위에 자기 URL 규칙(authorizeHttpRequests)만 추가하면 된다.
 */
public class ApiSecuritySupport {

    private final JwtDecoder accessTokenDecoder;
    private final InternalApiKeyFilter internalApiKeyFilter;
    private final JsonSecurityErrorHandler errorHandler = new JsonSecurityErrorHandler();

    public ApiSecuritySupport(JwtDecoder accessTokenDecoder, InternalApiKeyFilter internalApiKeyFilter) {
        this.accessTokenDecoder = accessTokenDecoder;
        this.internalApiKeyFilter = internalApiKeyFilter;
    }

    public HttpSecurity applyDefaults(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of(new SimpleGrantedAuthority("ROLE_USER")));

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(accessTokenDecoder).jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .addFilterBefore(internalApiKeyFilter, BearerTokenAuthenticationFilter.class);
    }
}
