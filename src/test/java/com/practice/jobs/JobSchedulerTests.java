package com.practice.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Dependency-free test suite. Run with ./run-tests.sh */
public class JobSchedulerTests {

    interface TestBody { void run() throws Exception; }
    record TestCase(String name, TestBody body) {}

    static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " | expected: " + expected + ", actual: " + actual);
        }
    }

    /** Small fixture: scheduler with a generous rate limit unless a test needs a tight one. */
    static class App {
        final ManualClock clock = new ManualClock(0);
        final ScriptedHandler handler = new ScriptedHandler();
        final JobScheduler scheduler;

        App() { this(1_000, 1_000, 100); }

        App(int maxPerWindow, long windowMillis, long baseBackoffMillis) {
            scheduler = new JobScheduler(clock, handler,
                    new SlidingWindowRateLimiter(maxPerWindow, windowMillis), baseBackoffMillis);
        }

        /** Runs jobs until nothing is eligible at the current time. */
        List<String> drain() {
            List<String> ran = new ArrayList<>();
            Optional<String> next;
            while ((next = scheduler.runNext()).isPresent()) {
                ran.add(next.get());
            }
            return ran;
        }
    }

    static Job dependent(String id, String... deps) {
        return new Job(id, "t1", 0, Set.of(deps), 1);
    }

    static List<TestCase> tests() {
        List<TestCase> t = new ArrayList<>();

//        t.add(new TestCase("higher priority runs first", () -> {
//            App app = new App();
//            app.scheduler.submit(Job.of("low", "t1", 1));
//            app.scheduler.submit(Job.of("high", "t1", 5));
//            assertEquals(List.of("high", "low"), app.drain(), "run order");
//        }));

//        t.add(new TestCase("equal priority runs in submission order", () -> {
//            App app = new App();
//            app.scheduler.submit(Job.of("first", "t1", 3));
//            app.scheduler.submit(Job.of("second", "t1", 3));
//            app.scheduler.submit(Job.of("third", "t1", 3));
//            assertEquals(List.of("first", "second", "third"), app.drain(), "FIFO within a priority");
//        }));
//
//        t.add(new TestCase("a job waits until its dependency succeeds", () -> {
//            App app = new App();
//            app.scheduler.submit(dependent("child", "parent"));
//            app.scheduler.submit(Job.of("parent", "t1", 0));
//            assertEquals(List.of("parent", "child"), app.drain(), "parent must run before child");
//        }));

//        t.add(new TestCase("a dependency that was never submitted blocks the job", () -> {
//            App app = new App();
//            app.scheduler.submit(dependent("orphan", "ghost"));
//            assertEquals(List.of(), app.drain(), "orphan must not run");
//            assertEquals(JobState.PENDING, app.scheduler.state("orphan"), "orphan stays pending");
//        }));

//        t.add(new TestCase("a dead-lettered dependency blocks the job", () -> {
//            App app = new App();
//            app.handler.failTimes("parent", ScriptedHandler.ALWAYS_FAIL);
//            app.scheduler.submit(Job.of("parent", "t1", 0));
//            app.scheduler.submit(dependent("child", "parent"));
//            app.drain();
//            assertEquals(JobState.DEAD_LETTERED, app.scheduler.state("parent"), "parent state");
//            assertEquals(JobState.PENDING, app.scheduler.state("child"), "child must not run");
//        }));

//        t.add(new TestCase("retries wait for exponential backoff", () -> {
//            App app = new App(1_000, 1_000, 100);
//            app.handler.failTimes("j", 2);
//            app.scheduler.submit(new Job("j", "t1", 0, Set.of(), 3));
//            assertEquals(List.of("j"), app.drain(), "attempt 1 at 0ms (fails)");
//            app.clock.set(99);
//            assertEquals(List.of(), app.drain(), "too early at 99ms");
//            app.clock.set(100);
//            assertEquals(List.of("j"), app.drain(), "attempt 2 at 100ms (fails)");
//            app.clock.set(299);
//            assertEquals(List.of(), app.drain(), "too early at 299ms");
//            app.clock.set(300);
//            assertEquals(List.of("j"), app.drain(), "attempt 3 at 300ms (succeeds)");
//            assertEquals(JobState.SUCCEEDED, app.scheduler.state("j"), "final state");
//        }));
//
//        t.add(new TestCase("a job is dead-lettered after exactly maxAttempts runs", () -> {
//            App app = new App(1_000, 1_000, 0);
//            app.handler.failTimes("j", ScriptedHandler.ALWAYS_FAIL);
//            app.scheduler.submit(new Job("j", "t1", 0, Set.of(), 3));
//            app.drain();
//            assertEquals(3, app.scheduler.attempts("j"), "attempts");
//            assertEquals(List.of("j"), app.scheduler.deadLetters(), "dead letters");
//            assertEquals("boom: j", app.scheduler.lastError("j"), "last error kept");
//        }));

//        t.add(new TestCase("rate limit frees up exactly when the window passes", () -> {
//            App app = new App(2, 1_000, 100);
//            app.scheduler.submit(Job.of("a", "t1", 0));
//            app.scheduler.submit(Job.of("b", "t1", 0));
//            app.scheduler.submit(Job.of("c", "t1", 0));
//            assertEquals(List.of("a", "b"), app.drain(), "two allowed at 0ms");
//            app.clock.set(999);
//            assertEquals(List.of(), app.drain(), "still limited at 999ms");
//            app.clock.set(1_000);
//            assertEquals(List.of("c"), app.drain(), "allowed again at 1000ms");
//        }));

        t.add(new TestCase("a rate-limited tenant does not block other tenants", () -> {
            App app = new App(1, 1_000, 100);
            app.scheduler.submit(Job.of("a1", "tenantA", 9));
            app.scheduler.submit(Job.of("a2", "tenantA", 9));
            app.scheduler.submit(Job.of("b1", "tenantB", 1));
            assertEquals(List.of("a1", "b1"), app.drain(), "b1 runs while tenantA is limited");
        }));

//        t.add(new TestCase("resubmitting a finished job is ignored", () -> {
//            App app = new App();
//            app.scheduler.submit(Job.of("j", "t1", 0));
//            app.drain();
//            assertEquals(false, app.scheduler.submit(Job.of("j", "t1", 0)), "duplicate submit");
//            app.drain();
//            assertEquals(JobState.SUCCEEDED, app.scheduler.state("j"), "state kept");
//            assertEquals(List.of("j"), app.handler.runs(), "ran exactly once");
//        }));

        return t;
    }

    public static void main(String[] args) {
        int passed = 0, failed = 0;
        for (TestCase test : tests()) {
            try {
                test.body().run();
                passed++;
                System.out.println("PASS  " + test.name());
            } catch (Throwable e) {
                failed++;
                System.out.println("FAIL  " + test.name() + "\n      " + e.getMessage());
            }
        }
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
