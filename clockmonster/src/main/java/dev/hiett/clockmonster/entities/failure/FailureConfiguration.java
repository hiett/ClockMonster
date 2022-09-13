package dev.hiett.clockmonster.entities.failure;

import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.JsonArray;
import io.vertx.mutiny.sqlclient.Row;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    // Static //
    public static FailureConfiguration fromRow(Row row) {
        // Parse the json for the backoff array
        // Parse json for the deadLetter action configuration
        // int for iterations count
        JsonArray backoffJsonArray = row.getJsonArray("failure_backoff");

        List<Long> parsedBackoff = new ArrayList<>();
        if(backoffJsonArray != null) {
            parsedBackoff = backoffJsonArray.stream().filter(i -> i instanceof Number)
                    .map(i -> ((Number) i).longValue()).collect(Collectors.toList());
        }

        // parse the Dead Letter configuration (failure_dead_letter_action)
        ActionConfiguration deadLetter = ActionConfiguration.fromRow(row, "failure_dead_letter_action");

        return new FailureConfiguration(parsedBackoff, deadLetter, row.getInteger("failure_iterations_count"));
    }
}
