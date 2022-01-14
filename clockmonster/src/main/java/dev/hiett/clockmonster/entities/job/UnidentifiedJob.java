package dev.hiett.clockmonster.entities.job;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.time.TimeConfiguration;
import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RegisterForReflection
public class UnidentifiedJob implements Job {

    private Object payload;

    @NotNull(message = "Time configuration must be provided!")
    @Valid
    private TimeConfiguration time;

    @NotNull(message = "You must provide an action configuration!")
    @Valid
    private ActionConfiguration action;

    public UnidentifiedJob(Object payload, TimeConfiguration time, ActionConfiguration action) {
        this.payload = payload;
        this.time = time;
        this.action = action;
    }

    public UnidentifiedJob() {}

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public TimeConfiguration getTime() {
        return time;
    }

    public void setTime(TimeConfiguration time) {
        this.time = time;
    }

    public ActionConfiguration getAction() {
        return action;
    }

    public void setAction(ActionConfiguration action) {
        this.action = action;
    }
}
