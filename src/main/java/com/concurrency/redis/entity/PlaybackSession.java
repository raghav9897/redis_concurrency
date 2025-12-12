package com.concurrency.redis.entity;

import com.concurrency.redis.dto.RegisterPlaybackRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackSession {
    private String subscriberId;
    private String deviceId;
    private String playbackToken;
    private long startedAt;
    private long lastSeen;
    private String deviceType;
    private String appVersion;
    private String os;
    private String ip;

    public static PlaybackSession from(RegisterPlaybackRequest req) {
        long now = System.currentTimeMillis();
        return PlaybackSession.builder()
                .subscriberId(req.getSubscriberId())
                .deviceId(req.getDeviceId())
                .playbackToken(req.getPlaybackToken())
                .deviceType(req.getDeviceType())
                .appVersion(req.getAppVersion())
                .os(req.getOs())
                .ip(req.getIp())
                .startedAt(now)
                .lastSeen(now)
                .build();
    }
}
