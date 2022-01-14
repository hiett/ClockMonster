package dev.hiett.clockmonster.services.job;

import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
                // Check if the number of iterations has passed. Add one to remember that we haven't yet incremented this iteration
                if(job.getTime().getIterations() != -1 && job.getTime().getIterationsCount() + 1 >= job.getTime().getIterations()) {
                    // Delete the job
                    return jobDatabaseService.deleteJob(job.getId());
                }

                long newUnixTime = (System.currentTimeMillis() / 1000) + job.getTime().getInterval();
                LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(newUnixTime, 0, ZoneOffset.UTC);

                // Update the job time and number of iterations
                return jobDatabaseService.updateJobTime(job.getId(), localDateTime, true);
            }
            default: return Uni.createFrom().voidItem();
        }
    }
}
