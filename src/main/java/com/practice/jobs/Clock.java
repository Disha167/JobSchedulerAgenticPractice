package com.practice.jobs;

/** Source of the current time in milliseconds. Injected so tests can control time. */
public interface Clock {
    long now();
}
