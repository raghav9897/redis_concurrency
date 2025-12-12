package com.concurrency.redis.service;

import com.concurrency.redis.dto.RegisterPlaybackRequest;
import com.concurrency.redis.entity.PlaybackSession;
import com.concurrency.redis.repository.PlaybackRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlaybackService {


    private final StringRedisTemplate redis;
    private final Gson gson ;
    private static final int TTL = 10;
    private static final int CONCURRENCY = 2;

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


    private String playbackKey(String sId, String dId) {
        return "playback:" + sId + ":" + dId;
    }


    private String indexKey(String sId) {
        return "subscriber:" + sId + ":devices";
    }


    public PlaybackSession registerPlayback(RegisterPlaybackRequest req) {
        PlaybackSession session = PlaybackSession.from(req);
        long nowEpochSeconds = Instant.now().getEpochSecond();

        /*String key = playbackKey(req.getSubscriberId(), req.getDeviceId());


        redis.opsForValue().set(key, gson.toJson(session), Duration.ofSeconds(TTL));
        redis.opsForSet().add(indexKey(req.getSubscriberId()), req.getDeviceId());
        redis.opsForSet().add("subscribers:index", req.getSubscriberId());*/

        Long result = redis.execute(
                startScript,
                List.of(indexKey(req.getSubscriberId())),   // KEYS[1]

                req.getSubscriberId(),                         // ARGV[1]
                req.getDeviceId(),                             // ARGV[2]
                String.valueOf(TTL),           // ARGV[3]
                String.valueOf(CONCURRENCY),    // ARGV[4]
                String.valueOf(nowEpochSeconds)       // ARGV[5]
        );



        if (result == null || result == -1L) {
            throw new RuntimeException("Concurrent streaming limit reached");
        }



        return session;
    }


    public boolean heartbeat(String subscriberId, String deviceId) {
        return Boolean.TRUE.equals(redis.expire(playbackKey(subscriberId, deviceId), Duration.ofSeconds(TTL)));
    }


    public List<Map<String, Object>> getActiveDevices(String subscriberId) {
        Set<String> devices = redis.opsForSet().members(indexKey(subscriberId));
        if (devices == null) return List.of();


        List<Map<String, Object>> active = new ArrayList<>();


        for (String deviceId : devices) {
            String json = redis.opsForValue().get(playbackKey(subscriberId, deviceId));
            if (json == null) {
                redis.opsForSet().remove(indexKey(subscriberId), deviceId);
                continue;
            }
            PlaybackSession s = gson.fromJson(json, PlaybackSession.class);
            active.add(Map.of(
                    "deviceId", s.getDeviceId(),
                    "lastSeen", s.getLastSeen(),
                    "deviceType", s.getDeviceType()
            ));
        }
        return active;
    }


    public void destroyPlayback(String subscriberId, String deviceId) {
        redis.delete(playbackKey(subscriberId, deviceId));
        redis.opsForSet().remove(indexKey(subscriberId), deviceId);
    }

    public void autoHealAllSubscribers() {
        Set<String> subscribers = redis.opsForSet().members("subscribers:index");


        for (String subscriberId : subscribers) {
            autoHeal(subscriberId);
        }
    }


    public void autoHeal(String subscriberId) {
        String indexKey = indexKey(subscriberId);

        Set<String> devices = redis.opsForSet().members(indexKey);
        if (devices == null || devices.isEmpty()) {
            redis.opsForSet().remove("subscribers:index",subscriberId );

            return;
        }

        for (String deviceId : devices) {
            String playbackKey = playbackKey(subscriberId, deviceId);
            Boolean exists = redis.hasKey(playbackKey);

            if (exists == null || !exists) {
                redis.opsForSet().remove(indexKey, deviceId);

            }
        }
    }

}

