package com.practice.jobs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runs submitted jobs one at a time via runNext().
 *  - Highest priority first; equal priority runs in submission order.
 *  - A job waits until all its dependencies have SUCCEEDED.
 *  - Each tenant is rate-limited; a limited tenant must not block other tenants.
 *  - Failed attempts retry with exponential backoff: baseBackoff, then 2x, 4x, ...
 *  - After maxAttempts failed runs the job is dead-lettered.
 *  - Submitting an id that already exists is ignored.
 */
public class JobScheduler {
    private static final Comparator<JobRecord> RUN_ORDER =
            Comparator.comparingInt((JobRecord r) -> r.job.priority()).reversed()
                    .thenComparingLong(r -> r.sequence);

    private final Clock clock;
    private final JobHandler handler;
    private final SlidingWindowRateLimiter limiter;
    private final long baseBackoffMillis;
    private final Map<String, JobRecord> jobs = new HashMap<>();
    private final List<String> deadLetters = new ArrayList<>();
    private long nextSequence = 0;

    public JobScheduler(Clock clock, JobHandler handler, SlidingWindowRateLimiter limiter, long baseBackoffMillis) {
        this.clock = clock;
        this.handler = handler;
        this.limiter = limiter;
        this.baseBackoffMillis = baseBackoffMillis;
    }

    /** @return true if accepted, false if a job with this id was already submitted */
    public boolean submit(Job job) {
        JobRecord previous = jobs.put(job.id(), new JobRecord(job, nextSequence++));
        return previous == null;
    }

    /** Runs the next eligible job, if any. @return the id of the job that ran */
    public Optional<String> runNext() {
        long now = clock.now();
        List<JobRecord> candidates = new ArrayList<>();
        for (JobRecord record : jobs.values()) {
            if (isRunnable(record, now)) {
                candidates.add(record);
            }
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        candidates.sort(RUN_ORDER);
        JobRecord next = candidates.get(0);
        if (!limiter.tryAcquire(next.job.tenantId(), now)) {
            return Optional.empty();
        }
        execute(next, now);
        return Optional.of(next.job.id());
    }

    public JobState state(String jobId) {
        JobRecord record = jobs.get(jobId);
        return record == null ? null : record.state;
    }

    public int attempts(String jobId) {
        JobRecord record = jobs.get(jobId);
        return record == null ? 0 : record.attempts;
    }

    public String lastError(String jobId) {
        JobRecord record = jobs.get(jobId);
        return record == null ? null : record.lastError;
    }

    public List<String> deadLetters() {
        return List.copyOf(deadLetters);
    }

    private boolean isRunnable(JobRecord record, long now) {
        if (record.state != JobState.PENDING || now < record.nextAttemptAt) {
            return false;
        }
        for (String dependencyId : record.job.dependsOn()) {
            if (state(dependencyId) != JobState.SUCCEEDED) {
                return false;
            }
        }
        return true;
    }

    private void execute(JobRecord record, long now) {
        record.attempts++;
        try {
            handler.run(record.job);
            record.state = JobState.SUCCEEDED;
        } catch (Exception e) {
            record.lastError = e.getMessage();
            if (record.attempts > record.job.maxAttempts()) {
                record.state = JobState.DEAD_LETTERED;
                deadLetters.add(record.job.id());
            } else {
                record.nextAttemptAt = now + backoffAfter(record.attempts);
            }
        }
    }

    /** Delay before the next try, after the given number of failed attempts (1, 2, 3...). */
    private long backoffAfter(int failedAttempts) {
        return baseBackoffMillis * (1L << failedAttempts);
    }
}
