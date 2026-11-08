package com.livebroadcast.broadcast.in.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * MediaMTX 의 authMethod: http 설정에서 보내는 요청 본문.
 * action: publish, read, playback, api, metrics, pprof
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaAuthRequest(
        String user,
        String password,
        String token,
        String ip,
        String action,
        String path,
        String protocol,
        String query
) {
}
