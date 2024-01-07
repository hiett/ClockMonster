package dev.hiett.clockmonster.entities.action.impls;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.hiett.clockmonster.entities.action.ActionPayload;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;

@RegisterForReflection
public class HttpActionPayload implements ActionPayload {

    @NotNull
    @URL
    private final String url;

    // Can be null to disable signing
    private final String signingSecret;

    private final Map<String, String> additionalHeaders;

    @ConstructorProperties({"http", "signingSecret", "additionalHeaders"})
    public HttpActionPayload(String url, String signingSecret, Map<String, String> additionalHeaders) {
        this.url = url;
        this.signingSecret = signingSecret;
        this.additionalHeaders = additionalHeaders;
    }

    public String getUrl() {
        return url;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public Map<String, String> getAdditionalHeaders() {
        if(additionalHeaders == null)
            return new HashMap<>();

        return additionalHeaders;
    }

    @JsonIgnore
    public boolean signingEnabled() {
        return getSigningSecret() != null;
    }
}
