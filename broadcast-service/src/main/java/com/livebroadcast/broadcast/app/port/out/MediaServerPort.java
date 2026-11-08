package com.livebroadcast.broadcast.app.port.out;

import java.util.List;

import com.livebroadcast.broadcast.domain.RecordedSegment;

public interface MediaServerPort {

    /** 해당 경로로 송출 중인 연결을 강제로 끊는다. 실패해도 예외를 던지지 않는다. */
    void kickPublisher(String mediaPath);

    /** 미디어 서버에 저장된 녹화 구간 목록 */
    List<RecordedSegment> listRecordedSegments(String mediaPath);
}
