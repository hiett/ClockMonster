package dev.hiett.clockmonster.events.impls;

import dev.hiett.clockmonster.events.AnnounceEventMethodDispatcher;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    @Override
    public Uni<Void> dispatch(String json) {
        // Validate that the SQS dispatcher has been correctly set up
        if(queueUrl == null || accessKeyId == null || secretAccessKey == null || region == null) {
            log.warn("SQS event announce method was selected, but is not configured correctly. Please ensure all SQS " +
                    "environment variables are provided.");
            return Uni.createFrom().voidItem();
        }

        // TODO: Add in the SQS library
        return Uni.createFrom().voidItem();
    }
}
