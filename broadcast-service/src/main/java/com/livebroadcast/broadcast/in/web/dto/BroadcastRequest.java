package com.livebroadcast.broadcast.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastRequest(
        @NotBlank(message = "방송 제목을 입력해 주세요.")
        @Size(max = 100, message = "방송 제목은 100자 이하여야 합니다.")
        String title,

        @Size(max = 1000, message = "방송 설명은 1000자 이하여야 합니다.")
        String description
) {
}
