package com.livebroadcast.auth.app.port.in;

import com.livebroadcast.auth.domain.User;

public interface SignUpUseCase {

    User signUp(SignUpCommand command);

    record SignUpCommand(String username, String password, String nickname) {
    }
}
