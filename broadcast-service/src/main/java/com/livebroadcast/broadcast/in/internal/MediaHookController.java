package com.livebroadcast.broadcast.in.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase;
import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase.MediaAuthCommand;
import com.livebroadcast.broadcast.in.internal.dto.MediaAuthRequest;

/**
 * MediaMTX 전용 내부 엔드포인트. 게이트웨이로 노출되지 않고, InternalApiKeyFilter 로 한 번 더 보호된다.
 */
@RestController
@RequestMapping("/internal/media")
public class MediaHookController {

    private static final Logger log = LoggerFactory.getLogger(MediaHookController.class);

    private final MediaEventUseCase mediaEventUseCase;

    public MediaHookController(MediaEventUseCase mediaEventUseCase) {
        this.mediaEventUseCase = mediaEventUseCase;
    }

    /** 2xx 를 돌려주면 허용, 그 외는 거절 */
    @PostMapping("/auth")
    public ResponseEntity<Void> auth(@RequestBody MediaAuthRequest request) {
        boolean allowed = mediaEventUseCase.authorize(new MediaAuthCommand(
                request.action(), request.path(), request.token(), request.password(), request.query(), request.ip()));
        if (!allowed && "publish".equals(request.action())) {
            log.info("송출 거절 path={} ip={}", request.path(), request.ip());
        }
        return ResponseEntity.status(allowed ? HttpStatus.OK : HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/hooks/ready")
    public ResponseEntity<Void> ready(@RequestParam String path) {
        mediaEventUseCase.onStreamReady(path);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hooks/not-ready")
    public ResponseEntity<Void> notReady(@RequestParam String path) {
        mediaEventUseCase.onStreamNotReady(path);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hooks/segment-complete")
    public ResponseEntity<Void> segmentComplete(@RequestParam String path,
                                                @RequestParam(required = false) String segmentPath) {
        mediaEventUseCase.onRecordSegmentComplete(path, segmentPath);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hooks/{unknown}")
    public ResponseEntity<Void> unknown(@PathVariable String unknown) {
        return ResponseEntity.notFound().build();
    }
}
