package com.livebroadcast.interaction.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.interaction.app.port.in.CommentUseCase;
import com.livebroadcast.interaction.in.web.dto.CommentRequest;
import com.livebroadcast.interaction.in.web.dto.CommentResponse;
import com.livebroadcast.interaction.in.web.dto.PageResponse;

import jakarta.validation.Valid;

@RestController
public class CommentController {

    private final CommentUseCase commentUseCase;

    public CommentController(CommentUseCase commentUseCase) {
        this.commentUseCase = commentUseCase;
    }

    /** 최신 댓글부터 페이지 단위로 내려준다. 화면에서는 역순으로 뒤집어 채팅처럼 보여준다. */
    @GetMapping("/api/broadcasts/{broadcastId}/comments")
    public PageResponse<CommentResponse> list(@PathVariable Long broadcastId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return PageResponse.from(commentUseCase.list(broadcastId, page, size).map(CommentResponse::from));
    }

    @PostMapping("/api/broadcasts/{broadcastId}/comments")
    public ResponseEntity<CommentResponse> write(@LoginUser AuthUser user, @PathVariable Long broadcastId,
                                                 @Valid @RequestBody CommentRequest request) {
        var comment = commentUseCase.write(broadcastId, user.id(), user.nickname(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(comment));
    }

    @PatchMapping("/api/comments/{commentId}")
    public CommentResponse edit(@LoginUser AuthUser user, @PathVariable Long commentId,
                                @Valid @RequestBody CommentRequest request) {
        return CommentResponse.from(commentUseCase.edit(commentId, user.id(), request.content()));
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> delete(@LoginUser AuthUser user, @PathVariable Long commentId) {
        commentUseCase.delete(commentId, user.id());
        return ResponseEntity.noContent().build();
    }
}
