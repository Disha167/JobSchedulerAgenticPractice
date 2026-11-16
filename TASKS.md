# Tasks for stages 2 and 3

Write tests for everything you add. Keep all existing tests passing.

## Feature A (stage 2): cancel, and reject dependency cycles

1. `cancel(jobId)`: a PENDING job becomes CANCELLED (add the state) and never runs.
   Every job that depends on it, directly or through other jobs, is cancelled too.
   Cancelling a job that already succeeded returns false and changes nothing.
2. `submit` must reject a job that would create a dependency cycle
   (A depends on B, B depends on A, or longer loops). Return false, store nothing.
   Dependencies that haven't been submitted yet are allowed (they just wait).
3. Extra: when a job is dead-lettered, its dependents can never run. Should they be
   dead-lettered too, cancelled, or left pending? Decide, write the rule down, test it.

## Task B (stage 3): concurrency and performance

1. Run ./run-drill.sh a few times. Explain why jobs run more than once.
2. Make it safe for several worker threads. Then explain the trade-off: does your fix
   let two different jobs run at the same time, or does it serialize all job execution?
   (The best answer claims the job under a lock, runs it outside the lock, and records
   the result under the lock again.)
3. `runNext` scans and sorts every job on every call. With 1 million queued jobs, what
   would you change? Think: a priority queue of ready jobs, a delay queue for backoff,
   and a map from a job to the jobs waiting on it.
4. If the scheduler ran on several machines sharing one database, how would you stop
   two machines from running the same job?
