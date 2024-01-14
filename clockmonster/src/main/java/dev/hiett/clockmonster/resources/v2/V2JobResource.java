package dev.hiett.clockmonster.resources.v2;

import dev.hiett.clockmonster.entities.GenericErrorException;
import dev.hiett.clockmonster.entities.GenericErrorResponse;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequestScoped
@Unremovable
public class V2JobResource {

    @Inject
    JobService jobService;

    @POST
    public Uni<Response> createJob(@Valid UnidentifiedJob payload) {
        return Uni.createFrom().item(payload)
                .onItem().transform(p -> p.getAction().validateActionConfiguration() ? p : null)
                .onItem().ifNull().failWith(new GenericErrorException(new GenericErrorResponse("Only one action configuration can be provided or you didn't provide one", 400)))
                .chain(p -> jobService.createJob(p))
                .onItem().transform(identifiedJob -> Response.ok(identifiedJob).build())
                .onFailure().recoverWithItem(e -> {
                    if(e instanceof GenericErrorException)
                        return ((GenericErrorException) e).getResponse().toResponse();

                    return new GenericErrorResponse(e.getMessage(), 500).toResponse();
                });
    }

    @GET
    public Uni<Response> getJob(@QueryParam("id") long id) {
        return jobService.getJob(id)
                .onItem().transform(identifiedJob -> {
                    if(identifiedJob == null)
                        return new GenericErrorResponse("Cannot find that job!", 404).toResponse();

                    return Response.ok(identifiedJob).build();
                });
    }

    @DELETE
    public Uni<Response> deleteJob(@QueryParam("id") long id) {
        return jobService.safeDeleteJob(id).onItem().transform(res -> Response.ok().build());
    }

    @DELETE
    @Path("/batch")
    public Uni<Response> batchDeleteJobs(@QueryParam("ids") String ids) {
        // Split the ids into strings
        List<Long> parsedIds = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        return Multi.createFrom().items(parsedIds.stream())
                .call(jobId -> jobService.safeDeleteJob(jobId))
                .collect()
                .asList()
                .onItem()
                .transform(res -> Response.ok().build());
    }
}
