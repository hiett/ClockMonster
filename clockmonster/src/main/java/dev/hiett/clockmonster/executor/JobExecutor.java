package dev.hiett.clockmonster.executor;

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
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class JobExecutor {

    private static final String SCHEDULER_IDENTITY = "clockmonster-job-executor";

    @Inject
    Scheduler scheduler;

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

    private boolean processing = false;

    private float lastOffset = 0;

    @RunOnVirtualThread
    @Scheduled(every = "10s")
    void syncExecutorLoop() {
        log.info("Syncing executor loop...");

        float offset = clusterService.getOffset();
        if (offset == lastOffset) {
            log.info("Offset hasn't changed, not rescheduling job.");
            return;
        }

        // Unschedule the previous job
        scheduler.unscheduleJob(SCHEDULER_IDENTITY);

        // The offset changed, we need to reschedule the job to be back in line
        // with where our node should be
        // We need to get the next time whereby the offset is adjusted to be 0

        long current = System.currentTimeMillis();
        // Find the next time going forward whereby it is rounded to the waitTimeSeconds
        long next = current + (clusterService.getWaitTimeSeconds() * 1000L) - (current % (clusterService.getWaitTimeSeconds() * 1000L));
        long offsetMillis = (long) (offset * 1000L);

        long millisTillNext = (next + offsetMillis) - current;

        // Round down to get seconds to next, and then the remaining millis
        // for some reason when delaying a scheduled job, it will only accept seconds so it will round any decimals
        // we want to avoid that behaviour, so we grab the remaining millis and delay intentionally inside the virtual thread.
        long secondsToNext = millisTillNext / 1000L;
        long remainingMillis = millisTillNext % 1000L;

        log.info("Waiting " + secondsToNext + "s then " + remainingMillis + "ms before first job iteration");

        // Create a new job with the same specification, but with the new offset
        scheduler.newJob(SCHEDULER_IDENTITY)
                .setDelayed(secondsToNext + "s")
                .setInterval(clusterService.getWaitTimeSeconds() + "s")
                .setConcurrentExecution(Scheduled.ConcurrentExecution.SKIP)
//                .setSkipPredicate(skipPredicate)
                .setTask(scheduledExecution -> {
                    try {
                        // Artificially delay the job to be in line with the offset
                        Thread.sleep(remainingMillis);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.println("I AM RUNNING!!!!!!!!!!!!!");
                    System.out.println("Time= " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()));
                }, true)
                .schedule();

        this.lastOffset = offset;
    }

    void executorLoop() {
        if (!jobService.isReady())
            return;

        System.out.println("Executing job at " + System.currentTimeMillis() + " time= " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()));

        processing = true;

        long jobExecutionStartTime = System.currentTimeMillis();
        Uni<List<IdentifiedJob>> jobList = jobService.findJobsToProcess().collect().asList();

        jobList.subscribe().with(res -> {
            res.forEach(this::handleJob);

            processing = false;
        });

//        jobList.onItem().transform(arr -> arr.stream().map(this::processJob).collect(Collectors.toList()))
//                .onItem().transform(r -> r.isEmpty() ? null : r)
//                .onItem().ifNull().fail()
//                .chain(r -> Uni.join().all(r).andCollectFailures())
//                .onItem().transform(resultList -> null)
//                .onFailure().recoverWithItem(() -> null)
//                .subscribe().with(res -> {
//                    long jobExecutionElapsedTime = System.currentTimeMillis() - jobExecutionStartTime;
//                    log.info("Job execution took " + jobExecutionElapsedTime + "ms.");
//
//                    processing = false;
//                });
    }

    private void handleJob(IdentifiedJob job) {
        long nextRunUnix = job.getTime().getNextRunUnix();
        long currentTime = System.currentTimeMillis() / 1000;
        if (nextRunUnix <= currentTime) {
            long secondsDiff = currentTime - nextRunUnix;

            // The job is in the past, process it
            log.info("Job " + job.getId() + " is in the past, processing. It was " + secondsDiff + " seconds late.");
            return;
        }

        // We want to schedule it at the start of the second, so we need to deal in milliseconds here
        long waitTime = (nextRunUnix * 1000) - System.currentTimeMillis();

        log.info("Job " + job.getId() + " is in the future, waiting " + waitTime + "ms.");
    }

    public boolean isProcessing() {
        return processing;
    }

    private Uni<Void> processJob(IdentifiedJob job) {
        log.info("Executing job " + job.getId() + ", type=" + job.getTime().getType() + ", method="
                + job.getAction().getType());

        return dispatcherService.dispatchJob(job)
                .chain(successful -> {
                   if(successful) {
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
