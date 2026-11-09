package com.livebroadcast.interaction.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.livebroadcast.common.security.ApiSecuritySupport;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecuritySupport support) throws Exception {
        support.applyDefaults(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        // SSE 는 비동기 디스패치가 한 번 더 일어나므로 경로 기준으로 허용한다.
                        .requestMatchers(HttpMethod.GET,
                                "/api/broadcasts/*/comments",
                                "/api/broadcasts/*/donations",
                                "/api/broadcasts/*/donations/summary",
                                "/api/broadcasts/*/events",
                                "/api/donations/config").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/donations/webhook").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
