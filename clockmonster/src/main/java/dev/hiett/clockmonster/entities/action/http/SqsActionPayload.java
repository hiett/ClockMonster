package dev.hiett.clockmonster.entities.action.http;

import dev.hiett.clockmonster.entities.action.ActionPayload;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;
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

    @ConstructorProperties({"queueUrl", "accessKeyId", "secretAccessKey", "region"})
    public SqsActionPayload(String queueUrl, String accessKeyId, String secretAccessKey, String region) {
        this.queueUrl = queueUrl;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
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
}
