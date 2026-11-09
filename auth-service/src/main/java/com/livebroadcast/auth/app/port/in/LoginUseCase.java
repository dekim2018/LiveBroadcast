package com.livebroadcast.auth.app.port.in;

import com.livebroadcast.auth.domain.IssuedTokens;
import com.livebroadcast.auth.domain.User;

public interface LoginUseCase {

    LoginResult login(String username, String password);

    /** 평가용 시드 계정으로 비밀번호 없이 로그인한다. 설정에서 허용된 계정만 가능하다. */
    LoginResult testLogin(String username);

    record LoginResult(User user, IssuedTokens tokens) {
    }
}
