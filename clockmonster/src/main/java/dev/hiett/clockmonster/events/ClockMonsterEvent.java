package dev.hiett.clockmonster.events;

public enum ClockMonsterEvent {

    JOB_CREATE,
    JOB_INVOKE_SUCCESSFUL,
    JOB_INVOKE_FAILURE,
    JOB_REMOVED;

    public WrappedClockMonsterEvent build(long jobId) {
        return new WrappedClockMonsterEvent(this, jobId);
    }
}
