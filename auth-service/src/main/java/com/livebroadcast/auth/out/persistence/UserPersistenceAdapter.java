package com.livebroadcast.auth.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.livebroadcast.auth.app.port.out.UserRepositoryPort;
import com.livebroadcast.auth.domain.User;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = repository.save(new UserJpaEntity(
                user.getId(), user.getUsername(), user.getPasswordHash(), user.getNickname(), user.getCreatedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return repository.existsByNickname(nickname);
    }

    private User toDomain(UserJpaEntity e) {
        return new User(e.getId(), e.getUsername(), e.getPasswordHash(), e.getNickname(), e.getCreatedAt());
    }
}
