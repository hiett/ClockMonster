package dev.hiett.clockmonster.services.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.PreparedQuery;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@ApplicationScoped
public class JobDatabaseService {

    private static final String JOB_TABLE = "clockmonster_jobs";

    @Inject
    PgPool pgPool;

    @Inject
    ObjectMapper objectMapper;

    public Uni<IdentifiedJob> createJob(UnidentifiedJob unidentifiedJob) {
        PreparedQuery<RowSet<Row>> query = pgPool.preparedQuery("INSERT INTO " + JOB_TABLE + "(payload, action_type, action_url, time_type, time_first_run, time_repeating_iterations, time_repeating_interval)\n" +
                "VALUES ($1, $2, $3, $4, $5, $6, $7) \n" +
                "RETURNING *");

        String payloadJsonString = null;
        if(unidentifiedJob.getPayload() != null) {
            try {
                payloadJsonString = objectMapper.writeValueAsString(unidentifiedJob.getPayload());
            } catch (JsonProcessingException e) {
                // TODO: Raise up the chain
                e.printStackTrace();
            }
        }

        Tuple tuple = Tuple.of(payloadJsonString)
                .addString(unidentifiedJob.getAction().getType().toString())
                .addString(unidentifiedJob.getAction().getUrl())
                .addString(unidentifiedJob.getTime().getType().toString())
                .addLocalDateTime(LocalDateTime.ofEpochSecond(unidentifiedJob.getTime().getFirstRunUnix(), 0, ZoneOffset.UTC))
                .addInteger(unidentifiedJob.getTime().getIterations())
                .addLong(unidentifiedJob.getTime().getInterval());

        return query.execute(tuple).onItem().transform(rows -> {
            for(Row row : rows)
                return IdentifiedJob.fromRow(row);

            return null;
        });
    }

    public Uni<IdentifiedJob> getJob(long id) {
        return pgPool.preparedQuery("SELECT * FROM " + JOB_TABLE + " WHERE id = $1")
                .execute(Tuple.of(id))
                .onItem().transform(rows -> {
                    for(Row row : rows)
                        return IdentifiedJob.fromRow(row);

                    return null;
                });
    }
}
