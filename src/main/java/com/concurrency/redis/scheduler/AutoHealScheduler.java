package com.concurrency.redis.scheduler;

import com.concurrency.redis.service.PlaybackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoHealScheduler {

    private final PlaybackService playbackService;

    // Runs every 10 seconds
    @Scheduled(fixedRate = 10000)
    public void runAutoHeal() {
        log.info("Running auto-heal job...");

        playbackService.autoHealAllSubscribers();
    }
}
