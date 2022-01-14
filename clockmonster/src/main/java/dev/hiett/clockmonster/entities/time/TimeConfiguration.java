package dev.hiett.clockmonster.entities.time;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.mutiny.sqlclient.Row;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.ZoneId;

@RegisterForReflection
public class TimeConfiguration {

    @NotNull(message = "You must provide a time type!")
    private TimeType type;

    @Min(message = "First run unix must be positive!", value = 0)
    private long firstRunUnix;

    private int iterations = 0;
    private long interval = 0; // seconds

    public TimeConfiguration(TimeType type, long firstRunUnix, int iterations, long interval) {
        this.type = type;
        this.firstRunUnix = firstRunUnix;

        if(type == TimeType.REPEATING) {
            this.iterations = iterations;
            this.interval = interval;
        }
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

    // Static //
    public static TimeConfiguration fromRow(Row row) {
        return new TimeConfiguration(
                TimeType.valueOf(row.getString("time_type")),
                row.getLocalDateTime("time_first_run").atZone(ZoneId.of("UTC")).toEpochSecond(),
                row.getInteger("time_repeating_iterations"),
                row.getLong("time_repeating_interval")
        );
    }
}
