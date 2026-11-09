package com.livebroadcast.broadcast.in.web.dto;

import com.livebroadcast.broadcast.domain.SourceType;

import jakarta.validation.constraints.NotNull;

public record StartLiveRequest(
        @NotNull(message = "송출 방식(PC_SCREEN, MOBILE_CAMERA)을 선택해 주세요.")
        SourceType sourceType
) {
}
