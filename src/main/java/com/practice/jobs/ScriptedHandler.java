package com.practice.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test double: each job fails a configured number of times, then succeeds. Records run order. */
public class ScriptedHandler implements JobHandler {
    public static final int ALWAYS_FAIL = Integer.MAX_VALUE;

    private final Map<String, Integer> failuresLeft = new HashMap<>();
    private final List<String> runs = new ArrayList<>();

    public ScriptedHandler failTimes(String jobId, int times) {
        failuresLeft.put(jobId, times);
        return this;
    }

    @Override
    public synchronized void run(Job job) throws Exception {
        runs.add(job.id());
        int left = failuresLeft.getOrDefault(job.id(), 0);
        if (left > 0) {
            failuresLeft.put(job.id(), left == ALWAYS_FAIL ? left : left - 1);
            throw new Exception("boom: " + job.id());
        }
    }

    public synchronized List<String> runs() {
        return new ArrayList<>(runs);
    }
}
