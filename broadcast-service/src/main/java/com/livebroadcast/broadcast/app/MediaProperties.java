package com.livebroadcast.broadcast.app;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param apiUrl            MediaMTX Control API (내부망)
 * @param playbackUrl       MediaMTX Playback 서버 (내부망)
 * @param publicMediaBase   브라우저가 WHIP/WHEP 요청을 보낼 경로 (Caddy 가 MediaMTX 로 프록시)
 * @param publicPlaybackBase 브라우저가 녹화 영상을 받을 경로 (Caddy 가 Playback 서버로 프록시)
 * @param recordingRoot     MediaMTX 컨테이너 안의 녹화 저장 루트
 * @param staleGrace        방송 중인데 송출이 이 시간 이상 끊기면 자동 종료
 */
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
        String apiUrl,
        String playbackUrl,
        String publicMediaBase,
        String publicPlaybackBase,
        String recordingRoot,
        Duration staleGrace
) {
    public MediaProperties {
        if (publicMediaBase == null) publicMediaBase = "/media";
        if (publicPlaybackBase == null) publicPlaybackBase = "/playback";
        if (recordingRoot == null) recordingRoot = "/recordings";
        if (staleGrace == null) staleGrace = Duration.ofMinutes(3);
    }

    public String whipUrl(String mediaPath) {
        return publicMediaBase + "/" + mediaPath + "/whip";
    }

    public String whepUrl(String mediaPath) {
        return publicMediaBase + "/" + mediaPath + "/whep";
    }
}
