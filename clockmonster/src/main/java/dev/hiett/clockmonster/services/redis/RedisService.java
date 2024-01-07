package dev.hiett.clockmonster.services.redis;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.redis.client.RedisOptions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class RedisService {

    @ConfigProperty(name = "quarkus.redis.hosts", defaultValue = "")
    String redisUrl;

    @Inject
    Vertx vertx;

    private RedisAPI redis;

    public RedisAPI getRedis() {
        if (redis == null)
            createRedis();

        return redis;
    }

    private void createRedis() {
        Redis internalRedis = Redis.createClient(vertx, new RedisOptions().setConnectionString(this.redisUrl));
        RedisConnection internalRedisConnection = internalRedis.connectAndAwait();
        this.redis = RedisAPI.api(internalRedisConnection);
    }
}
