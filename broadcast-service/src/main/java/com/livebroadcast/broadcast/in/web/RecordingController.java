package com.livebroadcast.broadcast.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.broadcast.app.port.in.RecordingUseCase;
import com.livebroadcast.broadcast.in.web.dto.PageResponse;
import com.livebroadcast.broadcast.in.web.dto.RecordingResponse;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;

@RestController
@RequestMapping("/api/recordings")
public class RecordingController {

    private final RecordingUseCase recordingUseCase;

    public RecordingController(RecordingUseCase recordingUseCase) {
        this.recordingUseCase = recordingUseCase;
    }

    @GetMapping
    public PageResponse<RecordingResponse> list(@RequestParam(required = false) Long broadcastId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(recordingUseCase.list(broadcastId, page, size)
                .map(r -> RecordingResponse.of(r, List.of())));
    }

    @GetMapping("/{recordingId}")
    public RecordingResponse get(@PathVariable Long recordingId) {
        var detail = recordingUseCase.get(recordingId);
        return RecordingResponse.of(detail.recording(), detail.segments());
    }

    @DeleteMapping("/{recordingId}")
    public ResponseEntity<Void> delete(@LoginUser AuthUser user, @PathVariable Long recordingId) {
        recordingUseCase.delete(recordingId, user.id());
        return ResponseEntity.noContent().build();
    }
}
