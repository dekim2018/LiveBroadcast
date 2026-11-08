package com.livebroadcast.common.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class CommonSecurityConfig implements WebMvcConfigurer {

    @Bean
    public TokenBlacklist tokenBlacklist(StringRedisTemplate redisTemplate) {
        return new TokenBlacklist(redisTemplate);
    }

    @Bean
    public JwtDecoder accessTokenDecoder(JwtProperties properties, TokenBlacklist blacklist) {
        return JwtKeys.decoder(properties, blacklist, JwtClaims.TYPE_ACCESS);
    }

    /**
     * InternalApiKeyFilter 는 서블릿 필터 빈으로 등록하지 않고 시큐리티 체인 안에만 넣는다.
     * (빈으로 등록하면 서블릿 컨테이너에도 자동 등록되어 두 번 실행된다)
     */
    @Bean
    public ApiSecuritySupport apiSecuritySupport(JwtDecoder accessTokenDecoder,
                                                 @Value("${app.internal.api-key}") String internalApiKey) {
        return new ApiSecuritySupport(accessTokenDecoder, new InternalApiKeyFilter(internalApiKey));
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginUserArgumentResolver());
    }
}
