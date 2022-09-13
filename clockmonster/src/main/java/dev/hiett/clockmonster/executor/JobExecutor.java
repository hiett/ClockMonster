package dev.hiett.clockmonster.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.failure.FailureConfiguration;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.services.dispatcher.DispatcherService;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class JobExecutor implements Runnable {

    @Inject
    Logger log;

    @Inject
    JobService jobService;

    @Inject
    DispatcherService dispatcherService;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "clockmonster.executor.wait-seconds", defaultValue = "5")
    int waitTimeSeconds;

    private Thread executorThread;
    private boolean running;

    void onStart(@Observes StartupEvent startupEvent) {
        this.running = true;
        this.executorThread = new Thread(this);
        this.executorThread.start();
    }

    void onStop(@Observes ShutdownEvent shutdownEvent) {
        running = false;
        executorThread.interrupt();
    }

    private boolean processJob(IdentifiedJob job) {
        // TODO: Introduce a timeout here
        return dispatcherService.dispatchJob(job).await().indefinitely();
    }

    @Override
    public void run() {
        log.info("Running JobExecutor at wait delay of " + waitTimeSeconds + "s");

        while(running) {
            if(!jobService.isReady())
                continue;

            long jobExecutionStartTime = System.currentTimeMillis();

            List<IdentifiedJob> jobs = jobService.findJobsToProcess()
                    .subscribe().asStream().collect(Collectors.toList());

            for(IdentifiedJob job : jobs) {
                String payloadString = "{}";

                try {
                    payloadString = objectMapper.writeValueAsString(job.getAction().getPayload());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                log.info("Executing job " + job.getId() + ", type=" + job.getTime().getType() + ", method="
                        + job.getAction().getType() + ", config=" + payloadString);

                boolean success = this.processJob(job);
                if (success) {
                    // Mark job as complete
                    jobService.stepJob(job).await().indefinitely();
                } else {
                    // Handle configuring based on the failure
                    FailureConfiguration failure = job.getFailure();
                    failure.incrementIterationsCount();

                    if(failure.getIterationsCount() > failure.getBackoff().size()) {
                        // Delete the job
                        if(failure.getDeadLetter() != null) {
                            // TODO: Fire to the dead letter queue
                            log.info("Firing to dead letter queue");
                        }

                        log.warn("Deleting job " + job.getId() + " because it failed max times.");
                        jobService.deleteJob(job.getId()).await().indefinitely();
                    } else {
                        // Update the job state based on the backoff time period
                        Long waitSeconds = failure.getBackoff().get(failure.getIterationsCount() - 1);
                        long currentTime = System.currentTimeMillis() / 1000;
                        job.getTime().setNextRunUnix(currentTime + waitSeconds);

                        jobService.updateJob(job).await().indefinitely();
                    }
                }
            }

            long jobExecutionElapsedTime = System.currentTimeMillis() - jobExecutionStartTime;

            log.info("Job execution took " + jobExecutionElapsedTime + "ms.");

            try {
                long waitTimeMs = waitTimeSeconds * 1000L;
                long sleepDuration = Math.max(waitTimeMs - jobExecutionElapsedTime, 0);
                if(jobExecutionElapsedTime > waitTimeMs)
                    log.warn("Job execution took longer than executor wait time! Not waiting before running next iteration");

                Thread.sleep(sleepDuration);
            } catch (InterruptedException e) {
            }
        }
    }
}
