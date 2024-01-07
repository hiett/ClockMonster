package dev.hiett.clockmonster.events;

import io.vertx.core.eventbus.EventBus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ClockMonsterEventDispatcherService {

    @Inject
    EventBus eventBus;

    public void dispatch(WrappedClockMonsterEvent event) {
        eventBus.publish(WrappedClockMonsterEvent.CHANNEL_NAME, event);
    }
}
