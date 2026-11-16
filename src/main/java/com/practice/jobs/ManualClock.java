package com.practice.jobs;

/** Test clock that only moves when told to. */
public class ManualClock implements Clock {
    private long now;

    public ManualClock(long start) {
        this.now = start;
    }

    @Override
    public long now() {
        return now;
    }

    public void set(long millis) {
        this.now = millis;
    }
}
