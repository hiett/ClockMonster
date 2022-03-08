package dev.hiett.clockmonster.services.job.storage;

import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.LocalDateTime;

public interface JobStorageService {

    default void createConnection() {}

    Uni<IdentifiedJob> createJob(UnidentifiedJob unidentifiedJob);

    Uni<IdentifiedJob> getJob(long id);

    Multi<IdentifiedJob> findJobs();

    Uni<Void> deleteJob(long id);

    Uni<Void> batchDeleteJobs(Long... ids);

    Uni<Void> updateJobTime(long id, LocalDateTime jobTime, boolean addIteration);
}
