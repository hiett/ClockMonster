package dev.hiett.clockmonster.resources.v2;

import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
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
                .onItem().ifNull().fail()
                .chain(p -> jobService.createJob(p))
                .onItem().transform(identifiedJob -> Response.ok(identifiedJob).build())
                .onFailure().recoverWithItem(Response.status(422).entity("Invalid action configuration provided.").build());
    }

    @GET
    public Uni<Response> getJob(@QueryParam("id") long id) {
        return jobService.getJob(id)
                .onItem().transform(identifiedJob -> {
                    if(identifiedJob == null)
                        return Response.status(404).build();

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
