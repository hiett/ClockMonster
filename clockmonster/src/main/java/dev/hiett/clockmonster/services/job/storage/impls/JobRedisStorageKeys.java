package dev.hiett.clockmonster.services.job.storage.impls;

import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class JobRedisStorageKeys {

    @ConfigProperty(name = "clockmonster.redis.key-prefix", defaultValue = "clockmonster")
    String keyPrefix;

    public String getJobIdIncrKey() {
        return keyPrefix + "-id-gen";
    }

    public String getJobKeyPrefix() {
        return keyPrefix + "-job:";
    }

    public String getJobZlistKey() {
        return keyPrefix + "-schedule";
    }
}
