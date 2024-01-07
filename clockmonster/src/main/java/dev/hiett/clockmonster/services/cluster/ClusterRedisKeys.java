package dev.hiett.clockmonster.services.cluster;

import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class ClusterRedisKeys {

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
}
