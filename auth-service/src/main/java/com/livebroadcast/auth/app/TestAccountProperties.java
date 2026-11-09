package com.livebroadcast.auth.app;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 평가용 테스트 계정. 서버 기동 시 자동 생성되고, 로그인 화면의 원클릭 로그인에 사용된다.
 */
@ConfigurationProperties(prefix = "app.test-accounts")
public record TestAccountProperties(boolean enabled, String password, List<Account> accounts) {

    public TestAccountProperties {
        accounts = accounts == null ? List.of() : List.copyOf(accounts);
    }

    public boolean isTestAccount(String username) {
        return enabled && accounts.stream().anyMatch(a -> a.username().equals(username));
    }

    public record Account(String username, String nickname) {
    }
}
