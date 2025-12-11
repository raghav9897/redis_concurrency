package com.concurrency.redis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackSession {
    private String sessionId;
    private String userId;
    private String deviceId;
    private long startTime;
    private long lastPing;
}
