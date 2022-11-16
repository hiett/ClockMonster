package dev.hiett.clockmonster.entities.job;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.failure.FailureConfiguration;
import dev.hiett.clockmonster.entities.time.TimeConfiguration;

public class TemporaryFailureJob implements Job {

    private final Object payload;
    private final ActionConfiguration actionConfiguration;

    public TemporaryFailureJob(Object payload, ActionConfiguration actionConfiguration) {
        this.payload = payload;
        this.actionConfiguration = actionConfiguration;
    }

    @Override
    public Object getPayload() {
        return payload;
    }

    @Override
    public TimeConfiguration getTime() {
        return null;
    }

    @Override
    public ActionConfiguration getAction() {
        return actionConfiguration;
    }

    @Override
    public FailureConfiguration getFailure() {
        return null;
    }
}
