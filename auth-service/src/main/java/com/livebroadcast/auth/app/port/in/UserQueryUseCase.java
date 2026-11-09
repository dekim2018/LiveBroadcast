package com.livebroadcast.auth.app.port.in;

import java.util.List;

import com.livebroadcast.auth.domain.User;

public interface UserQueryUseCase {

    User getUser(Long userId);

    List<User> getTestAccounts();
}
