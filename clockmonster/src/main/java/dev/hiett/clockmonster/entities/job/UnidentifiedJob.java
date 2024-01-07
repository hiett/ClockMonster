package dev.hiett.clockmonster.entities.job;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.failure.FailureConfiguration;
import dev.hiett.clockmonster.entities.time.TimeConfiguration;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
public class UnidentifiedJob implements Job {

    private Object payload;

    @NotNull(message = "Time configuration must be provided!")
    @Valid
    private TimeConfiguration time;

    @NotNull(message = "You must provide an action configuration!")
    @Valid
    private ActionConfiguration action;

    @Valid
    private FailureConfiguration failure = new FailureConfiguration();

    public UnidentifiedJob(Object payload, TimeConfiguration time, ActionConfiguration action, FailureConfiguration failure) {
        this.payload = payload;
        this.time = time;
        this.action = action;
        this.failure = failure;
    }

    public UnidentifiedJob() {}

    @Override
    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public TimeConfiguration getTime() {
        return time;
    }

    public void setTime(TimeConfiguration time) {
        this.time = time;
    }

    @Override
    public ActionConfiguration getAction() {
        return action;
    }

    public void setAction(ActionConfiguration action) {
        this.action = action;
    }

    @Override
    public FailureConfiguration getFailure() {
        return failure;
    }

    public void setFailure(FailureConfiguration failure) {
        this.failure = failure;
    }
}
