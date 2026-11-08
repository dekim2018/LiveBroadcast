package com.livebroadcast.auth.domain;

import com.livebroadcast.common.error.DomainException;

public final class AuthErrors {

    private AuthErrors() {
    }

    public static DomainException invalidCredentials() {
        return DomainException.unauthorized("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    public static DomainException duplicateUsername() {
        return DomainException.conflict("DUPLICATE_USERNAME", "이미 사용 중인 아이디입니다.");
    }

    public static DomainException duplicateNickname() {
        return DomainException.conflict("DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");
    }

    public static DomainException invalidRefreshToken() {
        return DomainException.unauthorized("INVALID_REFRESH_TOKEN", "다시 로그인해 주세요.");
    }

    public static DomainException userNotFound() {
        return DomainException.notFound("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
    }

    public static DomainException testLoginDisabled() {
        return DomainException.forbidden("TEST_LOGIN_DISABLED", "테스트 로그인을 사용할 수 없는 계정입니다.");
    }
}
