package dev.hiett.clockmonster.services.job.storage;

import dev.hiett.clockmonster.services.job.storage.impls.JobPostgresStorageService;
import dev.hiett.clockmonster.services.job.storage.impls.JobRedisStorageService;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Arrays;

@ApplicationScoped
public class JobStorageProviderService {

    @ConfigProperty(name = "clockmonster.storage.method", defaultValue = "POSTGRES")
    JobStorageMethod method;

    @Inject
    JobPostgresStorageService postgresImpl;

    @Inject
    JobRedisStorageService redisImpl;

    @Inject
    Logger log;

    private boolean ready;

    void onStart(@Observes StartupEvent event) {
        if(method == null) {
            // The method is null, this will cause errors when trying to save!
            log.error("Unknown job storage method! This will prevent any jobs from being saved or read, so must be fixed. " +
                    "Valid methods are: " + Arrays.toString(JobStorageMethod.values()));
        } else {
            this.getCurrentImplementation().createConnection();
            ready = true;
        }
    }

    public boolean isReady() {
        return ready;
    }

    public JobStorageService getCurrentImplementation() {
        switch (method) {
            case REDIS: return redisImpl;
            case POSTGRES: return postgresImpl;
            default: return null; // TODO: Eventually, let's properly error handle this
        }
    }
}
