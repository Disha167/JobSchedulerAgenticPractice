package com.practice.jobs;

import java.util.Set;

/**
 * A unit of work submitted by a tenant.
 * Higher priority runs first. A job may only start once every job in dependsOn has succeeded.
 * maxAttempts is the total number of runs allowed, including the first one.
 */
public record Job(String id, String tenantId, int priority, Set<String> dependsOn, int maxAttempts) {
    public Job {
        if (id == null || tenantId == null) {
            throw new IllegalArgumentException("id and tenantId are required");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        dependsOn = Set.copyOf(dependsOn);
    }

    public static Job of(String id, String tenantId, int priority) {
        return new Job(id, tenantId, priority, Set.of(), 1);
    }
}
