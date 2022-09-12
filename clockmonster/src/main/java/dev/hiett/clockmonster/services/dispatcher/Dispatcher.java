package dev.hiett.clockmonster.services.dispatcher;

import dev.hiett.clockmonster.entities.action.ActionPayload;
import dev.hiett.clockmonster.entities.action.ActionType;
import io.smallrye.mutiny.Uni;

public interface Dispatcher<T extends ActionPayload> {

    Uni<Boolean> dispatchJob(T actionPayload, Object payload);

    ActionType getActionType();
}
