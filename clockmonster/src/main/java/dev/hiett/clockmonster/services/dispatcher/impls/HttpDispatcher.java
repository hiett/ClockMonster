package dev.hiett.clockmonster.services.dispatcher.impls;

import dev.hiett.clockmonster.entities.action.ActionType;
import dev.hiett.clockmonster.entities.action.http.HttpActionPayload;
import dev.hiett.clockmonster.services.dispatcher.Dispatcher;
import io.smallrye.mutiny.Uni;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpDispatcher implements Dispatcher<HttpActionPayload> {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public Uni<Boolean> dispatchJob(HttpActionPayload httpActionPayload, Object payload) {
        return Uni.createFrom().item(httpActionPayload).chain(r -> {
            Client client = ClientBuilder.newBuilder().build();
            Response response = client.target(httpActionPayload.getUrl())
                    .request()
                    .header("x-dispatched-from", "ClockMonster")
                    .buildPost(Entity.json(payload))
                    .invoke();

            return Uni.createFrom().item(response.getStatus() < 300);
        }).runSubscriptionOn(executorService);
    }

    @Override
    public ActionType getActionType() {
        return ActionType.HTTP;
    }
}
