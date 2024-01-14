package dev.hiett.clockmonster.services.cluster.communication.messages;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClusterCommunicationMessage {

    private final UUID uuid;
    private final ClusterCommunicationMessageType type;
    private final Map<String, Object> payload;

    @ConstructorProperties({"uuid", "type", "payload"})
    public ClusterCommunicationMessage(UUID uuid, ClusterCommunicationMessageType type, Map<String, Object> payload) {
        this.uuid = uuid;
        this.type = type;
        this.payload = payload;
    }

    @ConstructorProperties({"type"})
    public ClusterCommunicationMessage(ClusterCommunicationMessageType type) {
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.payload = new HashMap<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public ClusterCommunicationMessageType getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
