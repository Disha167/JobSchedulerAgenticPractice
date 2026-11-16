package com.practice.jobs;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stage 3 drill. 8 worker threads call runNext() until 2,000 jobs are done.
 * Each job does a little "I/O". Expected: every job runs exactly once, 0 errors.
 * Run with ./run-drill.sh (results vary run to run).
 */
public class ConcurrencyDrill {
    public static void main(String[] args) throws Exception {
        int jobsCount = 2_000, workers = 8;
        ConcurrentHashMap<String, AtomicInteger> runs = new ConcurrentHashMap<>();
        JobHandler handler = job -> {
            runs.computeIfAbsent(job.id(), k -> new AtomicInteger()).incrementAndGet();
            Thread.sleep(0, 200_000); // simulated I/O
        };
        JobScheduler scheduler = new JobScheduler(System::currentTimeMillis, handler,
                new SlidingWindowRateLimiter(1_000_000, 1_000), 10);
        for (int i = 0; i < jobsCount; i++) {
            scheduler.submit(Job.of("job-" + i, "tenant-" + (i % 5), i % 3));
        }

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        AtomicInteger errors = new AtomicInteger();
        long deadline = System.currentTimeMillis() + 20_000;
        for (int w = 0; w < workers; w++) {
            pool.submit(() -> {
                try {
                    while (System.currentTimeMillis() < deadline) {
                        if (scheduler.runNext().isEmpty() && runs.size() >= jobsCount) {
                            break;
                        }
                    }
                } catch (Throwable e) {
                    errors.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        long duplicates = runs.values().stream().filter(c -> c.get() > 1).count();
        Set<String> ran = runs.keySet();
        System.out.println("jobs submitted:        " + jobsCount);
        System.out.println("jobs that ran:         " + ran.size());
        System.out.println("jobs that ran twice+:  " + duplicates);
        System.out.println("worker errors:         " + errors.get());
        System.exit(0);
    }
}
