package dev.hiett.clockmonster.services.dispatcher;

import dev.hiett.clockmonster.entities.action.ActionType;
import dev.hiett.clockmonster.entities.job.Job;
import dev.hiett.clockmonster.services.dispatcher.impls.HTTPDispatcher;
import io.smallrye.mutiny.Uni;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public class DispatcherService {

    private static final List<Dispatcher> dispatchers = List.of(
            new HTTPDispatcher()
    );

    public Uni<Boolean> dispatchJob(Job job) {
        // Find the dispatcher required
        Dispatcher dispatcher = this.findDispatcher(job.getAction().getType());
        if(dispatcher == null)
            return Uni.createFrom().item(false);

        return dispatcher.dispatchJob(job.getAction(), job.getPayload());
    }

    private Dispatcher findDispatcher(ActionType actionType) {
        for(Dispatcher dispatcher : dispatchers)
            if(dispatcher.getActionType() == actionType)
                return dispatcher;

        return null; // Unable to find dispatcher
    }
}
