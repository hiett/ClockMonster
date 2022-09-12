package dev.hiett.clockmonster.services.dispatcher.impls;

import dev.hiett.clockmonster.entities.action.ActionType;
import dev.hiett.clockmonster.entities.action.http.SqsActionPayload;
import dev.hiett.clockmonster.services.dispatcher.Dispatcher;
import io.smallrye.mutiny.Uni;

public class SqsDispatcher implements Dispatcher<SqsActionPayload> {

    @Override
    public Uni<Boolean> dispatchJob(SqsActionPayload actionPayload, Object payload) {
        System.out.println("SQS payload dispatch called.");

        return Uni.createFrom().item(false);
    }

    @Override
    public ActionType getActionType() {
        return ActionType.SQS;
    }
}
