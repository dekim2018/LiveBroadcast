package com.livebroadcast.auth.in.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.livebroadcast.auth.app.service.AuthService;

/**
 * 서버 기동 시 평가용 테스트 계정(tester1, tester2)을 준비한다.
 */
@Component
public class TestAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestAccountInitializer.class);

    private final AuthService authService;

    public TestAccountInitializer(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.ensureTestAccounts();
        log.info("테스트 계정 준비 완료");
    }
}
