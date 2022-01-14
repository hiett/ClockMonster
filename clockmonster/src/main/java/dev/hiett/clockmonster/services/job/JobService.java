package dev.hiett.clockmonster.services.job;

import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class JobService {

    @Inject
    JobDatabaseService jobDatabaseService;

    public Uni<IdentifiedJob> createJob(UnidentifiedJob job) {
        return jobDatabaseService.createJob(job);
    }

    public Uni<IdentifiedJob> getJob(long id) {
        return jobDatabaseService.getJob(id);
    }

    public Multi<IdentifiedJob> findJobsToProcess() {
        return jobDatabaseService.findJobs();
    }

    public Uni<Void> deleteJob(long id) {
        return jobDatabaseService.deleteJob(id);
    }

    /**
     * Will delete a job if its a one time job, or step it to the next iteration if multi run
     * @param job job to step
     * @return void
     */
    public Uni<Void> stepJob(IdentifiedJob job) {
        switch(job.getTime().getType()) {
            case ONCE: {
                return jobDatabaseService.deleteJob(job.getId());
            }
            case REPEATING: {
                // TODO: Update the job here
                return Uni.createFrom().voidItem();
            }
            default: return Uni.createFrom().voidItem();
        }
    }
}
