package dev.hiett.clockmonster.services.cluster.communication;

import dev.hiett.clockmonster.services.cluster.NodeIdService;
import dev.hiett.clockmonster.services.cluster.communication.impls.RedisRemoteClusterCommunicationService;
import dev.hiett.clockmonster.services.cluster.communication.messages.wrappers.DeleteLiveJobClusterMessage;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class RemoteClusterCommunicationServiceProvider implements RemoteClusterCommunicationService {

    @Inject
    NodeIdService nodeIdService;

    // In the future, we can have other implementations here
    @Inject
    RedisRemoteClusterCommunicationService redisImpl;

    void onStartup(@Observes StartupEvent event) {
        this.getImpl().createConnection(nodeIdService.getNodeId());
    }

    private RemoteClusterCommunicationService getImpl() {
        return redisImpl; // TODO: Eventually, have multiple options here
    }

    // Delegate methods to the implementation

    @Override
    public void sendJobDelete(DeleteLiveJobClusterMessage message) {
        getImpl().sendJobDelete(message);
    }

    @Override
    public void handleInboundJobDelete(DeleteLiveJobClusterMessage message) {
        getImpl().handleInboundJobDelete(message);
    }

    @Override
    public void sendJobDeleteResponse(DeleteLiveJobClusterMessage message) {
        getImpl().handleInboundJobDeleteResponse(message);
    }

    @Override
    public void handleInboundJobDeleteResponse(DeleteLiveJobClusterMessage message) {
        getImpl().handleInboundJobDeleteResponse(message);
    }
}
