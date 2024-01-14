package dev.hiett.clockmonster.services.job.storage.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.job.IdentifiedJobImpl;
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
import java.util.*;
import java.util.stream.StreamSupport;

@Singleton
public class JobRedisStorageService implements JobStorageService {

    //language=Lua
    private static final String LUA_POP_JOBS_SCRIPT = """
            local key, now, nodeId, lockTimeoutSeconds = KEYS[1], ARGV[1], ARGV[2], ARGV[3]
            local jobs = redis.call("zrange", key, "-inf", now, "BYSCORE")
            
            if jobs[1] then -- if there are jobs to process
                -- iterate over each job and try to apply a lock
                -- if a lock already exists, then we skip it, and don't include it in the mget
                -- we iterate backwards so that we can remove elements from the list without affecting the loop
                for i = #jobs, 1, -1 do
                    local lock = redis.call("set", jobs[i] .. ":lock", nodeId, "NX", "EX", lockTimeoutSeconds) -- 10 seconds
                    if not lock then -- if we can't get a lock, then remove it from the list
                        table.remove(jobs, i)
                    end
                end
               \s
                if jobs[1] then -- if there are any jobs after lock checking
                    return redis.call("mget", unpack(jobs))
                else
                    return
                end
            else
                return
            end
            """;

    //language=Lua
    private static final String LUA_BULK_REMOVE_JOBS_SCRIPT = """
            local schkey = ARGV[1]
            
            local deleteKeys = {}
            -- for each of the keys, include the lock variant
            for i, key in ipairs(KEYS) do
                table.insert(deleteKeys, key)
                table.insert(deleteKeys, key .. ":lock")
            end
            
            redis.call("del", unpack(deleteKeys))
            redis.call("zrem", schkey, unpack(KEYS)) -- locks don't exist in the sorted set
            """;

    //language=Lua
    private static final String LUA_CREATE_JOB_SCRIPT =
            "local schkey, key, payload, extime = KEYS[1], KEYS[2], ARGV[1], ARGV[2]\n" +
                    "redis.call(\"set\", key, payload)\n" +
                    "redis.call(\"zadd\", schkey, extime, key)\n";

    //language=Lua
    private static final String LUA_UPDATE_JOB_SCRIPT =
            "local schkey, key, payload, extime = KEYS[1], KEYS[2], ARGV[1], ARGV[2]\n" +
                    "redis.call(\"set\", key, payload)\n" +
                    "redis.call(\"zadd\", schkey, extime, key)";

    @Inject
    JobRedisStorageKeys keys;

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
    public Uni<IdentifiedJobImpl> createJob(UnidentifiedJob unidentifiedJob) {
        return this.createJobId()
                .chain(jobId -> {
                    IdentifiedJobImpl identifiedJob = new IdentifiedJobImpl(jobId, unidentifiedJob.getPayload(),
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
    public Uni<IdentifiedJobImpl> getJob(long id) {
        return this.getRedis().get(this.createJobKey(id)).onItem().ifNotNull()
                .transform(r -> this.fromJson(r.toString()));
    }

    @Override
    public Multi<IdentifiedJobImpl> findJobs(float lookaheadPeriodSeconds, int lockTimeoutSeconds, long nodeId) {
        int flooredLookaheadPeriod = (int) Math.floor(lookaheadPeriodSeconds);
        long periodLong = (System.currentTimeMillis() / 1000) + flooredLookaheadPeriod;

        return this.getRedis().evalsha(List.of(this.popJobsLuaSha, "1", keys.getJobZlistKey(),
                        Long.valueOf(periodLong).toString(), Long.valueOf(nodeId).toString(), Integer.valueOf(lockTimeoutSeconds).toString()))
                .onItem().transformToMulti(r -> {
                    if (r.type() != ResponseType.MULTI)
                        return Multi.createFrom().empty();

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

    // TODO: remove, only here for legacy reasons
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
    public Uni<Void> updateJob(IdentifiedJobImpl job) {
        // Encode the job into json
        String jobJson = this.jsonifyJob(job);
        if (jobJson == null)
            return Uni.createFrom().failure(new IOException());

        // We also need to update the sorted set value
        return this.getRedis().evalsha(List.of(updateJobLuaSha, "2", keys.getJobZlistKey(), this.createJobKey(job.getId()),
                        jobJson, Long.valueOf(job.getTime().getNextRunUnix()).toString()))
                .onItem().transform(r -> null);
    }

    @Override
    public Uni<Void> extendJobLock(long id, int lockTimeoutSeconds) {
        return this.getRedis().expire(List.of(this.createJobKey(id) + ":lock", Integer.valueOf(lockTimeoutSeconds).toString()))
                .onItem().transform(r -> null);
    }

    @Override
    public Uni<Long> queryJobLock(long id) {
        return this.getRedis().get(this.createJobKey(id) + ":lock")
                .onItem().ifNotNull().transform(Response::toLong);
    }

    @Override
    public Uni<Void> deleteJobLock(long id) {
        return this.getRedis().del(List.of(this.createJobKey(id) + ":lock"))
                .onItem().transform(r -> null);
    }

    private String loadScript(String script) {
        // Preprocess the script: remove all lines that are just comments
        // we need to ignore any indentation when doing so (trim)
        script = Arrays.stream(script.split("\n"))
                .filter(s -> !s.trim().startsWith("--"))
                .reduce((s1, s2) -> s1 + "\n" + s2)
                .orElseThrow(); // This should never happen

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

    private String jsonifyJob(IdentifiedJobImpl identifiedJob) {
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

    public IdentifiedJobImpl fromJson(String jobJson) {
        try {
            return objectMapper.readValue(jobJson, IdentifiedJobImpl.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
