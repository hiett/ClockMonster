package dev.hiett.clockmonster.services.dispatcher;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.action.ActionType;
import io.smallrye.mutiny.Uni;

public interface Dispatcher {

    Uni<Boolean> dispatchJob(ActionConfiguration actionConfiguration, Object payload);

    ActionType getActionType();
}
