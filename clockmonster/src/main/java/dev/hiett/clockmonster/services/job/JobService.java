package dev.hiett.clockmonster.services.job;

import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
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
}
