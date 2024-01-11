package dev.hiett.clockmonster.entities.failure;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.beans.ConstructorProperties;
import java.util.List;

@RegisterForReflection
public class FailureConfiguration {

    private static final List<Long> DEFAULT_EXPONENTIAL_BACKOFF = List.of(1L, 2L, 4L, 16L, 32L, 64L);

    private List<Long> backoff = DEFAULT_EXPONENTIAL_BACKOFF;
    private ActionConfiguration deadLetter;
    private int iterationsCount;

    @ConstructorProperties({"backoff", "deadLetter"})
    public FailureConfiguration(List<Long> backoff, ActionConfiguration deadLetter, int iterationsCount) {
        this.backoff = backoff;
        this.deadLetter = deadLetter;
        this.iterationsCount = iterationsCount;
    }

    public FailureConfiguration() {}

    public List<Long> getBackoff() {
        return backoff;
    }

    public ActionConfiguration getDeadLetter() {
        return deadLetter;
    }

    public int getIterationsCount() {
        return iterationsCount;
    }

    public void setIterationsCount(int iterationsCount) {
        this.iterationsCount = iterationsCount;
    }

    public void incrementIterationsCount() {
        this.iterationsCount++;
    }
}
