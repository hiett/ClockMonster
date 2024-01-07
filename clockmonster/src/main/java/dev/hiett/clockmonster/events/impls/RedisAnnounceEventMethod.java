package dev.hiett.clockmonster.events.impls;

import dev.hiett.clockmonster.events.AnnounceEventMethodDispatcher;
import dev.hiett.clockmonster.services.job.storage.JobStorageProviderService;
import dev.hiett.clockmonster.services.job.storage.JobStorageService;
import dev.hiett.clockmonster.services.job.storage.impls.JobRedisStorageService;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

@Singleton
public class RedisAnnounceEventMethod implements AnnounceEventMethodDispatcher {

    @Inject
    JobStorageProviderService jobStorageProviderService;

    @Inject
    Logger log;

    @Override
    public Uni<Void> dispatch(String json) {
        // Piggyback from the redis connection that's defined for now
        JobStorageService provider = jobStorageProviderService.getCurrentImplementation();
        if(!(provider instanceof JobRedisStorageService)) {
            log.warn("Job announcement is set to Redis, however Redis storage is not being used. Currently, both must be set to Redis to enable this method.");
            return Uni.createFrom().voidItem();
        }

        JobRedisStorageService redisStorageService = (JobRedisStorageService) provider;
        RedisAPI redis = redisStorageService.getRedis();

        return redis.publish("clockmonster-event", json)
                .onItem().transform(r -> null);
    }
}
