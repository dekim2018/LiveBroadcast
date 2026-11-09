package com.livebroadcast.interaction.domain.comment;

/** 댓글이 삭제됨 */
public record CommentDeleted(Long commentId, Long broadcastId) {
}
