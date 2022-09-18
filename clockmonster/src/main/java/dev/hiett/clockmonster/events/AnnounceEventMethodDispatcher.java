package dev.hiett.clockmonster.events;

import io.smallrye.mutiny.Uni;

public interface AnnounceEventMethodDispatcher {

    default void onCreate() {}

    Uni<Void> dispatch(String json);
}
