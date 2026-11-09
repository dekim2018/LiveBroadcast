package com.livebroadcast.broadcast.app.policy;

import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase.MediaAuthCommand;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;

/** 녹화 재생: 다시보기 가능한 녹화가 있는 경로만 허용 */
@Component
public class PlaybackAccessPolicy implements MediaAccessPolicy {

    private final RecordingRepositoryPort recordingRepository;

    public PlaybackAccessPolicy(RecordingRepositoryPort recordingRepository) {
        this.recordingRepository = recordingRepository;
    }

    @Override
    public String action() {
        return "playback";
    }

    @Override
    public boolean allows(MediaAuthCommand command) {
        return command.path() != null && recordingRepository.existsReadyByMediaPath(command.path());
    }
}
