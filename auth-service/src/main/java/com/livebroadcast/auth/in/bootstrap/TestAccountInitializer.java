package com.livebroadcast.auth.in.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.livebroadcast.auth.app.TestAccountProperties;
import com.livebroadcast.auth.app.service.AuthService;

/**
 * 서버 기동 시 평가용 테스트 계정(tester1, tester2)을 준비한다.
 */
@Component
public class TestAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestAccountInitializer.class);

    private final AuthService authService;
    private final TestAccountProperties testAccounts;

    public TestAccountInitializer(AuthService authService, TestAccountProperties testAccounts) {
        this.authService = authService;
        this.testAccounts = testAccounts;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.ensureTestAccounts();
        if (!testAccounts.enabled()) {
            log.info("테스트 계정 비활성화 (TEST_ACCOUNTS_ENABLED=false): 원클릭 로그인을 막고 기존 테스트 계정은 잠갔습니다.");
        } else if (testAccounts.hasPassword()) {
            log.warn("테스트 계정 활성화: 원클릭 로그인이 열려 있습니다. 평가가 끝나면 TEST_ACCOUNTS_ENABLED=false 로 끄세요.");
        } else {
            log.warn("테스트 계정 활성화: TEST_ACCOUNT_PASSWORD 가 없어 무작위 비밀번호를 사용합니다(원클릭 로그인만 가능). "
                    + "평가가 끝나면 TEST_ACCOUNTS_ENABLED=false 로 끄세요.");
        }
    }
}
