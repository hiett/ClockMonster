package dev.hiett.clockmonster.services.cluster;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

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

    void onStart(@Observes StartupEvent event) {
        log.info("ClusterService started");
    }
}
