package dev.hiett.clockmonster.services.cluster;

import dev.hiett.clockmonster.services.redis.RedisService;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This service is responsible for managing the cluster of ClockMonster instances.
 * When an instance starts, it adds itself to the cluster table. When an instance gracefully shuts down, it removes
 * itself from the cluster table. If a cluster member is not seen for a certain amount of time, it is removed from the
 * cluster table by other members.
 */
@Singleton
public class ClusterService {

    @Inject
    Logger log;

    @Inject
    RedisService redisService;

    @Inject
    ClusterRedisKeys clusterRedisKeys;

    @ConfigProperty(name = "clockmonster.executor.wait-seconds", defaultValue = "5")
    int waitTimeSeconds;

    private long nodeId;
    private float offset;
    private float lookaheadPeriod;

    void onStart(@Observes StartupEvent event) {
        nodeId = getRedis().incrAndAwait(clusterRedisKeys.getNodeIdGenKey()).toLong();
        log.info("ClusterService started with nodeId=" + nodeId);

        updateNodeHash();
        checkForDeadNodes();
        findOffset();
    }

    @Scheduled(
            every = "10s",
            delay = 10,
            delayUnit = TimeUnit.SECONDS, // We want our onStart to run earlier than the first schedule would, so we offset a bit
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP
    )
    void updateNodeIdLoop() {
        updateNodeHash();
        checkForDeadNodes();
        findOffset();
    }

    private void updateNodeHash() {
        // Every 10 seconds, update the cluster hash table with this node's timestamp
        getRedis().hsetAndAwait(List.of(clusterRedisKeys.getClusterHashKey(), String.valueOf(nodeId), String.valueOf(System.currentTimeMillis())));
    }

    private void checkForDeadNodes() {
        // Only one node should check for dead nodes, so let's create a lock
        if (!getRedis().setnxAndAwait(clusterRedisKeys.getClusterLockKey(), "1").toBoolean()) {
            // We didn't get the lock, so we'll just return
            return;
        }

        // Expire the lock in 10 seconds - we are the ones doing the check
        getRedis().expireAndAwait(List.of(clusterRedisKeys.getClusterLockKey(), "10"));

        // Load in the hash, and check for any nodes that haven't been updated in the last 30 seconds
        Response response = getRedis().hgetallAndAwait(clusterRedisKeys.getClusterHashKey());
        Set<String> nodeIds = response.getKeys();
        long now = System.currentTimeMillis();

        Set<Long> deadNodes = new HashSet<>();
        for (String nodeId : nodeIds) {
            long lastUpdated = Long.parseLong(response.get(nodeId).toString());
            if (now - lastUpdated > 30000)
                deadNodes.add(Long.parseLong(nodeId));
        }

        if (deadNodes.isEmpty()) {
            // No dead nodes, so we can just remove the lock and return
            getRedis().delAndAwait(List.of(clusterRedisKeys.getClusterLockKey()));
            return;
        }

        // Remove the dead nodes from the cluster hash, and then remove the lock
        log.info("Removing dead nodes from cluster: " + deadNodes);
        List<String> clusterHashKeys = new ArrayList<>();
        clusterHashKeys.add(clusterRedisKeys.getClusterHashKey());
        clusterHashKeys.addAll(deadNodes.stream().map(String::valueOf).toList());
        getRedis().hdelAndAwait(clusterHashKeys);

        // Remove lock
        getRedis().delAndAwait(List.of(clusterRedisKeys.getClusterLockKey()));
    }

    private void findOffset() {
        // Get the keys of the cluster hash
        // Divide the wait period by the number of keys
        // find the index of this node in the list (based on the nodeId)
        // multiply the index by the wait period
        // Now you have the offset from the start of the wait period that this node should run at
        List<Response> res = new ArrayList<>();
        getRedis().hkeysAndAwait(clusterRedisKeys.getClusterHashKey()).iterator().forEachRemaining(res::add);
        List<Long> ids = res.stream().map(Response::toLong).sorted(Long::compareTo).toList();

        if (ids.isEmpty())
            return; // Not a valid cluster state

        this.lookaheadPeriod = this.waitTimeSeconds / (float) ids.size();
        int index = ids.indexOf(nodeId);
        this.offset = index * this.lookaheadPeriod;

        log.info("Wait period: " + this.waitTimeSeconds + ", lookahead period: " + this.lookaheadPeriod + ", index: " + index + ", offset: " + this.offset);
    }

    public float getOffset() {
        return offset;
    }

    public float getLookaheadPeriod() {
        return lookaheadPeriod;
    }

    public int getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    private RedisAPI getRedis() {
        return redisService.getRedis();
    }
}
