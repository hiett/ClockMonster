package dev.hiett.clockmonster.events;

import io.quarkus.vertx.ConsumeEvent;

import javax.inject.Singleton;

@Singleton
public class ClockMonsterEventConsumer {

    @ConsumeEvent(WrappedClockMonsterEvent.CHANNEL_NAME)
    public void onEvent(WrappedClockMonsterEvent event) {
        
    }
}
