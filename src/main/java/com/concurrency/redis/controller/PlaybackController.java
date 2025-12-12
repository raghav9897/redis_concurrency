package com.concurrency.redis.controller;

import com.concurrency.redis.dto.DestroyPlaybackRequest;
import com.concurrency.redis.dto.HeartbeatRequest;
import com.concurrency.redis.dto.RegisterPlaybackRequest;
import com.concurrency.redis.entity.PlaybackSession;
import com.concurrency.redis.service.PlaybackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/playback")
@RequiredArgsConstructor
public class PlaybackController {


    private final PlaybackService service;


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterPlaybackRequest req) {
        PlaybackSession s = service.registerPlayback(req);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "Playback registered successfully",
                "data", Map.of(
                        "deviceId", s.getDeviceId(),
                        "ttl", 10
                )
        ));
    }


    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(@Valid @RequestBody HeartbeatRequest req) {
        boolean updated = service.heartbeat(req.getSubscriberId(), req.getDeviceId());
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "Heartbeat updated",
                "data", Map.of("ttlRefreshed", updated)
        ));
    }


    @GetMapping("/{subscriberId}/devices")
    public ResponseEntity<?> activeDevices(@PathVariable String subscriberId) {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "Success",
                "data", Map.of("devices", service.getActiveDevices(subscriberId))
        ));
    }


    @PostMapping("/destroy")
    public ResponseEntity<?> destroy(@Valid @RequestBody DestroyPlaybackRequest req) {
        service.destroyPlayback(req.getSubscriberId(), req.getDeviceId());
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "Playback destroyed",
                "data", Map.of("deviceId", req.getDeviceId())
        ));
    }
}

