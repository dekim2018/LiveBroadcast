package com.livebroadcast.broadcast.domain;

public enum RecordingStatus {
    /** 송출 중이라 녹화가 진행 중 */
    RECORDING,
    /** 녹화 완료, 다시보기 가능 */
    READY,
    /** 녹화 길이가 너무 짧거나 실패 */
    FAILED;

    /** 아직 파일이 쓰이고 있는 상태. 마무리할 수 있고, 삭제는 할 수 없다. */
    public boolean isInProgress() {
        return this == RECORDING;
    }

    /** 다시보기로 재생할 수 있는 상태 */
    public boolean isPlayable() {
        return this == READY;
    }
}
