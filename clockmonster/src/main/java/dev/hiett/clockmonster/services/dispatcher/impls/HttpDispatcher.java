package dev.hiett.clockmonster.services.dispatcher.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.action.ActionType;
import dev.hiett.clockmonster.entities.action.impls.HttpActionPayload;
import dev.hiett.clockmonster.entities.job.IdentifiedJob;
import dev.hiett.clockmonster.services.dispatcher.Dispatcher;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.Future;

public class HttpDispatcher implements Dispatcher<HttpActionPayload> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Uni<Boolean> dispatchJob(IdentifiedJob job, HttpActionPayload httpActionPayload, Object payload) {
        Client client = ClientBuilder.newClient();

        // Encode the payload to JSON
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Uni.createFrom().item(false);
        }

        Invocation.Builder builder = client.target(httpActionPayload.getUrl())
                .request();

        // Add all the additional headers that might be defined
        for (Map.Entry<String, String> headerEntry : httpActionPayload.getAdditionalHeaders().entrySet())
            builder.header(headerEntry.getKey(), headerEntry.getValue());

        builder.header("x-dispatched-from", "ClockMonster");
        builder.header("x-clockmonster-job-id", job.getId() + "");
        builder.header("x-clockmonster-job-target-unix", job.getTime().getNextRunUnix() + "");

        if(httpActionPayload.signingEnabled()) {
            // Signing is enabled, create a signature for x-webhook-signate
            try {
                long timestamp = System.currentTimeMillis();
                String hmac = calculateHmac(httpActionPayload.getSigningSecret(), jsonPayload + timestamp);
                String webhookSignature = "v=" + timestamp + ",d=" + hmac;

                builder.header("x-webhook-signature", webhookSignature);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
                return Uni.createFrom().item(false);
            }
        }

        Future<Response> response = builder
                .async()
                .post(Entity.entity(jsonPayload, MediaType.APPLICATION_JSON));

        return Uni.createFrom().future(response)
                .onItem().transform(res -> res.getStatus() < 300);
    }

    @Override
    public ActionType getActionType() {
        return ActionType.HTTP;
    }

    private static String calculateHmac(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);

        // Hex encode
        byte[] utf8Data = data.getBytes(StandardCharsets.UTF_8);
        byte[] signed = sha256Hmac.doFinal(utf8Data);

        // Return the signed as hex
        return bytesToHex(signed);
    }

    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
}
