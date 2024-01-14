package dev.hiett.clockmonster.services.cluster.communication.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.services.cluster.communication.AbstractRemoteClusterCommunicationService;
import dev.hiett.clockmonster.services.cluster.ClusterRedisKeys;
import dev.hiett.clockmonster.services.cluster.NodeIdService;
import dev.hiett.clockmonster.services.cluster.communication.messages.ClusterCommunicationMessage;
import dev.hiett.clockmonster.services.cluster.communication.messages.wrappers.DeleteLiveJobClusterMessage;
import dev.hiett.clockmonster.services.redis.RedisService;
import io.vertx.mutiny.redis.client.Response;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.util.List;

@Singleton
public class RedisRemoteClusterCommunicationService extends AbstractRemoteClusterCommunicationService {

    @Inject
    RedisService redisService;

    @Inject
    ClusterRedisKeys clusterRedisKeys;

    @Inject
    NodeIdService nodeIdService;

    @Inject
    Logger log;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void createConnection(long localNodeId) {
        List<String> channelNames = List.of(
                clusterRedisKeys.getPubSubGlobalKey(),
                clusterRedisKeys.getPubSubKey(localNodeId)
        );
        redisService.createPubSubSubscriber(channelNames, this::handlePubSubMessage);
    }

    private void handlePubSubMessage(Response channel, Response message) {
        String channelName = channel.toString();
        String messageString = message.toString();

        // All messages conform to a ClusterCommunicationMessage, so we can parse to that
        ClusterCommunicationMessage clusterCommunicationMessage;
        try {
            clusterCommunicationMessage = objectMapper.readValue(messageString, ClusterCommunicationMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("Received PubSub message that does not conform to ClusterCommunicationMessage: " + channelName + ", " + messageString);
            return;
        }

        if (channelName.equals(clusterRedisKeys.getPubSubGlobalKey())) {
            // Global message, here for future use
            return;
        }

        if (channelName.equals(clusterRedisKeys.getPubSubKey(nodeIdService.getNodeId()))) {
            // Message for us, here for future use
            switch (clusterCommunicationMessage.getType()) {
                case DELETE_LIVE_JOB -> {
                    DeleteLiveJobClusterMessage deleteLiveJobClusterMessage = new DeleteLiveJobClusterMessage(clusterCommunicationMessage);
                    this.handleDeleteJobMessage(deleteLiveJobClusterMessage);
                }
                default -> log.warn("Received PubSub message with unknown type: " + clusterCommunicationMessage.getType());
            }

            return;
        }

        log.warn("Received PubSub message on unknown channel: " + channelName);
    }

    private void handleDeleteJobMessage(DeleteLiveJobClusterMessage deleteMessage) {
        if (deleteMessage.isResponse()) {
            this.handleInboundJobDeleteResponse(deleteMessage);
        } else {
            this.handleInboundJobDelete(deleteMessage);
        }
    }

    @Override
    public void sendJobDelete(DeleteLiveJobClusterMessage message) {
        this.sendPubSubAndForget(clusterRedisKeys.getPubSubKey(message.getExecutorNodeId()), message);
    }

    @Override
    public void sendJobDeleteResponse(DeleteLiveJobClusterMessage message) {
        this.sendPubSubAndForget(clusterRedisKeys.getPubSubKey(message.getRequesterNodeId()), message);
    }

    private void sendPubSubAndForget(String channel, Object pojo) {
        String message;
        try {
            message = objectMapper.writeValueAsString(pojo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        redisService.getRedis().publishAndForget(channel, message);
    }
}
