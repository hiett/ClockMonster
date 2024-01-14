package dev.hiett.clockmonster.services.redis;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.ResponseType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

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

    public void createPubSubSubscriber(List<String> channels, BiConsumer<Response, Response> messageHandler) {
        Redis internalRedis = Redis.createClient(vertx, new RedisOptions().setConnectionString(this.redisUrl));
        RedisConnection subscriptionConnection = internalRedis.connectAndAwait();
        subscriptionConnection.handler(response -> {
            if (response.type() != ResponseType.PUSH)
                return; // Not a message

            Iterator<Response> responseIterator = response.iterator();
            if (!responseIterator.hasNext())
                return; // No message

            Response messageHeader = responseIterator.next();
            if (messageHeader.type() != ResponseType.BULK || !messageHeader.toString().equals("message"))
                return;

            // We have a message! Next item is the channel, then the message
            Response channel = responseIterator.next();
            Response message = responseIterator.next();

            messageHandler.accept(channel, message);
        });
        RedisAPI.api(subscriptionConnection).subscribeAndForget(channels);
    }
}
