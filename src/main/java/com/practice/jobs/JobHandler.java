package com.practice.jobs;

/** Executes a job. Throwing means the attempt failed. */
public interface JobHandler {
    void run(Job job) throws Exception;
}
