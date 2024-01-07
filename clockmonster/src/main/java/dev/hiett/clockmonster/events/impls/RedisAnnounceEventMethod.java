package dev.hiett.clockmonster.events.impls;

import dev.hiett.clockmonster.events.AnnounceEventMethodDispatcher;
import dev.hiett.clockmonster.services.redis.RedisService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class RedisAnnounceEventMethod implements AnnounceEventMethodDispatcher {

    @Inject
    RedisService redisService;

    @Override
    public Uni<Void> dispatch(String json) {
        return redisService.getRedis().publish("clockmonster-event", json)
                .onItem().transform(r -> null);
    }
}
