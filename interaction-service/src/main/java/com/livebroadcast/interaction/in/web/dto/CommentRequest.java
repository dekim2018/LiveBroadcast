package com.livebroadcast.interaction.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "댓글 내용을 입력해 주세요.")
        @Size(max = 300, message = "댓글은 300자 이하여야 합니다.")
        String content
) {
}
