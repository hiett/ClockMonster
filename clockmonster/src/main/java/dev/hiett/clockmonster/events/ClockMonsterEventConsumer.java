package dev.hiett.clockmonster.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.events.impls.RedisAnnounceEventMethod;
import dev.hiett.clockmonster.events.impls.SqsAnnounceEventMethod;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.event.Observes;
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

    @Inject
    Logger log;

    @ConfigProperty(name = "clockmonster.announce-events.enabled")
    boolean announceEventsEnabled;

    @ConfigProperty(name = "clockmonster.announce-events.method")
    AnnounceEventsMethod announceEventsMethod;

    // On start, create the client for the required config
    void onStart(@Observes StartupEvent event) {
        log.info("Attempting to create announcement dispatcher. Enabled? " + announceEventsEnabled);

        AnnounceEventMethodDispatcher dispatcher = this.getDispatcher();
        if(dispatcher == null)
            return;

        dispatcher.onCreate();
    }

    @ConsumeEvent(WrappedClockMonsterEvent.CHANNEL_NAME)
    public Uni<Void> onEvent(WrappedClockMonsterEvent event) {
        AnnounceEventMethodDispatcher dispatcher = this.getDispatcher();
        if(dispatcher == null)
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

        log.info("Dispatching " + eventJson + " to method " + announceEventsMethod);

        return dispatcher.dispatch(eventJson);
    }

    private AnnounceEventMethodDispatcher getDispatcher() {
        if(!announceEventsEnabled)
            return null;

        switch(announceEventsMethod) {
            case REDIS:
                return redisAnnounceEventMethod;
            case SQS:
                return sqsAnnounceEventMethod;
            default:
                return null;
        }
    }
}
