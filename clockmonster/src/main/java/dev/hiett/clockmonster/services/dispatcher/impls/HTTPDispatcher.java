package dev.hiett.clockmonster.services.dispatcher.impls;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.action.ActionType;
import dev.hiett.clockmonster.services.dispatcher.Dispatcher;
import io.smallrye.mutiny.Uni;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HTTPDispatcher implements Dispatcher {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public Uni<Boolean> dispatchJob(ActionConfiguration actionConfiguration, Object payload) {
        return Uni.createFrom().item(actionConfiguration).chain(r -> {
            Client client = ClientBuilder.newBuilder().build();
            Response response = client.target(actionConfiguration.getUrl())
                    .request()
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
