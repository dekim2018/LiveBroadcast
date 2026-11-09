package com.livebroadcast.broadcast.app.port.in;

import java.util.List;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.domain.Recording;

public interface RecordingUseCase {

    PageResult<Recording> list(Long broadcastId, int page, int size);

    RecordingDetail get(Long recordingId);

    void delete(Long recordingId, Long requesterId);

    record RecordingDetail(Recording recording, List<PlaybackSegment> segments) {
    }

    record PlaybackSegment(String url, String start, double durationSeconds) {
    }
}
