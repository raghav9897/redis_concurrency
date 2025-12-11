package com.concurrency.redis.controller;

import com.concurrency.redis.entity.PlaybackSession;
import com.concurrency.redis.service.PlaybackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playback")
@RequiredArgsConstructor
public class PlaybackController {


    private final PlaybackService service;


    @PostMapping("/start")
    public ResponseEntity<PlaybackSession> start(
            @RequestParam String userId,
            @RequestParam String deviceId
    ) {
        PlaybackSession session = service.startPlayback(userId, deviceId);
        return ResponseEntity.ok(session);
    }


    @PostMapping("/ping")
    public ResponseEntity<String> ping(
            @RequestParam String userId,
            @RequestParam String sessionId
    ) {
        service.ping(userId, sessionId);
        return ResponseEntity.ok("OK");
    }


    @PostMapping("/stop")
    public ResponseEntity<String> stop(
            @RequestParam String userId,
            @RequestParam String sessionId
    ) {
        service.stop(userId, sessionId);
        return ResponseEntity.ok("Stopped");
    }
}

