package dev.hiett.clockmonster.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.failure.FailureConfiguration;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.TemporaryFailureJob;
import dev.hiett.clockmonster.events.ClockMonsterEvent;
import dev.hiett.clockmonster.events.ClockMonsterEventDispatcherService;
import dev.hiett.clockmonster.services.cluster.ClusterService;
import dev.hiett.clockmonster.services.dispatcher.DispatcherService;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Singleton
public class JobExecutor {

    private static final String SCHEDULER_IDENTITY = "clockmonster-job-executor";
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor(); // TODO: error handling

    @Inject
    Logger log;

    @Inject
    JobService jobService;

    @Inject
    DispatcherService dispatcherService;

    @Inject
    ClockMonsterEventDispatcherService eventDispatcherService;

    @Inject
    ClusterService clusterService;

    @Inject
    ObjectMapper objectMapper;

    private boolean processing = false;

    private float lastOffset = -1;

    private Future<?> timingFuture;
    private Map<Long, Future<?>> futureJobMap = new ConcurrentHashMap<>();

    @RunOnVirtualThread
    @Scheduled(every = "10s")
    void syncExecutorLoop() {
        float offset = clusterService.getOffset();
        if (offset == lastOffset)
            return; // Do nothing, offset has not changed

        log.info("Syncing executor timer loop, offset=" + offset + "s");

        // Unschedule the previous job
        if (this.timingFuture != null)
            this.timingFuture.cancel(true);

        // Find the next time going forward whereby it is rounded to the waitTimeSeconds
        int waitTimeSeconds = clusterService.getWaitTimeSeconds();
        long offsetMillis = (long) (offset * 1000L);

        this.timingFuture = EXECUTOR_SERVICE.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    long currentTime = System.currentTimeMillis();
                    long nextIterationTime = currentTime + (waitTimeSeconds * 1000L) - (currentTime % (waitTimeSeconds * 1000L));
                    long millisTillNextSleep = (nextIterationTime + offsetMillis) - currentTime;

                    // Adjust the wait time to account for the time it took to run the executor loop
                    if (millisTillNextSleep < 0) {
                        log.warn("Executor loop is behind by " + millisTillNextSleep + "ms, adjusting wait time to next iteration!");
                        millisTillNextSleep = (waitTimeSeconds * 1000L) + millisTillNextSleep;
                    }

                    if (millisTillNextSleep != 0)
                        Thread.sleep(millisTillNextSleep);
                } catch (InterruptedException e) {
                    return; // Totally okay, cancel was called
                }

                // Run the executor loop
//                log.info("Running executor loop...");
//                log.info("I AM RUNNING!!!!!!!!!!!!!");
//                log.info("Time= " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()));
//
//                long currentTimeMs = System.currentTimeMillis();
//                long timeSinceLastIteration = currentTimeMs - lastIterationTime;
//                log.info("Time since last iteration: " + timeSinceLastIteration + "ms");
//                lastIterationTime = currentTimeMs;

