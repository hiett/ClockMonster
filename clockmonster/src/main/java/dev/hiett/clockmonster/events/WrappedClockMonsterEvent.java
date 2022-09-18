package dev.hiett.clockmonster.events;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.beans.ConstructorProperties;

@RegisterForReflection
public class WrappedClockMonsterEvent {

    public static final String CHANNEL_NAME = "clockmonster.event";

    private final ClockMonsterEvent event;
    private final long jobId;
    private final long createdAtUnix;

    @ConstructorProperties({"event", "jobId", "createdAtUnix"})
    public WrappedClockMonsterEvent(ClockMonsterEvent event, long jobId, long createdAtUnix) {
        this.event = event;
        this.jobId = jobId;
        this.createdAtUnix = createdAtUnix;
    }

    public ClockMonsterEvent getEvent() {
        return event;
    }

    public long getJobId() {
        return jobId;
    }

    public long getCreatedAtUnix() {
        return createdAtUnix;
    }
}
