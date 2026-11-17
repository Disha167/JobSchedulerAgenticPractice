package com.practice.jobs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows at most maxRequests per key in any window of windowMillis.
 * A request at time t counts against requests made in (t - windowMillis, t].
 */
public class SlidingWindowRateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, Deque<Long>> recent = new HashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /** Records and allows the request if the key is under its limit; otherwise rejects it. */
    public boolean tryAcquire(String key, long now) {
        Deque<Long> times = recent.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!times.isEmpty() && times.peekFirst() <= now - windowMillis) {
            times.pollFirst();
        }
        if (times.size() >= maxRequests) {
            return false;
        }
        times.addLast(now);
        return true;
    }
}
