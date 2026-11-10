package com.livebroadcast.auth.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.livebroadcast.auth.app.TestAccountProperties;
import com.livebroadcast.auth.app.TestAccountProperties.Account;
import com.livebroadcast.auth.app.port.out.PasswordHasherPort;
import com.livebroadcast.auth.app.port.out.UserRepositoryPort;
import com.livebroadcast.auth.domain.User;

/** 서버 기동 시 테스트 계정 준비 규칙: 기본 꺼짐, 비밀번호 동기화, 비밀번호 미설정 시 무작위 */
class TestAccountSetupTest {

    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
    private static final List<Account> ACCOUNTS = List.of(new Account("tester1", "테스터1"));

    private final FakeUserRepository users = new FakeUserRepository();
    /** 해시 대신 접두사만 붙이는 가짜 해셔 */
    private final PasswordHasherPort hasher = new PasswordHasherPort() {
        @Override
        public String hash(String raw) {
            return "h:" + raw;
        }

        @Override
        public boolean matches(String raw, String hash) {
            return ("h:" + raw).equals(hash);
        }
    };

    private AuthService service(boolean enabled, String password) {
        return new AuthService(users, hasher, null, null, new TestAccountProperties(enabled, password, ACCOUNTS),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void 꺼져_있으면_계정을_만들지_않고_원클릭_로그인도_거절한다() {
        AuthService service = service(false, "secret-1");

        service.ensureTestAccounts();

        assertThat(users.byUsername).isEmpty();
        assertThat(service.getTestAccounts()).isEmpty();
        assertThatThrownBy(() -> service.testLogin("tester1")).hasMessageContaining("테스트");
    }

    @Test
    void 꺼지면_이미_있는_테스트_계정도_예전_비밀번호로_로그인할_수_없다() {
        service(true, "test1234!").ensureTestAccounts();

        service(false, "test1234!").ensureTestAccounts();

        assertThat(hasher.matches("test1234!", users.byUsername.get("tester1").getPasswordHash())).isFalse();
    }

    @Test
    void 켜져_있으면_설정한_비밀번호로_계정을_만든다() {
        service(true, "secret-1").ensureTestAccounts();

        assertThat(users.byUsername.get("tester1").getPasswordHash()).isEqualTo("h:secret-1");
    }

    @Test
    void 이미_있는_계정은_비밀번호를_새_설정_값으로_바꾼다() {
        service(true, "test1234!").ensureTestAccounts();
        Long id = users.byUsername.get("tester1").getId();

        service(true, "new-secret").ensureTestAccounts();

        User user = users.byUsername.get("tester1");
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getPasswordHash()).isEqualTo("h:new-secret");
    }

    @Test
    void 비밀번호가_없으면_무작위_비밀번호를_써서_예전_비밀번호로는_로그인할_수_없다() {
        service(true, "test1234!").ensureTestAccounts();

        service(true, "").ensureTestAccounts();

        String hash = users.byUsername.get("tester1").getPasswordHash();
        assertThat(hasher.matches("test1234!", hash)).isFalse();
        assertThat(hasher.matches("", hash)).isFalse();
    }

    static class FakeUserRepository implements UserRepositoryPort {
        final Map<String, User> byUsername = new HashMap<>();
        long seq;

        @Override
        public User save(User u) {
            User saved = new User(u.getId() == null ? ++seq : u.getId(), u.getUsername(), u.getPasswordHash(),
                    u.getNickname(), u.getCreatedAt());
            byUsername.put(saved.getUsername(), saved);
            return saved;
        }

        @Override
        public Optional<User> findById(Long id) {
            return byUsername.values().stream().filter(u -> u.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(byUsername.get(username));
        }

        @Override
        public boolean existsByUsername(String username) {
            return byUsername.containsKey(username);
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return byUsername.values().stream().anyMatch(u -> u.getNickname().equals(nickname));
        }
    }
}
