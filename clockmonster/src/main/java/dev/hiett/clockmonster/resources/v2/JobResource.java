package dev.hiett.clockmonster.resources.v2;

import dev.hiett.clockmonster.entities.GenericErrorException;
import dev.hiett.clockmonster.entities.GenericErrorResponse;
import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.arc.Unremovable;
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
public class JobResource {

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
                    if(e instanceof GenericErrorException) {
                        return ((GenericErrorException) e).getResponse().toResponse();
                    }

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
        return jobService.deleteJob(id).onItem().transform(res -> Response.ok().build());
    }

    @DELETE
    @Path("/batch")
    public Uni<Response> batchDeleteJob(@QueryParam("ids") String ids) {
        // Split the ids into strings
        List<Long> parsedIds = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        return jobService.batchDeleteJobs(parsedIds.toArray(new Long[]{}))
                .onItem().transform(res -> Response.ok().build());
    }
}
