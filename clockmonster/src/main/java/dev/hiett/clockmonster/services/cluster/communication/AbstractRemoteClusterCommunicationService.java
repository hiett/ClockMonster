package dev.hiett.clockmonster.services.cluster.communication;

import dev.hiett.clockmonster.services.cluster.ClusterCommunicationService;
import dev.hiett.clockmonster.services.cluster.communication.messages.wrappers.DeleteLiveJobClusterMessage;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;

public abstract class AbstractRemoteClusterCommunicationService implements RemoteClusterCommunicationService {

    @Inject
    EventBus eventBus;

    @Override
    public void handleInboundJobDelete(DeleteLiveJobClusterMessage message) {
        eventBus.publish(ClusterCommunicationService.REMOTE_DELETE_JOB_EVENT_BUS_EVENT, message);
    }

    @Override
    public void handleInboundJobDeleteResponse(DeleteLiveJobClusterMessage message) {
        eventBus.publish(ClusterCommunicationService.REMOTE_DELETE_JOB_RESPONSE_EVENT_BUS_EVENT, message);
    }
}
