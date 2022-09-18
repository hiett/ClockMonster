package dev.hiett.clockmonster.events;

public enum ClockMonsterEvent {

    JOB_CREATE,
    JOB_INVOKE_SUCCESSFUL,
    JOB_INVOKE_FAILURE,
    JOB_REMOVE;

    public WrappedClockMonsterEvent build(long jobId) {
        return new WrappedClockMonsterEvent(this, jobId, System.currentTimeMillis() / 1000);
    }
}
