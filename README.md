# Agentic coding practice 3: job scheduler with retries and rate limits

A system you haven't seen: a background job scheduler. It combines the problems most
often reported in 2026 AI-assisted coding rounds across companies:
- a **task queue with backoff retries and a dead-letter list** (reported at Snowflake),
- a **sliding-window rate limiter** (Snowflake, Scale AI, many others),
- a **dependency-aware task scheduler** (Scale AI, PracHub's AI-coding list),
- priority ordering with a heap-style comparator (Task Scheduler / LeetCode style).

No frameworks or downloads needed. Requires JDK 17+.

    ./run-tests.sh     # unit tests (8 fail at the start, on purpose)
    ./run-drill.sh     # stage 3 concurrency drill

## What the code does

Tenants submit jobs. `runNext()` picks the highest-priority job (ties: submission order)
whose dependencies have all succeeded and whose retry backoff has passed, checks the
tenant's rate limit, and runs it. Failures retry with exponential backoff
(base, 2x, 4x...) until `maxAttempts`, then the job is dead-lettered.

Files: `Job` (input), `JobRecord` (internal state), `JobScheduler` (the logic),
`SlidingWindowRateLimiter`, `ScriptedHandler` (test double), `ManualClock`.

## Interview-style rules for this session

1. Timer on, narrate out loud. First 5 minutes: read `JobScheduler`'s class comment —
   it is the spec. Most bugs are a gap between that comment and the code.
2. For each failing test: read the assertion, say a hypothesis, then ask the agent a
   targeted question ("which lines decide X?"). Compare its answer with yours.
3. Ask for the smallest fix, review the diff, re-run tests.
4. After each fix, ask the agent to explain it in two sentences and name one edge case.

**Target: under 30 minutes for stage 1.** There are 7 bugs; fixing one can reveal
another. Some patterns will feel familiar from the last two repos. That's the point:
they recur.

## Session plan

| Stage | Time | Task |
|---|---|---|
| 0 | 5 min | Understand: read the class comment; list the 6 rules it promises |
| 1 | 30 min | Debug: make all tests pass (7 bugs) |
| 2 | 25 min | Build: TASKS.md, feature A |
| 3 | 15 min | Concurrency + performance: TASKS.md, task B |
| 4 | 15 min | Review: review/PR-311.md |

Don't open SOLUTIONS.md until you've finished.
