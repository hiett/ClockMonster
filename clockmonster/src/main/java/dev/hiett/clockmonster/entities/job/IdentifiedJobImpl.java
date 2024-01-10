package dev.hiett.clockmonster.entities.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.action.ActionConfiguration;
import dev.hiett.clockmonster.entities.failure.FailureConfiguration;
import dev.hiett.clockmonster.entities.time.TimeConfiguration;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.mutiny.sqlclient.Row;

@RegisterForReflection
public class IdentifiedJobImpl extends UnidentifiedJob implements IdentifiedJob {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private long id;

    public IdentifiedJobImpl(long id, Object payload, TimeConfiguration time, ActionConfiguration action, FailureConfiguration failure) {
        super(payload, time, action, failure);
        this.id = id;
    }

    public IdentifiedJobImpl() {}

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // Static //
    public static IdentifiedJobImpl fromRow(Row row) {
        String payloadString = row.getString("payload");
        Object payload = null;
        if(payloadString != null) {
            try {
                payload = objectMapper.readValue(payloadString, Object.class);
            } catch (JsonProcessingException e) {
                // TODO: Handle this up chain
                e.printStackTrace();
            }
        }

        return new IdentifiedJobImpl(
                row.getLong("id"),
                payload,
                TimeConfiguration.fromRow(row),
                ActionConfiguration.fromRow(row),
                FailureConfiguration.fromRow(row)
        );
    }
}
