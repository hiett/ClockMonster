package dev.hiett.clockmonster.services.job.storage.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import dev.hiett.clockmonster.services.job.storage.JobStorageService;
import io.quarkus.redis.client.runtime.MutinyRedisAPI;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.RedisOptions;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * zadd, zrangebyscore and zrem are used here to make ClockMonster work in redis as efficiently as possible!
 * Really like the score based commands. They're fun.
 *
 * Rather than all values being stored in the score set, they're stored in separate keys that are then linked to
 * via ids in the score set.
 *
 * This is because the system allows you to get values by ID, and I didn't want to scan the set. To minimise friction
 * everything that requires the multiple redis commands is done in Lua.
 */
@ApplicationScoped
public class JobRedisStorageService implements JobStorageService {

    private static final String JOB_ID_INCR_KEY = "clockmonster-id-gen";
    private static final String JOB_KEY_PREFIX = "clockmonster-job:";
    private static final String JOB_ZLIST_KEY = "clockmonster-schedule";

    //language=Lua
    private static final String LUA_POP_JOBS_SCRIPT =
            "local key, now = KEYS[1], ARGV[1]\n" +
            "local jobs = redis.call(\"zrangebyscore\", key, \"-inf\", now)\n" +
            "return redis.call(\"mget\", unpack(jobs))";

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

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "quarkus.redis.hosts")
    String redisUrl;

    private RedisAPI redis;
    private String popJobsLuaSha, removeJobsLuaSha, createJobLuaSha;

    /**
     * If REDIS is the selected storage method, then we need to create the redis connection and
     * run the SCRIPT LOAD command to add in our pop jobs lua script.
     */
    @Override
    public void createConnection() {
        Redis internalRedis = Redis.createClient(vertx, new RedisOptions().setConnectionString(this.redisUrl));
        RedisConnection internalRedisConnection = internalRedis.connectAndAwait();
        this.redis = MutinyRedisAPI.api(internalRedisConnection);
        this.popJobsLuaSha = this.loadScript(LUA_POP_JOBS_SCRIPT);
        this.removeJobsLuaSha = this.loadScript(LUA_BULK_REMOVE_JOBS_SCRIPT);
        this.createJobLuaSha = this.loadScript(LUA_CREATE_JOB_SCRIPT);
    }

    @Override
    public Uni<IdentifiedJob> createJob(UnidentifiedJob unidentifiedJob) {
        return this.createJobId()
                .chain(jobId -> {
                    IdentifiedJob identifiedJob = new IdentifiedJob(jobId, unidentifiedJob.getPayload(),
                            unidentifiedJob.getTime(), unidentifiedJob.getAction());

                    // Stringify this
                    String jobJson = this.jsonifyJob(identifiedJob);
                    if (jobJson == null)
                        return Uni.createFrom().item(null);

                    return this.redis.evalsha(List.of(this.createJobLuaSha, "2", JOB_ZLIST_KEY, this.createJobKey(jobId),
                            jobJson, Long.valueOf(identifiedJob.getTime().getFirstRunUnix()).toString()))
                            .onItem().transform(r -> identifiedJob);
                });
    }

    @Override
    public Uni<IdentifiedJob> getJob(long id) {
        return this.redis.get(this.createJobKey(id)).onItem().ifNotNull()
                .transform(r -> this.fromJson(r.toString()));
    }

    @Override
    public Multi<IdentifiedJob> findJobs() {
        return this.redis.evalsha(List.of(this.popJobsLuaSha, "1", JOB_ZLIST_KEY,
                        Long.valueOf(System.currentTimeMillis() / 1000).toString()))
                .onItem().transformToMulti(r -> {
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
        List<String> idList = Arrays.stream(ids).map(Object::toString).collect(Collectors.toList());

        List<String> args = new ArrayList<>();
        args.add(this.removeJobsLuaSha);

        // Key Size + keys
        args.add(idList.size() + "");
        args.addAll(idList);

        // args
        args.add(JOB_ZLIST_KEY);

        return this.redis.evalsha(args).onItem().transform(r -> null);
    }

    @Override
    public Uni<Void> updateJobTime(long id, LocalDateTime jobTime, boolean addIteration) {
        // There is some JSON parsing that has to go on here
        return this.getJob(id)
                .onItem().ifNull().fail()
                .onItem().transform(res -> {
                    res.getTime().setFirstRunUnix(jobTime.atZone(ZoneId.of("UTC")).toEpochSecond());
                    if(addIteration)
                        res.getTime().setIterationsCount(res.getTime().getIterations() + 1);
                    return this.jsonifyJob(res);
                })
                .onItem().ifNull().fail()
                .chain(r -> this.redis.set(List.of(this.createJobKey(id), r))
                        .onItem().transform(b -> null))
                .onFailure().recoverWithNull().replaceWithVoid();
    }

    private String loadScript(String script) {
        return this.redis.scriptAndAwait(List.of("LOAD", script)).toString();
    }

    /**
     * Creates an incremental job ID in redis using incr
     */
    private Uni<Long> createJobId() {
        return this.redis.incr(JOB_ID_INCR_KEY)
                .onItem().transform(Response::toLong);
    }

    private String createJobKey(long id) {
        return JOB_KEY_PREFIX + id;
    }

    private String jsonifyJob(IdentifiedJob identifiedJob) {
        try {
            return objectMapper.writeValueAsString(identifiedJob);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
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
