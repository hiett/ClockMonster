package dev.hiett.clockmonster.services.cluster;

import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class ClusterRedisKeys {

    private static final String GLOBAL_CHANNEL_NAME = "global";

    @ConfigProperty(name = "clockmonster.redis.key-prefix", defaultValue = "clockmonster")
    String keyPrefix;

    public String getNodeIdGenKey() {
        return keyPrefix + "-node-id-gen";
    }

    public String getClusterHashKey() {
        return keyPrefix + "-cluster";
    }

    public String getClusterLockKey() {
        return keyPrefix + "-cluster-lock";
    }

    public String getPubSubPrefix() {
        return keyPrefix + ":node:";
    }

    public String getPubSubKey(String subChannel) {
        return getPubSubPrefix() + subChannel;
    }

    public String getPubSubKey(long nodeId) {
        return getPubSubKey(Long.valueOf(nodeId).toString());
    }

    public String getPubSubGlobalKey() {
        return getPubSubKey(GLOBAL_CHANNEL_NAME);
    }
}
