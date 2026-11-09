package com.livebroadcast.broadcast.domain;

public enum RecordingStatus {
    /** 송출 중이라 녹화가 진행 중 */
    RECORDING,
    /** 녹화 완료, 다시보기 가능 */
    READY,
    /** 녹화 길이가 너무 짧거나 실패 */
    FAILED
}
