package dev.hiett.clockmonster.resources.v2;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/v2")
public class V2Router {

    @Inject
    ResourceContext resourceContext;

    @Path("/job")
    public JobResource jobResource() {
        return resourceContext.getResource(JobResource.class);
    }
}
