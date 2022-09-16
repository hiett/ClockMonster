package dev.hiett.clockmonster.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.events.impls.RedisAnnounceEventMethod;
import dev.hiett.clockmonster.events.impls.SqsAnnounceEventMethod;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClockMonsterEventConsumer {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RedisAnnounceEventMethod redisAnnounceEventMethod;

    @Inject
    SqsAnnounceEventMethod sqsAnnounceEventMethod;

    @ConfigProperty(name = "clockmonster.announce-events.enabled")
    boolean announceEventsEnabled;

    @ConfigProperty(name = "clockmonster.announce-events.method")
    AnnounceEventsMethod announceEventsMethod;

    @ConsumeEvent(WrappedClockMonsterEvent.CHANNEL_NAME)
    public Uni<Void> onEvent(WrappedClockMonsterEvent event) {
        if(!announceEventsEnabled)
            return Uni.createFrom().voidItem();

        // Publish this announce event via the specified method
        // First, json stringify it
        String eventJson;
        try {
            eventJson = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Uni.createFrom().voidItem();
        }

        switch(announceEventsMethod) {
            case REDIS:
                return redisAnnounceEventMethod.dispatch(eventJson);
            case SQS:
                return sqsAnnounceEventMethod.dispatch(eventJson);
            default:
                return Uni.createFrom().voidItem();
        }
    }
}
