package com.livebroadcast.auth.app.port.out;

import java.util.Optional;

import com.livebroadcast.auth.domain.User;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);
}