                this.executorLoop();
            }
        });

        this.lastOffset = offset;
    }

    private void executorLoop() {
        if (!jobService.isReady())
            return;

        log.info("Preparing execution of jobs at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()));

        processing = true;

        long jobExecutionStartTime = System.currentTimeMillis();
        Uni<List<IdentifiedJob>> jobList = jobService.findJobsToProcess().collect().asList();

        jobList.onItem().transform(arr -> arr.stream().map(this::handleJob).collect(Collectors.toList()))
                .onItem().transform(r -> r.isEmpty() ? null : r)
                .onItem().ifNull().fail()
                .chain(r -> Uni.join().all(r).andCollectFailures())
                .onItem().transform(resultList -> null)
                .onFailure().recoverWithItem(() -> null)
                .subscribe().with(res -> {
                    long jobExecutionElapsedTime = System.currentTimeMillis() - jobExecutionStartTime;
                    log.info("Job execution took " + jobExecutionElapsedTime + "ms.");

                    processing = false;
                });
    }

    private Uni<Void> handleJob(IdentifiedJob job) {
        try {
            String jsonOfJob = objectMapper.writeValueAsString(job);
            System.out.println(jsonOfJob);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        long nextRunUnix = job.getTime().getNextRunUnix();
        long currentTime = System.currentTimeMillis() / 1000;
        if (nextRunUnix <= currentTime) {
            long secondsDiff = currentTime - nextRunUnix;

            // The job is in the past, process it right now
            log.info("Job " + job.getId() + " is in the past, processing. It was " + secondsDiff + " seconds late.");

            return this.processJob(job, 0);
        }

        // We want to schedule it at the start of the second, so we need to deal in milliseconds here
        long waitTime = (nextRunUnix * 1000) - System.currentTimeMillis();
        log.info("Job " + job.getId() + " is in the future, waiting " + waitTime + "ms.");

        Future<?> jobFuture = EXECUTOR_SERVICE.submit(() -> {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                return; // Totally okay that it was interrupted - it has been cancelled
            }

            log.info("Firing job " + job.getId() + "!");

            // Fire off the process
            processJob(job, waitTime).subscribe().with((res) -> {
                log.info("Completed delayed job " + job.getId() + ".");
            });
        });
        futureJobMap.put(job.getId(), jobFuture);

        return Uni.createFrom().voidItem();
    }

    /**
     * Cancels a job that has been locked and internally queued on this node
     * @param jobId job id to cancel
     * @return true if the job was cancelled, false if it was not found
     */
    public boolean cancelFutureJob(long jobId) {
        Future<?> future = futureJobMap.remove(jobId);
        if (future != null) {
            future.cancel(true);
            return true;
        }

        return false;
    }

    public boolean isProcessing() {
        return processing;
    }

    private Uni<Void> processJob(IdentifiedJob job, long estTimeSinceLockCreationMs) {
        log.info("Executing job " + job.getId() + ", type=" + job.getTime().getType() + ", method="
                + job.getAction().getType());

        CompletableFuture<Void> jobCompletableFuture = dispatcherService.dispatchJob(job)
                .chain(successful -> {
                   if (successful) {
                       eventDispatcherService.dispatch(ClockMonsterEvent.JOB_INVOKE_SUCCESSFUL.build(job.getId()));

                       // Mark job as complete
                       return jobService.stepJob(job);
                   } else {
                       eventDispatcherService.dispatch(ClockMonsterEvent.JOB_INVOKE_FAILURE.build(job.getId()));

                       // Handle configuring based on the failure
                       FailureConfiguration failure = job.getFailure();
                       failure.incrementIterationsCount();

                       if (failure.getIterationsCount() > failure.getBackoff().size()) {
                           return Uni.createFrom().voidItem()
                                   .chain(v -> {
                                       // Delete the job
                                       if (failure.getDeadLetter() != null) {
                                           TemporaryFailureJob failureJob = new TemporaryFailureJob(job.getPayload(), job.getFailure().getDeadLetter());
                                           return dispatcherService.dispatchJob(failureJob).onItem().transform(r -> null); // Don't care if this fails
                                       }

                                       return Uni.createFrom().voidItem();
                                   }).chain(v -> {
                                       log.warn("Deleting job " + job.getId() + " because it failed max times.");
                                       return jobService.deleteJob(job.getId());
                                   });
                       } else {
                           // Update the job state based on the backoff time period
                           Long waitSeconds = failure.getBackoff().get(failure.getIterationsCount() - 1);
                           long currentTime = System.currentTimeMillis() / 1000;
                           job.getTime().setNextRunUnix(currentTime + waitSeconds);

                           return jobService.updateJob(job);
                       }
                   }
                }).subscribeAsCompletionStage();

        Future<?> jobMonitor = EXECUTOR_SERVICE.submit(() -> {
            long initialWaitTime = 7500 - estTimeSinceLockCreationMs;
            if (initialWaitTime > 0) { // Less than 0 and we want to do it right away
                try {
                    Thread.sleep(initialWaitTime);
                } catch (InterruptedException e) {
                    return; // Totally okay, cancel was called
                }
            }

            while (!jobCompletableFuture.isDone()) {
                // Extend the lock
                jobService.extendJobLock(job.getId()).subscribe().with((res) -> {
                    log.info("Extended lock on job " + job.getId() + " due to still processing.");
                });

                try {
                    Thread.sleep(7500); // Less than the lock amount, but for a good safety net
                } catch (InterruptedException e) {
                    return; // Totally okay, cancel was called
                }
            }
        });

        return Uni.createFrom().completionStage(jobCompletableFuture).onItem().invoke((u) -> {
            jobMonitor.cancel(true); // Cancel the job monitor, we no longer need to update the lock
        });
    }

    @Singleton
    public static class SkipPredicate implements Scheduled.SkipPredicate {

        @Inject
        JobExecutor jobExecutor;

        @Override
        public boolean test(ScheduledExecution execution) {
            return jobExecutor.isProcessing();
        }
    }
}
