package com.livebroadcast.broadcast.out.media;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.port.out.MediaServerPort;
import com.livebroadcast.broadcast.domain.RecordedSegment;
import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.error.ErrorType;

/**
 * MediaMTX Control API(v3)와 Playback 서버를 호출하는 아웃바운드 어댑터.
 * 참고: https://github.com/bluenviron/mediamtx (MIT License)
 */
@Component
public class MediaMtxAdapter implements MediaServerPort {

    private static final Logger log = LoggerFactory.getLogger(MediaMtxAdapter.class);

    /** 송출 연결 종류별 강제 종료 API */
    private static final Map<String, String> KICK_ENDPOINTS = Map.of(
            "webRTCSession", "/v3/webrtcsessions/kick/",
            "rtmpConn", "/v3/rtmpconns/kick/",
            "rtspSession", "/v3/rtspsessions/kick/",
            "srtConn", "/v3/srtconns/kick/"
    );

    private final RestClient restClient;
    private final MediaProperties properties;

    public MediaMtxAdapter(MediaProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void kickPublisher(String mediaPath) {
        try {
            PathInfo info = restClient.get()
                    .uri(URI.create(properties.apiUrl() + "/v3/paths/get/" + mediaPath))
                    .retrieve()
                    .body(PathInfo.class);
            if (info == null || info.source() == null || info.source().id() == null) {
                return;
            }
            String endpoint = KICK_ENDPOINTS.get(info.source().type());
            if (endpoint == null) {
                log.warn("지원하지 않는 송출 연결 종류 type={}", info.source().type());
                return;
            }
            restClient.post()
                    .uri(URI.create(properties.apiUrl() + endpoint + info.source().id()))
                    .retrieve()
                    .toBodilessEntity();
            log.info("송출 연결 강제 종료 path={} sessionId={}", mediaPath, info.source().id());
        } catch (RestClientException e) {
            // 이미 끊긴 경로는 404 가 오므로 정상 케이스로 본다.
            log.debug("송출 연결 종료 요청 실패(이미 끊겼을 수 있음) path={} reason={}", mediaPath, e.getMessage());
        }
    }

    @Override
    public List<RecordedSegment> listRecordedSegments(String mediaPath) {
        URI uri = UriComponentsBuilder.fromUriString(properties.playbackUrl())
                .path("/list")
                .queryParam("path", mediaPath)
                .encode()
                .build()
                .toUri();
        try {
            PlaybackEntry[] entries = restClient.get().uri(uri).retrieve().body(PlaybackEntry[].class);
            if (entries == null) {
                return List.of();
            }
            return Arrays.stream(entries)
                    .filter(e -> e.start() != null)
                    .map(e -> new RecordedSegment(OffsetDateTime.parse(e.start()).toInstant(), e.duration()))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (RestClientException e) {
            log.warn("녹화 목록 조회 실패 path={} reason={}", mediaPath, e.getMessage());
            return List.of();
        }
    }

    @Override
    public void deleteRecordedSegments(String mediaPath, Instant from, Instant to) {
        URI listUri = UriComponentsBuilder.fromUriString(properties.apiUrl())
                .path("/v3/recordings/get/{path}")
                .buildAndExpand(mediaPath)
                .encode()
                .toUri();
        try {
            RecordingInfo info = restClient.get().uri(listUri).retrieve().body(RecordingInfo.class);
            if (info == null || info.segments() == null) {
                return;
            }
            for (SegmentInfo segment : info.segments()) {
                Instant start = OffsetDateTime.parse(segment.start()).toInstant();
                if (start.isBefore(from) || !start.isBefore(to)) {
                    continue;
                }
                URI deleteUri = UriComponentsBuilder.fromUriString(properties.apiUrl())
                        .path("/v3/recordings/deletesegment")
                        .queryParam("path", mediaPath)
                        .queryParam("start", segment.start())
                        .encode()
                        .build()
                        .toUri();
                restClient.delete().uri(deleteUri).retrieve().toBodilessEntity();
                log.info("녹화 파일 삭제 path={} start={}", mediaPath, segment.start());
            }
        } catch (HttpClientErrorException.NotFound e) {
            // 이 경로에 남은 녹화 파일이 없다.
        } catch (RestClientException e) {
            log.warn("녹화 파일 삭제 실패 path={} reason={}", mediaPath, e.getMessage());
            throw new DomainException(ErrorType.BAD_GATEWAY, "MEDIA_SERVER_ERROR",
                    "녹화 파일을 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RecordingInfo(String name, List<SegmentInfo> segments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SegmentInfo(String start) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PathInfo(String name, Source source, boolean ready) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Source(String type, String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlaybackEntry(String start, double duration, String url) {
    }
}
