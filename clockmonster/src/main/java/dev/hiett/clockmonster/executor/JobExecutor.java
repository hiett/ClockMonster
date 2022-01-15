package dev.hiett.clockmonster.executor;

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
            List<IdentifiedJob> jobs = jobService.findJobsToProcess()
                    .subscribe().asStream().collect(Collectors.toList());

            for(IdentifiedJob job : jobs) {
                log.info("Executing job " + job.getId() + ", type=" + job.getTime().getType() + ", method="
                        + job.getAction().getType() + ", url=" + job.getAction().getUrl());

                boolean success = this.processJob(job);
                if (success) {
                    // Mark job as complete
                    jobService.stepJob(job).await().indefinitely();
                }
            }

            try {
                Thread.sleep(waitTimeSeconds * 1000L);
            } catch (InterruptedException e) {
            }
        }
    }
}
