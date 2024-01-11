package dev.hiett.clockmonster.entities.time;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
public class TimeConfiguration {

    @NotNull(message = "You must provide a time type!")
    private TimeType type;

    @Min(message = "Next run unix must be positive!", value = 0)
    private long nextRunUnix;

    private int iterations = 0;
    private int iterationsCount = 0;
    private long interval = 0; // seconds

    public TimeConfiguration(TimeType type, long nextRunUnix, int iterations, long interval, int iterationsCount) {
        this.type = type;
        this.nextRunUnix = nextRunUnix;

        if(type == TimeType.REPEATING) {
            this.iterations = iterations;
            this.interval = interval;
            this.iterationsCount = iterationsCount;
        }
    }

    public TimeConfiguration() {}

    public TimeType getType() {
        return type;
    }

    public void setType(TimeType type) {
        this.type = type;
    }

    public int getIterationsCount() {
        return iterationsCount;
    }

    public void setIterationsCount(int iterationsCount) {
        this.iterationsCount = iterationsCount;
    }

    public long getNextRunUnix() {
        return nextRunUnix;
    }

    public void setNextRunUnix(long nextRunUnix) {
        this.nextRunUnix = nextRunUnix;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
}
