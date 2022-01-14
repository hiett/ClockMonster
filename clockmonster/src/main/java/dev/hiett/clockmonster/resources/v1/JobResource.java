package dev.hiett.clockmonster.resources.v1;

import dev.hiett.clockmonster.payloads.CreateJobPayload;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.RequestScoped;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

@RequestScoped
@Unremovable
public class JobResource{

    @POST
    public Uni<Response> createJob(@Valid CreateJobPayload payload) {
        return Uni.createFrom().item(Response.ok(payload).build());
    }

    @GET
    public Uni<Response> getJob() {
        return Uni.createFrom().item(Response.ok("Working!").build());
    }

    @DELETE
    public Uni<Response> deleteJob() {
        return Uni.createFrom().item(Response.ok("Working!").build());
    }
}
