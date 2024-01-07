package dev.hiett.clockmonster.events.impls;

import dev.hiett.clockmonster.events.AnnounceEventMethodDispatcher;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Singleton
public class SqsAnnounceEventMethod implements AnnounceEventMethodDispatcher {

    @Inject
    Logger log;
    
    @ConfigProperty(name = "clockmonster.announce-events.method.sqs.queue-url")
    String queueUrl;

    @ConfigProperty(name = "clockmonster.announce-events.method.sqs.access-key-id")
    String accessKeyId;

    @ConfigProperty(name = "clockmonster.announce-events.method.sqs.secret-access-key")
    String secretAccessKey;

    @ConfigProperty(name = "clockmonster.announce-events.method.sqs.region")
    String region;

    private SqsAsyncClient asyncClient;

    @Override
    public void onCreate() {
        // Validate that the SQS dispatcher has been correctly set up
        if(queueUrl == null || accessKeyId == null || secretAccessKey == null || region == null) {
            log.warn("SQS event announce method was selected, but is not configured correctly. Please ensure all SQS " +
                    "environment variables are provided.");
            return;
        }

        asyncClient = SqsAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return accessKeyId;
                    }

                    @Override
                    public String secretAccessKey() {
                        return secretAccessKey;
                    }
                }))
                .region(Region.of(region))
                .build();
    }

    @Override
    public Uni<Void> dispatch(String json) {
        if(asyncClient == null)
            return Uni.createFrom().voidItem(); // Client doesn't exist

        // Send it
        return Uni.createFrom()
                .completionStage(asyncClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(json)
                .build()))
                .onItem().transform(r -> null); // .minimalCompletionStage()
    }
}
