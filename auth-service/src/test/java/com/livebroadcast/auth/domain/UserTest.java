package com.livebroadcast.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.livebroadcast.common.error.DomainException;

class UserTest {

    @Test
    void 올바른_아이디와_닉네임이면_회원을_만든다() {
        User user = User.register("tester_01", "hash", "  테스터  ", Instant.now());

        assertThat(user.getUsername()).isEqualTo("tester_01");
        assertThat(user.getNickname()).isEqualTo("테스터");
    }

    @Test
    void 아이디_형식이_틀리면_예외() {
        assertThatThrownBy(() -> User.register("AB", "hash", "닉", Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("아이디");
    }

    @Test
    void 닉네임이_비어있으면_예외() {
        assertThatThrownBy(() -> User.register("tester1", "hash", " ", Instant.now()))
                .isInstanceOf(DomainException.class);
    }
}
