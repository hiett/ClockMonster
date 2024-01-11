package dev.hiett.clockmonster.entities.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.failure.FailureConfiguration;
import dev.hiett.clockmonster.entities.time.TimeConfiguration;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class IdentifiedJobImpl extends UnidentifiedJob implements IdentifiedJob {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private long id;

    public IdentifiedJobImpl(long id, Object payload, TimeConfiguration time, ActionConfiguration action, FailureConfiguration failure) {
        super(payload, time, action, failure);
        this.id = id;
    }

    public IdentifiedJobImpl() {}

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
