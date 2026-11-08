package com.livebroadcast.broadcast.app.port.in;

/**
 * MediaMTX 가 호출하는 인증/이벤트 훅 처리.
 */
public interface MediaEventUseCase {

    boolean authorize(MediaAuthCommand command);

    void onStreamReady(String mediaPath);

    void onStreamNotReady(String mediaPath);

    void onRecordSegmentComplete(String mediaPath, String segmentPath);

    int endStaleBroadcasts();

    record MediaAuthCommand(String action, String path, String token, String password, String query, String ip) {
    }
}
