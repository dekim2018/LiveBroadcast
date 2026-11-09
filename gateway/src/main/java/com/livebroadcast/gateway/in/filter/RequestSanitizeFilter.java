package com.livebroadcast.gateway.in.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 1. 외부 요청이 서비스 간 내부 인증 헤더(X-Internal-Key)를 위조해 넣지 못하도록 제거한다.
 * 2. 요청 추적용 X-Request-Id 를 붙이고 간단한 접근 로그를 남긴다.
 */
@Component
public class RequestSanitizeFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestSanitizeFilter.class);
    private static final String REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-Internal-Key");
                    h.set(REQUEST_ID, requestId);
                })
                .build();
        long start = System.currentTimeMillis();
        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> log.info("{} {} {} -> {} ({}ms)",
                        requestId,
                        request.getMethod(),
                        request.getPath().value(),
                        exchange.getResponse().getStatusCode(),
                        System.currentTimeMillis() - start));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
