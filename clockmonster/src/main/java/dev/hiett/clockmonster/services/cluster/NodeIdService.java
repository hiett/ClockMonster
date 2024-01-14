package dev.hiett.clockmonster.services.cluster;

import dev.hiett.clockmonster.services.redis.RedisService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class NodeIdService {

    @Inject
    RedisService redisService;

    @Inject
    ClusterRedisKeys clusterRedisKeys;

    private long nodeId = -1;

    public long getNodeId() {
        if (nodeId == -1)
            nodeId = redisService.getRedis().incrAndAwait(clusterRedisKeys.getNodeIdGenKey()).toLong();

        return nodeId;
    }
}
