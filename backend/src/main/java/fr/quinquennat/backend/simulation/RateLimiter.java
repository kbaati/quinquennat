package fr.quinquennat.backend.simulation;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Fixed one-minute window per key (client IP), kept in memory: enough for a single instance. */
@Component
public class RateLimiter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_KEYS = 100_000;

    private final int limitPerMinute;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimiter(@Value("${simulations.rate-limit-per-minute}") int limitPerMinute) {
        this(limitPerMinute, Clock.systemUTC());
    }

    RateLimiter(int limitPerMinute, Clock clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    public boolean tryAcquire(String key) {
        long now = clock.millis();
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(window -> now - window.start >= WINDOW_MILLIS);
        }
        Window window = windows.compute(key, (k, current) ->
                current == null || now - current.start >= WINDOW_MILLIS
                        ? new Window(now, 1)
                        : new Window(current.start, current.count + 1));
        return window.count <= limitPerMinute;
    }

    private record Window(long start, int count) {
    }
}
