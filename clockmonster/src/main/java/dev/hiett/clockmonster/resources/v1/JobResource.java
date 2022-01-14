package dev.hiett.clockmonster.resources.v1;

import dev.hiett.clockmonster.entities.job.UnidentifiedJob;
import dev.hiett.clockmonster.services.job.JobService;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@RequestScoped
@Unremovable
public class JobResource{

    @Inject
    JobService jobService;

    @POST
    public Uni<Response> createJob(@Valid UnidentifiedJob payload) {
        return jobService.createJob(payload)
                .onItem().transform(identifiedJob -> Response.ok(identifiedJob).build());
    }

    @GET
    public Uni<Response> getJob(@QueryParam(value = "id") long id) {
        return jobService.getJob(id)
                .onItem().transform(identifiedJob -> {
                    if(identifiedJob == null)
                        return Response.status(404).build();

                    return Response.ok(identifiedJob).build();
                });
    }

    @DELETE
    public Uni<Response> deleteJob(@QueryParam(value = "id") long id) {
        return jobService.deleteJob(id).onItem().transform(res -> Response.ok().build());
    }
}
