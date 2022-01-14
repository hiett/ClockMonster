package dev.hiett.clockmonster.payloads;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.time.TimeConfiguration;
import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RegisterForReflection
public class CreateJobPayload {

    private Object payload;

    @NotNull(message = "Time configuration must be provided!")
    @Valid
    private TimeConfiguration time;

    @NotNull(message = "You must provide an action configuration!")
    @Valid
    private ActionConfiguration action;

    public CreateJobPayload(Object payload, TimeConfiguration time, ActionConfiguration action) {
        this.payload = payload;
        this.time = time;
        this.action = action;
    }

    public CreateJobPayload() {}

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
