package dev.hiett.clockmonster.services.job.storage;

import dev.hiett.clockmonster.entities.job.IdentifiedJobImpl;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface JobStorageService {

    default void createConnection() {}

    // JOB CRUD OPERATIONS ////////////////////////////////
    Uni<IdentifiedJobImpl> createJob(UnidentifiedJob unidentifiedJob);

    Uni<IdentifiedJobImpl> getJob(long id);

    Uni<Void> updateJob(IdentifiedJobImpl job);

    Uni<Void> deleteJob(long id);
    // END JOB CRUD OPERATIONS ////////////////////////////

    // JOB DISCOVERY OPERATIONS ///////////////////////////
    Multi<IdentifiedJobImpl> findJobs(float lookaheadPeriodSeconds, int lockTimeoutSeconds, long nodeId);
    // END JOB DISCOVERY OPERATIONS ///////////////////////

    // JOB LOCK OPERATIONS ////////////////////////////////
    Uni<Void> extendJobLock(long id, int lockTimeoutSeconds);

    /**
     * Attempts to query a lock on the job. Will return null if the lock doesn't exists. The long value is
     * the NodeID which owns the job.
     * @param id  The job id
     * @return The nodeID which owns the lock or null if the lock doesn't exist
     */
    Uni<Long> queryJobLock(long id);

    Uni<Void> deleteJobLock(long id);
    // END JOB LOCK OPERATIONS ////////////////////////////
}
