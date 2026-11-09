package com.livebroadcast.auth.in.web.dto;

import com.livebroadcast.auth.domain.User;

public record UserResponse(Long id, String username, String nickname) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getNickname());
    }
}
