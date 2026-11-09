package com.livebroadcast.broadcast.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.livebroadcast.common.security.ApiSecuritySupport;

/**
 * 방송 목록/상세/녹화 조회는 비로그인 허용, 그 외(등록, 수정, 삭제, 송출 제어)는 로그인 필수.
 * 소유자 검증은 도메인(Broadcast.ensureOwner)에서 한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecuritySupport support) throws Exception {
        support.applyDefaults(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/broadcasts/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/broadcasts/*/live/session").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/broadcasts", "/api/broadcasts/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recordings", "/api/recordings/*").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
