package com.livebroadcast.interaction.app.port.out;

import java.util.Optional;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.domain.comment.Comment;

public interface CommentRepositoryPort {

    Comment save(Comment comment);

    Optional<Comment> findById(Long id);

    PageResult<Comment> findLatestByBroadcast(Long broadcastId, int page, int size);

    void delete(Long id);
}
