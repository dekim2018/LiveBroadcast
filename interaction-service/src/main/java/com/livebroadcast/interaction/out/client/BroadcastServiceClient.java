package com.livebroadcast.interaction.out.client;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.error.ErrorType;
import com.livebroadcast.common.security.InternalApiKeyFilter;
import com.livebroadcast.interaction.app.port.out.BroadcastQueryPort;
import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;

/**
 * broadcast-service 의 내부 API 를 호출해서 방송 존재 여부와 방송자(후원 대상)를 확인한다.
 */
@Component
public class BroadcastServiceClient implements BroadcastQueryPort {

    private final RestClient restClient;

    public BroadcastServiceClient(@Value("${services.broadcast-url}") String baseUrl,
                                  @Value("${app.internal.api-key}") String internalApiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
    }

    @Override
    public Optional<BroadcastInfo> findBroadcast(Long broadcastId) {
        try {
            return restClient.get()
                    .uri("/internal/broadcasts/{id}", broadcastId)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                            return Optional.<BroadcastInfo>empty();
                        }
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw unavailable();
                        }
                        BroadcastDto dto = response.bodyTo(BroadcastDto.class);
                        if (dto == null) {
                            throw unavailable();
                        }
                        return Optional.of(new BroadcastInfo(dto.id(), dto.ownerId(), dto.ownerNickname(),
                                dto.title(), dto.status()));
                    });
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private static DomainException unavailable() {
        return new DomainException(ErrorType.BAD_GATEWAY, "BROADCAST_SERVICE_UNAVAILABLE",
                "방송 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BroadcastDto(Long id, Long ownerId, String ownerNickname, String title, String status) {
    }
}
