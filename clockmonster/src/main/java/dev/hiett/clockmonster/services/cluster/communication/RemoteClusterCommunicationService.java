package dev.hiett.clockmonster.services.cluster.communication;

import dev.hiett.clockmonster.services.cluster.communication.messages.wrappers.DeleteLiveJobClusterMessage;

public interface RemoteClusterCommunicationService {

    /**
     * Called when this communication method is selected as the one to use.
     * Allows for setup of any listeners required to call the methods below
     * @param localNodeId the id of the node that is calling this method
     */
    default void createConnection(long localNodeId) {}

    void sendJobDelete(DeleteLiveJobClusterMessage message);

    void handleInboundJobDelete(DeleteLiveJobClusterMessage message);

    void sendJobDeleteResponse(DeleteLiveJobClusterMessage message);

    void handleInboundJobDeleteResponse(DeleteLiveJobClusterMessage message);
}
