package com.livebroadcast.interaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.interaction.domain.comment.Comment;

class CommentTest {

    private final Instant now = Instant.parse("2026-11-10T00:00:00Z");

    @Test
    void 작성자는_수정할_수_있다() {
        Comment c = Comment.write(1L, 5L, "작성자", "안녕하세요", now);
        c.edit(5L, "수정했어요", now.plusSeconds(1));

        assertThat(c.getContent()).isEqualTo("수정했어요");
        assertThat(c.isEdited()).isTrue();
    }

    @Test
    void 다른_사용자는_수정_삭제할_수_없다() {
        Comment c = Comment.write(1L, 5L, "작성자", "안녕하세요", now);

        assertThatThrownBy(() -> c.edit(6L, "몰래 수정", now)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> c.ensureAuthor(6L)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> c.ensureAuthor(null)).isInstanceOf(DomainException.class);
    }

    @Test
    void 빈_댓글이나_너무_긴_댓글은_작성할_수_없다() {
        assertThatThrownBy(() -> Comment.write(1L, 5L, "n", "   ", now)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Comment.write(1L, 5L, "n", "a".repeat(301), now)).isInstanceOf(DomainException.class);
    }
}
