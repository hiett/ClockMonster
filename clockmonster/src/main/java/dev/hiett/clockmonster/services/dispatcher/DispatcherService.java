package dev.hiett.clockmonster.services.dispatcher;

import dev.hiett.clockmonster.entities.action.ActionType;
import dev.hiett.clockmonster.entities.job.Job;
import dev.hiett.clockmonster.services.dispatcher.impls.HttpDispatcher;
import dev.hiett.clockmonster.services.dispatcher.impls.SqsDispatcher;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;

import java.util.List;

@SuppressWarnings("rawtypes")
@Singleton
public class DispatcherService {

    private static final List<Dispatcher> dispatchers = List.of(
            new HttpDispatcher(),
            new SqsDispatcher()
    );

    public Uni<Boolean> dispatchJob(Job job) {
        // Find the dispatcher required
        Dispatcher dispatcher = this.findDispatcher(job.getAction().getType());
        if(dispatcher == null)
            return Uni.createFrom().item(false);

        try {
            //noinspection unchecked
            return dispatcher.dispatchJob(job.getAction().getPayload(), job.getPayload());
        } catch (Exception e) {
            return Uni.createFrom().item(false);
        }
    }

    private Dispatcher findDispatcher(ActionType actionType) {
        for(Dispatcher dispatcher : dispatchers)
            if(dispatcher.getActionType() == actionType)
                return dispatcher;

        return null; // Unable to find dispatcher
    }
}
