package dev.hiett.clockmonster.events;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.beans.ConstructorProperties;

@RegisterForReflection
public class WrappedClockMonsterEvent {

    public static final String CHANNEL_NAME = "clockmonster.event";

    private final ClockMonsterEvent event;
    private final long jobId;

    @ConstructorProperties({"event", "jobId"})
    public WrappedClockMonsterEvent(ClockMonsterEvent event, long jobId) {
        this.event = event;
        this.jobId = jobId;
    }

    public ClockMonsterEvent getEvent() {
        return event;
    }

    public long getJobId() {
        return jobId;
    }
}
