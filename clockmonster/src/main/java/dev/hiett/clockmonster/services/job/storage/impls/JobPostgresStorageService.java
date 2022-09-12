package dev.hiett.clockmonster.services.job.storage.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import dev.hiett.clockmonster.services.job.storage.JobStorageService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.PreparedQuery;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import org.flywaydb.core.Flyway;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Migrations are handled via Flyway. To add a new migration, go into the src/main/resources/db/migration folder.
 */
@ApplicationScoped
public class JobPostgresStorageService implements JobStorageService {

    private static final String JOB_TABLE = "clockmonster_jobs";

    @Inject
    PgPool pgPool;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Flyway flyway;

    /**
     * If POSTGRES is the selected storage method, then we need to run the migrations
     * to setup the database
     */
    @Override
    public void createConnection() {
        flyway.migrate();
    }

    @Override
    public Uni<IdentifiedJob> createJob(UnidentifiedJob unidentifiedJob) {
        PreparedQuery<RowSet<Row>> query = pgPool.preparedQuery("INSERT INTO " + JOB_TABLE + "(payload, action, time_type, time_next_run, time_repeating_iterations, time_repeating_interval, failure_dead_letter_action, failure_backoff)\n" +
                "VALUES ($1, $2, $3, $4, $5, $6, $7, $8) \n" +
                "RETURNING *");

        String payloadJsonString = null;
        String actionJsonString = null;
        String failureDeadLetterActionJsonString = null;
        if(unidentifiedJob.getPayload() != null) {
            try {
                payloadJsonString = objectMapper.writeValueAsString(unidentifiedJob.getPayload());
                actionJsonString = objectMapper.writeValueAsString(unidentifiedJob.getAction());

                if(unidentifiedJob.getFailure().getDeadLetter() != null) {
                    failureDeadLetterActionJsonString = objectMapper.writeValueAsString(unidentifiedJob.getFailure().getDeadLetter());
                }
            } catch (JsonProcessingException e) {
                // TODO: Raise up the chain
                e.printStackTrace();
            }
        }

        Tuple tuple = Tuple.of(payloadJsonString)
                .addString(actionJsonString)
                .addString(unidentifiedJob.getTime().getType().toString())
                .addLocalDateTime(LocalDateTime.ofEpochSecond(unidentifiedJob.getTime().getNextRunUnix(), 0, ZoneOffset.UTC))
                .addInteger(unidentifiedJob.getTime().getIterations())
                .addLong(unidentifiedJob.getTime().getInterval())
                .addString(failureDeadLetterActionJsonString)
                .addJsonArray(new JsonArray(unidentifiedJob.getFailure().getBackoff()));

        return query.execute(tuple).onItem().transform(rows -> {
            for(Row row : rows)
                return IdentifiedJob.fromRow(row);

            return null;
        });
    }

    @Override
    public Uni<IdentifiedJob> getJob(long id) {
        return pgPool.preparedQuery("SELECT * FROM " + JOB_TABLE + " WHERE id = $1")
                .execute(Tuple.of(id))
                .onItem().transform(rows -> {
                    for(Row row : rows)
                        return IdentifiedJob.fromRow(row);

                    return null;
                });
    }

    @Override
    public Multi<IdentifiedJob> findJobs() {
        return pgPool.preparedQuery("SELECT * FROM " + JOB_TABLE + " WHERE time_next_run < NOW()")
                .execute().onItem().transformToMulti(rows -> {
                    List<IdentifiedJob> jobs = new ArrayList<>();

                    for(Row row : rows)
                        jobs.add(IdentifiedJob.fromRow(row));

                    return Multi.createFrom().items(jobs.stream());
                });
    }

    @Override
    public Uni<Void> deleteJob(long id) {
        return pgPool.preparedQuery("DELETE FROM " + JOB_TABLE + " WHERE id = $1")
                .execute(Tuple.of(id))
                .onItem().transform(r -> null);
    }

    @Override
    public Uni<Void> batchDeleteJobs(Long... ids) {
        Tuple tuple = Tuple.tuple().addArrayOfLong(ids);

        return pgPool.preparedQuery("DELETE FROM " + JOB_TABLE + " WHERE id = ANY($1)")
                .execute(tuple)
                .onItem().transform(r -> null);
    }

    @Override
    public Uni<Void> updateJobTime(long id, LocalDateTime jobTime, boolean addIteration) {
        String countIterationSql = "";
        if(addIteration)
            countIterationSql = ", time_repeating_iterations_count = time_repeating_iterations_count + 1 ";

        return pgPool.preparedQuery("UPDATE " + JOB_TABLE + " SET time_next_run = $1" + countIterationSql + " WHERE id = $2")
                .execute(Tuple.of(jobTime, id))
                .onItem().transform(r -> null);
    }

    @Override
    public Uni<Void> updateJob(IdentifiedJob job) {
        return null;
    }
}
