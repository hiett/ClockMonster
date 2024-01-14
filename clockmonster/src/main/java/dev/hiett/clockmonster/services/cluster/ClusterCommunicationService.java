package dev.hiett.clockmonster.services.cluster;

import dev.hiett.clockmonster.services.cluster.communication.RemoteClusterCommunicationServiceProvider;
import dev.hiett.clockmonster.services.cluster.communication.messages.wrappers.DeleteLiveJobClusterMessage;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class ClusterCommunicationService {

    public static final String DELETE_JOB_EVENT_BUS_EVENT = "clockmonster.job.delete";
    public static final String REMOTE_DELETE_JOB_RESPONSE_EVENT_BUS_EVENT = "clockmonster.remote.job.delete.response";
    public static final String REMOTE_DELETE_JOB_EVENT_BUS_EVENT = "clockmonster.remote.job.delete";

    @Inject
    NodeIdService nodeIdService;

    @Inject
    EventBus eventBus;

    @Inject
    JobService jobService;

    @Inject
    Logger log;

    @Inject
    RemoteClusterCommunicationServiceProvider remoteClusterCommunicationServiceProvider;

    private final Map<UUID, CompletableFuture<Void>> deleteJobResponseWaiters = new ConcurrentHashMap<>();

    public Uni<Void> emitLiveJobDelete(long nodeId, long jobId) {
        if (nodeId == nodeIdService.getNodeId()) {
            // We are this node: we can just directly emit to this to ourselves
            return emitDeleteToSelf(jobId);
        }

        DeleteLiveJobClusterMessage message = new DeleteLiveJobClusterMessage(nodeId, nodeIdService.getNodeId(), jobId);
        remoteClusterCommunicationServiceProvider.sendJobDelete(message);

        CompletableFuture<Void> future = new CompletableFuture<Void>()
                .orTimeout(10, TimeUnit.SECONDS);
        deleteJobResponseWaiters.put(message.getUuid(), future);

        future.whenComplete((v, t) -> {
            // Make sure we clean up from the waiter map no matter the outcome.
            log.info("Cleaning up waiter map for message: " + message.getUuid());
            deleteJobResponseWaiters.remove(message.getUuid());
        });

        return Uni.createFrom().completionStage(future);
    }

    @ConsumeEvent(REMOTE_DELETE_JOB_EVENT_BUS_EVENT)
    public Uni<Void> onRequestRemoteJobDelete(DeleteLiveJobClusterMessage message) {
        // We need to emit this now locally, as this has been sent to us
        if (message.getExecutorNodeId() != nodeIdService.getNodeId()) {
            // This is real bad, we should never receive a message that is not for us
            log.warn("Received remote job delete request for node that is not us: " + message.getExecutorNodeId());
            return Uni.createFrom().voidItem();
        }

        // We are the executor node, so we need to emit this to ourselves
        return this.emitDeleteToSelf(message.getJobId())
                .invoke(() -> {
                    message.setCompletionState(true, null); // TODO: error handling

                    // The message is now in a completed state, so we want to send it back to the original node
                    remoteClusterCommunicationServiceProvider.sendJobDeleteResponse(message);
                });
    }

    @ConsumeEvent(REMOTE_DELETE_JOB_RESPONSE_EVENT_BUS_EVENT)
    public void onRemoteJobDelete(DeleteLiveJobClusterMessage message) {
        // Find the CompletableFuture for this message
        CompletableFuture<Void> future = deleteJobResponseWaiters.get(message.getUuid());
        if (future == null) {
            log.warn("Received remote job delete response for message that does not exist in waiter map: " + message.getUuid());
            return;
        }

        // Complete this, which will resolve in the uni
        future.complete(null);
    }

    private Uni<Void> emitDeleteToSelf(long jobId) {
        Future<Message<Boolean>> future = eventBus.request(DELETE_JOB_EVENT_BUS_EVENT, jobId);
        return Uni.createFrom().completionStage(future.toCompletionStage())
                .onItem().transform(Message::body)
                .chain(cancelledLocally -> {
                    if (!cancelledLocally)
                        log.info("Warning: attempted to cancel local job in future map, however it was missing. Deleting from database anyway.");

                    // The job was cancelled locally, so we want to delete from the database
                    return jobService.forceDeleteJob(jobId);
                });
    }
}
