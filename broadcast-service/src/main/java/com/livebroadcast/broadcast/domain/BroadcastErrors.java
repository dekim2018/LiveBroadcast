package com.livebroadcast.broadcast.domain;

import com.livebroadcast.common.error.DomainException;

public final class BroadcastErrors {

    private BroadcastErrors() {
    }

    public static DomainException notFound() {
        return DomainException.notFound("BROADCAST_NOT_FOUND", "방송을 찾을 수 없습니다.");
    }

    public static DomainException notOwner() {
        return DomainException.forbidden("NOT_BROADCAST_OWNER", "방송 소유자만 할 수 있는 작업입니다.");
    }

    public static DomainException alreadyLive() {
        return DomainException.conflict("ALREADY_LIVE", "이미 방송 중입니다.");
    }

    public static DomainException notLive() {
        return DomainException.conflict("NOT_LIVE", "방송 중이 아닙니다.");
    }

    public static DomainException deleteWhileLive() {
        return DomainException.conflict("DELETE_WHILE_LIVE", "방송 중에는 삭제할 수 없습니다. 먼저 방송을 종료해 주세요.");
    }

    public static DomainException invalidTitle() {
        return DomainException.badRequest("INVALID_TITLE", "방송 제목은 1~100자여야 합니다.");
    }

    public static DomainException invalidDescription() {
        return DomainException.badRequest("INVALID_DESCRIPTION", "방송 설명은 1000자 이하여야 합니다.");
    }

    public static DomainException recordingNotFound() {
        return DomainException.notFound("RECORDING_NOT_FOUND", "녹화 영상을 찾을 수 없습니다.");
    }

    public static DomainException notRecordingOwner() {
        return DomainException.forbidden("NOT_RECORDING_OWNER", "녹화 영상 소유자만 삭제할 수 있습니다.");
    }
}
