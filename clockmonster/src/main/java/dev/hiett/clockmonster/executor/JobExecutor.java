package dev.hiett.clockmonster.executor;

import dev.hiett.clockmonster.entities.failure.FailureConfiguration;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.TemporaryFailureJob;
import dev.hiett.clockmonster.events.ClockMonsterEvent;
import dev.hiett.clockmonster.events.ClockMonsterEventDispatcherService;
import dev.hiett.clockmonster.services.dispatcher.DispatcherService;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class JobExecutor {

    @Inject
    Logger log;

    @Inject
    JobService jobService;

    @Inject
    DispatcherService dispatcherService;

    @Inject
    ClockMonsterEventDispatcherService eventDispatcherService;

    private boolean processing = false;

//    @ConfigProperty(name = "clockmonster.executor.wait-seconds", defaultValue = "5")
//    int waitTimeSeconds;

    @Scheduled(
            every = "{clockmonster.executor.wait-seconds}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP,
            skipExecutionIf = SkipPredicate.class
    )
    void executorLoop() {
        if (!jobService.isReady())
            return;

        processing = true;

        long jobExecutionStartTime = System.currentTimeMillis();
        Uni<List<IdentifiedJob>> jobList = jobService.findJobsToProcess().collect().asList();

        jobList.onItem().transform(arr -> arr.stream().map(this::processJob).collect(Collectors.toList()))
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
