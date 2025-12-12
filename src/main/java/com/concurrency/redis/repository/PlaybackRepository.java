package com.concurrency.redis.repository;

import com.concurrency.redis.entity.PlaybackSession;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class PlaybackRepository {
    private final StringRedisTemplate redis;
    private final Gson gson;

    public void register(PlaybackSession session, long ttlSec) {
        String key = "playback:" + session.getSubscriberId() + ":" + session.getDeviceId();
        redis.opsForValue().set(key, gson.toJson(session), Duration.ofSeconds(ttlSec));
        redis.opsForSet().add("subscriber:" + session.getSubscriberId() + ":devices", session.getDeviceId());
    }

    public void heartbeat(String subscriberId, String deviceId, long ttlSec) {
        String key = "playback:" + subscriberId + ":" + deviceId;
        redis.expire(key, ttlSec, TimeUnit.SECONDS);
    }

    public void destroy(String subscriberId, String deviceId) {
        String key = "playback:" + subscriberId + ":" + deviceId;
        redis.delete(key);
        redis.opsForSet().remove("subscriber:" + subscriberId + ":devices", deviceId);
    }

    public Set<String> fetchDevices(String subscriberId) {
        return redis.opsForSet().members("subscriber:" + subscriberId + ":devices");
    }

    public boolean validate(String subscriberId, String deviceId) {
        String key = "playback:" + subscriberId + ":" + deviceId;
        Boolean exists = redis.hasKey(key);
        return exists != null && exists;
    }

    public void autoHeal(String subscriberId) {
        Set<String> devices = fetchDevices(subscriberId);
        if (devices == null) return;
        for (String deviceId : devices) {
            if (!validate(subscriberId, deviceId)) {
                redis.opsForSet().remove("subscriber:" + subscriberId + ":devices", deviceId);
            }
        }
    }
}
