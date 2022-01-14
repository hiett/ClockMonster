package dev.hiett.clockmonster.entities.time;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@RegisterForReflection
public class TimeConfiguration {

    @NotNull(message = "You must provide a time type!")
    private TimeType type;

    @Min(message = "First run unix must be positive!", value = 0)
    private long firstRunUnix;

    public TimeConfiguration(TimeType type, long firstRunUnix) {
        this.type = type;
        this.firstRunUnix = firstRunUnix;
    }

    public TimeConfiguration() {}

    public TimeType getType() {
        return type;
    }

    public void setType(TimeType type) {
        this.type = type;
    }

    public long getFirstRunUnix() {
        return firstRunUnix;
    }

    public void setFirstRunUnix(long firstRunUnix) {
        this.firstRunUnix = firstRunUnix;
    }
}
