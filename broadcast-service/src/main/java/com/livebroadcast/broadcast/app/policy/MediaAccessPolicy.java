package com.livebroadcast.broadcast.app.policy;

import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase.MediaAuthCommand;

/**
 * MediaMTX 요청 종류(action)별 접근 허용 규칙.
 * 새 규칙(비공개 방송, 유료 방송 등)은 이 인터페이스 구현체를 추가하거나 기존 구현체를 교체해서 붙인다.
 */
public interface MediaAccessPolicy {

    /** 담당하는 MediaMTX action (publish, read, playback ...) */
    String action();

    boolean allows(MediaAuthCommand command);
}
