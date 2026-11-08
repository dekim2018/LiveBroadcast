package com.livebroadcast.broadcast.app.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 외부 시스템 호출(미디어 서버 연결 끊기 등)은 DB 커밋이 확정된 뒤에 실행한다.
 */
final class AfterCommit {

    private AfterCommit() {
    }

    static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
