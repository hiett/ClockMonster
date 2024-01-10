package dev.hiett.clockmonster.services.dispatcher.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.action.ActionType;
import dev.hiett.clockmonster.entities.action.impls.SqsActionPayload;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.services.dispatcher.Dispatcher;
import io.smallrye.mutiny.Uni;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SqsDispatcher implements Dispatcher<SqsActionPayload> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Uni<Boolean> dispatchJob(IdentifiedJob job, SqsActionPayload actionPayload, Object payload) {
        SqsAsyncClient asyncClient = SqsAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return actionPayload.getAccessKeyId();
                    }

                    @Override
                    public String secretAccessKey() {
                        return actionPayload.getSecretAccessKey();
                    }
                }))
                .region(Region.of(actionPayload.getRegion()))
                .build();

        if (actionPayload.isWrapPayload()) {
            // Include the metadata in a wrapper
            // TODO;
        }

        // Encode the payload to JSON
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Uni.createFrom().item(false);
        }

        return Uni.createFrom()
                .completionStage(asyncClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(actionPayload.getQueueUrl())
                    .messageBody(jsonPayload)
                .build()))
                .onItem().transform(r -> true)
                .onFailure().recoverWithItem(false);
    }

    @Override
    public ActionType getActionType() {
        return ActionType.SQS;
    }
}
