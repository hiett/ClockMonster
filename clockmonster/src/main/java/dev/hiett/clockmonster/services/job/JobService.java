package dev.hiett.clockmonster.services.job;

import dev.hiett.clockmonster.entities.job.IdentifiedJobImpl;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import dev.hiett.clockmonster.events.ClockMonsterEvent;
import dev.hiett.clockmonster.events.ClockMonsterEventDispatcherService;
import dev.hiett.clockmonster.services.cluster.ClusterCommunicationService;
import dev.hiett.clockmonster.services.cluster.ClusterService;
import dev.hiett.clockmonster.services.cluster.NodeIdService;
import dev.hiett.clockmonster.services.job.storage.JobStorageProviderService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JobService {

    @Inject
    JobStorageProviderService jobStorageProviderService;

    @Inject
    ClockMonsterEventDispatcherService eventDispatcherService;

    @Inject
    ClusterService clusterService;

    @Inject
    ClusterCommunicationService clusterCommunicationService;

    @Inject
    NodeIdService nodeIdService;

    public boolean isReady() {
        return jobStorageProviderService.isReady();
    }

    public Uni<IdentifiedJobImpl> createJob(UnidentifiedJob job) {
        return jobStorageProviderService.getCurrentImplementation().createJob(job)
                .invoke(identifiedJob ->
                        eventDispatcherService.dispatch(ClockMonsterEvent.JOB_CREATE.build(identifiedJob.getId())));
    }

    public Uni<IdentifiedJobImpl> getJob(long id) {
        return jobStorageProviderService.getCurrentImplementation().getJob(id);
    }

    public Multi<IdentifiedJobImpl> findJobsToProcess() {
        return jobStorageProviderService.getCurrentImplementation().findJobs(clusterService.getLookaheadPeriod(),
                clusterService.getLockTimeoutSeconds(),
                nodeIdService.getNodeId());
    }

    public Uni<Void> forceDeleteJob(long id) {
        return jobStorageProviderService.getCurrentImplementation().deleteJob(id)
                .invoke(r -> eventDispatcherService.dispatch(ClockMonsterEvent.JOB_REMOVE.build(id)));
    }

    public Uni<Void> safeDeleteJob(long id) {
        // We have to be careful here:
        // - The job may be queued for execution on this node
        // - The job may be queued for execution on *another* node!!
        // - the job may just be in the future
        // The fastest way to tell this state is by checking if a lock exists on the job
        // the value of this lock is the nodeID that has the job queued

        // Check if the job is locked
        return jobStorageProviderService.getCurrentImplementation().queryJobLock(id)
                .chain(nodeId -> {
                    if (nodeId == null)
                        return forceDeleteJob(id); // Safe to delete, not locked

                    return clusterCommunicationService.emitLiveJobDelete(nodeId, id);
                });
    }

    public Uni<Void> updateJob(IdentifiedJobImpl job) {
        return jobStorageProviderService.getCurrentImplementation().updateJob(job);
    }

    public Uni<Void> extendJobLock(long id) {
        return jobStorageProviderService.getCurrentImplementation().extendJobLock(id, clusterService.getLockTimeoutSeconds());
    }

    /**
     * Will delete a job if its a one time job, or step it to the next iteration if multi run
     * @param job job to step
     * @return void
     */
    public Uni<Void> stepJob(IdentifiedJobImpl job) {
        switch (job.getTime().getType()) {
            case ONCE -> {
                return this.forceDeleteJob(job.getId());
            }
            case REPEATING -> {
                // Check if the number of iterations has passed. Add one to remember that we haven't yet incremented this iteration
                if (job.getTime().getIterations() != -1 && job.getTime().getIterationsCount() + 1 >= job.getTime().getIterations()) {
                    // Delete the job
                    return this.forceDeleteJob(job.getId());
                }

                long newUnixTime = (System.currentTimeMillis() / 1000) + job.getTime().getInterval();

                job.getTime().setNextRunUnix(newUnixTime);
                job.getTime().incrementIterationsCount();

                return jobStorageProviderService.getCurrentImplementation()
                        .updateJob(job);
            }
            default -> {
                return Uni.createFrom().voidItem();
            }
        }
    }
}
