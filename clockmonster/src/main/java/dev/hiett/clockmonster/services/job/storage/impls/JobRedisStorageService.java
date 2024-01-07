package dev.hiett.clockmonster.services.job.storage.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import dev.hiett.clockmonster.services.job.storage.JobStorageService;
import dev.hiett.clockmonster.services.redis.RedisService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.StreamSupport;

@Singleton
public class JobRedisStorageService implements JobStorageService {

    @Inject
    JobRedisStorageKeys keys;

    //language=Lua
    private static final String LUA_POP_JOBS_SCRIPT =
            "local key, now = KEYS[1], ARGV[1]\n" +
                    "local jobs = redis.call(\"zrangebyscore\", key, \"-inf\", now)\n" +
                    "\n" +
                    "if jobs[1] then \n" +
                    "    return redis.call(\"mget\", unpack(jobs))\n" +
                    "else\n" +
                    "    return {}\n" +
                    "end";

    //language=Lua
    private static final String LUA_BULK_REMOVE_JOBS_SCRIPT =
            "local schkey = ARGV[1] \n" +
                    "redis.call(\"del\", unpack(KEYS))\n" +
                    "redis.call(\"zrem\", schkey, unpack(KEYS))";

    //language=Lua
    private static final String LUA_CREATE_JOB_SCRIPT =
            "local schkey, key, payload, extime = KEYS[1], KEYS[2], ARGV[1], ARGV[2]\n" +
                    "redis.call(\"set\", key, payload)\n" +
                    "redis.call(\"zadd\", schkey, extime, key)\n";

    //language=Lua
    private static final String LUA_UPDATE_JOB_SCRIPT =
            "local schkey, key, payload, extime = KEYS[1], KEYS[2], ARGV[1], ARGV[2]\n" +
                    "redis.call(\"set\", key, payload)\n" +
                    "redis.call(\"zrem\", schkey, key)\n" +
                    "redis.call(\"zadd\", schkey, extime, key)";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Logger log;

    @Inject
    RedisService redisService;

    private String popJobsLuaSha, removeJobsLuaSha, createJobLuaSha, updateJobLuaSha;

    /**
     * If REDIS is the selected storage method, then we need to create the redis connection and
     * run the SCRIPT LOAD command to add in our pop jobs lua script.
     */
    @Override
    public void createConnection() {
        this.popJobsLuaSha = this.loadScript(LUA_POP_JOBS_SCRIPT);
        this.removeJobsLuaSha = this.loadScript(LUA_BULK_REMOVE_JOBS_SCRIPT);
        this.createJobLuaSha = this.loadScript(LUA_CREATE_JOB_SCRIPT);
        this.updateJobLuaSha = this.loadScript(LUA_UPDATE_JOB_SCRIPT);

        log.info("Redis SHAs: pop=" + this.popJobsLuaSha + ", remove=" + this.removeJobsLuaSha + ", create=" + this.createJobLuaSha);
    }

    @Override
    public Uni<IdentifiedJob> createJob(UnidentifiedJob unidentifiedJob) {
        return this.createJobId()
                .chain(jobId -> {
                    IdentifiedJob identifiedJob = new IdentifiedJob(jobId, unidentifiedJob.getPayload(),
                            unidentifiedJob.getTime(), unidentifiedJob.getAction(), unidentifiedJob.getFailure());

                    // Stringify this
                    String jobJson = this.jsonifyJob(identifiedJob);
                    if (jobJson == null)
                        return Uni.createFrom().item(null);

                    return this.getRedis().evalsha(List.of(this.createJobLuaSha, "2", keys.getJobZlistKey(), this.createJobKey(jobId),
                            jobJson, Long.valueOf(identifiedJob.getTime().getNextRunUnix()).toString()))
                            .onItem().transform(r -> identifiedJob);
                });
    }

    @Override
    public Uni<IdentifiedJob> getJob(long id) {
        return this.getRedis().get(this.createJobKey(id)).onItem().ifNotNull()
                .transform(r -> this.fromJson(r.toString()));
    }

    @Override
    public Multi<IdentifiedJob> findJobs(float lookaheadPeriodSeconds) {
        return this.getRedis().evalsha(List.of(this.popJobsLuaSha, "1", keys.getJobZlistKey(),
                        Long.valueOf(System.currentTimeMillis() / 1000).toString()))
                .onItem().transformToMulti(r -> {
                    if (r.type() != ResponseType.MULTI)
                        return Multi.createFrom().empty(); // No jobs

//                    log.info(r.type());
//                    log.info(r);
                    Iterator<Response> iterator = r.iterator();

                    return Multi.createFrom().items(
                            StreamSupport.stream(
                                            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                                    .filter(Objects::nonNull)
                                    .map(response -> this.fromJson(response.toString()))
                                    .filter(Objects::nonNull)
                    );
                });
    }

    @Override
    public Uni<Void> deleteJob(long id) {
        return this.batchDeleteJobs(id);
    }

    @Override
    public Uni<Void> batchDeleteJobs(Long... ids) {
        List<String> idList = Arrays.stream(ids).map(this::createJobKey).toList();

        List<String> args = new ArrayList<>();
        args.add(this.removeJobsLuaSha);

        // Key Size + keys
        args.add(idList.size() + "");
        args.addAll(idList);

        // args
        args.add(keys.getJobZlistKey());

        return this.getRedis().evalsha(args).onItem().transform(r -> null);
    }

    @Override
    public Uni<Void> updateJobTime(long id, LocalDateTime jobTime, boolean addIteration) {
        // There is some JSON parsing that has to go on here
        return this.getJob(id)
                .onItem().ifNull().fail()
                .onItem().transform(res -> {
                    res.getTime().setNextRunUnix(jobTime.atZone(ZoneId.of("UTC")).toEpochSecond());
                    if(addIteration)
                        res.getTime().setIterationsCount(res.getTime().getIterationsCount() + 1);
                    return res;
                })
                .onItem().ifNull().fail() // Update the sorted set & entry
                .chain(this::updateJob)
                .onFailure().recoverWithNull().replaceWithVoid();
    }

    @Override
    public Uni<Void> updateJob(IdentifiedJob job) {
        // Encode the job into json
        String jobJson = this.jsonifyJob(job);
        if (jobJson == null)
            return Uni.createFrom().failure(new IOException());

        // We also need to update the sorted set value
        return this.getRedis().evalsha(List.of(updateJobLuaSha, "2", keys.getJobZlistKey(), this.createJobKey(job.getId()),
                        jobJson, Long.valueOf(job.getTime().getNextRunUnix()).toString()))
                .onItem().transform(r -> null);
    }

    private String loadScript(String script) {
        return this.getRedis().scriptAndAwait(List.of("LOAD", script)).toString();
    }

    /**
     * Creates an incremental job ID in redis using incr
     */
    private Uni<Long> createJobId() {
        return this.getRedis().incr(keys.getJobIdIncrKey())
                .onItem().transform(Response::toLong);
    }

    private String createJobKey(long id) {
        return keys.getJobKeyPrefix() + id;
    }

    private String jsonifyJob(IdentifiedJob identifiedJob) {
        try {
            return objectMapper.writeValueAsString(identifiedJob);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private RedisAPI getRedis() {
        return this.redisService.getRedis();
    }

    public IdentifiedJob fromJson(String jobJson) {
        try {
            return objectMapper.readValue(jobJson, IdentifiedJob.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
