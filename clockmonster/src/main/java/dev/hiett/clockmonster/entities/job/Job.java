package dev.hiett.clockmonster.entities.job;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.time.TimeConfiguration;

public interface Job {

    Object getPayload();

    TimeConfiguration getTime();

    ActionConfiguration getAction();
}
