package dev.hiett.clockmonster.services.job.storage;

import dev.hiett.clockmonster.entities.job.IdentifiedJobImpl;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.LocalDateTime;

public interface JobStorageService {

    default void createConnection() {}

    Uni<IdentifiedJobImpl> createJob(UnidentifiedJob unidentifiedJob);

    Uni<IdentifiedJobImpl> getJob(long id);

    Multi<IdentifiedJobImpl> findJobs(float lookaheadPeriodSeconds, int lockTimeoutSeconds, long nodeId);

    Uni<Void> deleteJob(long id);

    Uni<Void> batchDeleteJobs(Long... ids);

    Uni<Void> updateJobTime(long id, LocalDateTime jobTime, boolean addIteration);

    Uni<Void> updateJob(IdentifiedJobImpl job);

    Uni<Void> extendJobLock(long id, int lockTimeoutSeconds);
}
