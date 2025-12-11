package com.concurrency.redis.repository;

import com.concurrency.redis.entity.PlaybackSession;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PlaybackSessionRepository {


    private final StringRedisTemplate redis;
    private final Gson gson;


    private String key(String userId) {
        return "user:" + userId + ":sessions";
    }


    public List<PlaybackSession> getSessions(String userId) {
        Map<Object, Object> entries = redis.opsForHash().entries(key(userId));
        if (entries == null || entries.isEmpty()) return List.of();
        return entries.values().stream()
                .map(v -> gson.fromJson(String.valueOf(v), PlaybackSession.class))
                .collect(Collectors.toList());
    }


    public String getRaw(String userId, String sessionId) {
        Object o = redis.opsForHash().get(key(userId), sessionId);
        return o == null ? null : String.valueOf(o);
    }


    public void put(String userId, String sessionId, PlaybackSession session) {
        redis.opsForHash().put(key(userId), sessionId, gson.toJson(session));
    }


    public void delete(String userId, String sessionId) {
        redis.opsForHash().delete(key(userId), sessionId);
    }
}
