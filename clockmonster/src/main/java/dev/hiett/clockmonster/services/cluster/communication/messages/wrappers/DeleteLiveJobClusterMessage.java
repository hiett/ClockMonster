package dev.hiett.clockmonster.services.cluster.communication.messages.wrappers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.hiett.clockmonster.services.cluster.communication.messages.ClusterCommunicationMessage;
import dev.hiett.clockmonster.services.cluster.communication.messages.ClusterCommunicationMessageType;

import java.util.Map;
import java.util.UUID;

public class DeleteLiveJobClusterMessage extends ClusterCommunicationMessage {

    private static final String EXECUTOR_NODE_ID_KEY = "executorNodeId";
    private static final String REQUESTER_NODE_ID_KEY = "requesterNodeId";
    private static final String JOB_ID_KEY = "jobId";
    private static final String COMPLETION_STATE_KEY = "completionState";
    private static final String ERROR_MESSAGE_KEY = "errorMessage";

    public DeleteLiveJobClusterMessage(long executorNodeId, long requesterNodeId, long jobId) {
        super(UUID.randomUUID(), ClusterCommunicationMessageType.DELETE_LIVE_JOB, Map.of(
                EXECUTOR_NODE_ID_KEY, executorNodeId,
                REQUESTER_NODE_ID_KEY, requesterNodeId,
                JOB_ID_KEY, jobId
        ));
    }

    public DeleteLiveJobClusterMessage(ClusterCommunicationMessage baseMessage) {
        super(baseMessage.getUuid(), ClusterCommunicationMessageType.DELETE_LIVE_JOB, baseMessage.getPayload());
    }

    @JsonIgnore
    public long getExecutorNodeId() {
        return (long) getPayload().getOrDefault(EXECUTOR_NODE_ID_KEY, -1);
    }

    @JsonIgnore
    public long getRequesterNodeId() {
        return (long) getPayload().getOrDefault(REQUESTER_NODE_ID_KEY, -1);
    }

    @JsonIgnore
    public long getJobId() {
        return (long) getPayload().getOrDefault(JOB_ID_KEY, -1);
    }

    @JsonIgnore
    public boolean getCompletionState() {
        return (boolean) getPayload().getOrDefault(COMPLETION_STATE_KEY, false);
    }

    @JsonIgnore
    public String getErrorMessage() {
        return (String) getPayload().get(ERROR_MESSAGE_KEY);
    }

    @JsonIgnore
    public boolean isResponse() {
        return getPayload().containsKey(COMPLETION_STATE_KEY);
    }

    public void setCompletionState(boolean success, String errorMessage) {
        getPayload().put(COMPLETION_STATE_KEY, success);
        getPayload().put(ERROR_MESSAGE_KEY, errorMessage);
    }
}
