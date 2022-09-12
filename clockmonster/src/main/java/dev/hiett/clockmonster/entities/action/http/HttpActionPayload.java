package dev.hiett.clockmonster.entities.action.http;

import dev.hiett.clockmonster.entities.action.ActionPayload;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;

@RegisterForReflection
public class HttpActionPayload implements ActionPayload {

    @NotNull
    @URL
    private final String url;

    @ConstructorProperties({"http"})
    public HttpActionPayload(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
