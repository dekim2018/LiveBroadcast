package com.livebroadcast.auth.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TestLoginRequest(@NotBlank String username) {
}
