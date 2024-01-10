package dev.hiett.clockmonster.entities.action.impls;

import dev.hiett.clockmonster.entities.action.ActionPayload;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import java.beans.ConstructorProperties;

@RegisterForReflection
public class SqsActionPayload implements ActionPayload {

    @NotNull
    @URL
    private final String queueUrl;

    @NotNull
    private final String accessKeyId;

    @NotNull
    private final String secretAccessKey;

    @NotNull
    private final String region;

    /**
     * Wrapping the payload is including what would be headers around the payload.
     * This is because there isn't a direct way to include "headers" like there is in HTTP,
     * so we offer the option if the information is required to just encapsulate the real payload with that info.
     */
    private final boolean wrapPayload;

    @ConstructorProperties({"queueUrl", "accessKeyId", "secretAccessKey", "region", "wrapPayload"})
    public SqsActionPayload(String queueUrl, String accessKeyId, String secretAccessKey, String region, boolean wrapPayload) {
        this.queueUrl = queueUrl;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
        this.wrapPayload = wrapPayload;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getRegion() {
        return region;
    }

    public boolean isWrapPayload() {
        return wrapPayload;
    }
}
