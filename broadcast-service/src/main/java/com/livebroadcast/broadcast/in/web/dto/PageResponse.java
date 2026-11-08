package com.livebroadcast.broadcast.in.web.dto;

import java.util.List;

import com.livebroadcast.broadcast.app.PageResult;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(result.content(), result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }
}
