package com.livebroadcast.interaction.in.web.dto;

import java.util.List;

import com.livebroadcast.interaction.app.PageResult;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(PageResult<T> r) {
        return new PageResponse<>(r.content(), r.page(), r.size(), r.totalElements(), r.totalPages());
    }
}
