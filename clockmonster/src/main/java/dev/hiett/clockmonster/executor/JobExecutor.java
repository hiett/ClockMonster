package dev.hiett.clockmonster.executor;

import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class JobExecutor implements Runnable {

    @Inject
    JobService jobService;

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


        return true;
    }

    @Override
    public void run() {
        while(running) {
            List<IdentifiedJob> jobs = jobService.findJobsToProcess()
                    .subscribe().asStream().collect(Collectors.toList());

            for(IdentifiedJob job : jobs) {
                System.out.println("Executing job " + job.getId() + ", type=" + job.getTime().getType() + ", method="
                        + job.getAction().getType() + ", url=" + job.getAction().getUrl());

                boolean success = this.processJob(job);
                if (success) {
                    // Mark job as complete
                    jobService.stepJob(job).await().indefinitely();
                }
            }

            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
            }
        }
    }
}
