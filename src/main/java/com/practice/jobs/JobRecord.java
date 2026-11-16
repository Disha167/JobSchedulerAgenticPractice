package com.practice.jobs;

/** Scheduler's internal bookkeeping for one job. */
class JobRecord {
    final Job job;
    final long sequence;
    JobState state = JobState.PENDING;
    int attempts = 0;
    long nextAttemptAt = 0;
    String lastError;

    JobRecord(Job job, long sequence) {
        this.job = job;
        this.sequence = sequence;
    }
}
