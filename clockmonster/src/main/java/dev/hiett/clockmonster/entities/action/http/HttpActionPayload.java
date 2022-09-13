package dev.hiett.clockmonster.entities.action.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // Can be null to disable signing
    private final String signingSecret;

    @ConstructorProperties({"http", "signingSecret"})
    public HttpActionPayload(String url, String signingSecret) {
        this.url = url;
        this.signingSecret = signingSecret;
    }

    public String getUrl() {
        return url;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    @JsonIgnore
    public boolean signingEnabled() {
        return getSigningSecret() != null;
    }
}
