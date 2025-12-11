package com.concurrency.redis.service;

import com.concurrency.redis.entity.PlaybackSession;
import com.concurrency.redis.repository.PlaybackSessionRepository;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaybackService {


    private final StringRedisTemplate redis;
    private final Gson gson;
    private final PlaybackSessionRepository repository;


    @Value("${playback.max-streams}")
    private int maxStreams;


    @Value("${playback.heartbeat-timeout-ms}")
    private long heartbeatTimeout;


    private DefaultRedisScript<Long> startScript;


    @PostConstruct
    public void loadScripts() throws Exception {
        ClassPathResource res = new ClassPathResource("scripts/start_playback.lua");
        try (InputStreamReader r = new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
            startScript = new DefaultRedisScript<>();
            startScript.setScriptText(sb.toString());
            startScript.setResultType(Long.class);
        }
    }


    public PlaybackSession startPlayback(String userId, String deviceId) {
        long now = System.currentTimeMillis();
        String sessionId = UUID.randomUUID().toString();


        PlaybackSession session = PlaybackSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .deviceId(deviceId)
                .startTime(now)
                .lastPing(now)
                .build();


        Long result = redis.execute(
                startScript,
                List.of(repositoryKey(userId)),
                String.valueOf(now),
                String.valueOf(heartbeatTimeout),
                sessionId,
                gson.toJson(session),
                String.valueOf(maxStreams)
        );


        if (result == null || result == -1L) {
            throw new RuntimeException("Concurrent streaming limit reached");
        }


        return session;
    }


    public void ping(String userId, String sessionId) {
        String raw = repository.getRaw(userId, sessionId);
        if (raw == null) throw new RuntimeException("Session not found");
        PlaybackSession session = gson.fromJson(raw, PlaybackSession.class);
        session.setLastPing(System.currentTimeMillis());
        repository.put(userId, sessionId, session);
    }

    public void stop(String userId, String sessionId) {
        repository.delete(userId, sessionId);
    }


    private String repositoryKey(String userId) {
        return "user:" + userId + ":sessions";
    }
}

