package dev.hiett.clockmonster.events;

import io.smallrye.mutiny.Uni;

public interface AnnounceEventMethodDispatcher {

    Uni<Void> dispatch(String json);
}
