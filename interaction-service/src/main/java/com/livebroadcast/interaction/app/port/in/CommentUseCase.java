package com.livebroadcast.interaction.app.port.in;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.domain.comment.Comment;

public interface CommentUseCase {

    Comment write(Long broadcastId, Long authorId, String authorNickname, String content);

    PageResult<Comment> list(Long broadcastId, int page, int size);

    Comment edit(Long commentId, Long requesterId, String content);

    void delete(Long commentId, Long requesterId);
}
