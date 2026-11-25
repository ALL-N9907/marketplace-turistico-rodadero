package com.edu.unimag.rodatours.media;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Carga directa firmada; los metadatos se consultan al proveedor, nunca se confía en URLs del cliente. */
@Component
public class CloudinaryVideoProvider implements VideoProvider {
    private static final int METADATA_ATTEMPTS = 3;
    private final CloudinaryProperties properties;
    private final RestClient client;
    private final RetryPause pause;

    @org.springframework.beans.factory.annotation.Autowired
    public CloudinaryVideoProvider(CloudinaryProperties properties) {
        this(properties, createClient());
    }

    CloudinaryVideoProvider(CloudinaryProperties properties, RestClient client) {
        this(properties, client, () -> Thread.sleep(500));
    }

    CloudinaryVideoProvider(CloudinaryProperties properties, RestClient client, RetryPause pause) {
        this.properties = properties;
        this.client = client;
        this.pause = pause;
    }

    @FunctionalInterface
    interface RetryPause { void await() throws InterruptedException; }

    private static RestClient createClient() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().baseUrl("https://api.cloudinary.com/v1_1")
                .requestFactory(factory).build();
    }

    private void configured() {
        CloudinaryFailures.configured(properties, properties.getUploadPreset());
    }

    @Override
    public UploadAuthorization authorize(String publicId, Instant issuedAt) {
        configured();
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("public_id", publicId);
        parameters.put("timestamp", Long.toString(issuedAt.getEpochSecond()));
        parameters.put("upload_preset", properties.getUploadPreset());
        parameters.put("overwrite", "false");
        return new UploadAuthorization("https://api.cloudinary.com/v1_1/" + properties.getCloudName() + "/video/upload",
                properties.getApiKey(), Map.copyOf(parameters), sign(parameters));
    }

    private String sign(Map<String, String> parameters) {
        String payload = new TreeMap<>(parameters).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((payload + properties.getApiSecret()).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }

    @Override
    public VideoMetadata inspect(String publicId) {
        configured();
        for (int attempt = 1; attempt <= METADATA_ATTEMPTS; attempt++) {
            try {
                return inspectOnce(publicId);
            } catch (MissingMetadata missing) {
                // Sólo nombres de campos definidos por el servidor; nunca cuerpos o URLs remotos.
                LoggerFactory.getLogger(CloudinaryVideoProvider.class).warn(
                        "Cloudinary operacion=VERIFICAR_VIDEO categoria=METADATOS_PENDIENTES campo={} intento={}/{}",
                        missing.field, attempt, METADATA_ATTEMPTS);
                if (attempt == METADATA_ATTEMPTS) {
                    throw new ConflictException("Cloudinary encontró el archivo, pero aún no devuelve todos los metadatos del video. Espere unos segundos y pulse Reintentar verificación. Si persiste, revise que el archivo tenga imagen de video válida; no vuelva a cargarlo sin eliminar la carga pendiente.");
                }
                try {
                    pause.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new MediaProviderException("Se interrumpió la verificación. Reintente en unos segundos.");
                }
            }
        }
        throw new IllegalStateException("Verificación de video sin resultado.");
    }

    private VideoMetadata inspectOnce(String publicId) {
        try {
            Map<String, Object> response = client.get().uri("/{cloud}/resources/video/upload/{id}?media_metadata=true",
                            properties.getCloudName(), publicId)
                    .headers(headers -> headers.setBasicAuth(properties.getApiKey(), properties.getApiSecret()))
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            if (response == null || !publicId.equals(response.get("public_id"))
                    || !"video".equals(response.get("resource_type"))) {
                throw new MediaProviderException("Cloudinary no devolvió un video válido para esta carga.");
            }
            String url = String.valueOf(response.get("secure_url"));
            if (!url.startsWith("https://res.cloudinary.com/" + properties.getCloudName() + "/video/upload/")) {
                throw new MediaProviderException("Cloudinary no devolvió una URL de entrega válida.");
            }
            return new VideoMetadata(publicId, url, String.valueOf(response.get("format")),
                    number(response, "bytes").longValue(), number(response, "duration").doubleValue(),
                    number(response, "width").intValue(), number(response, "height").intValue());
        } catch (RestClientException exception) {
            throw CloudinaryFailures.failure("VERIFICAR_VIDEO", exception);
        }
    }

    private Number number(Map<String, Object> response, String field) {
        if (response.get(field) == null) {
            throw new MissingMetadata(field);
        }
        if (!(response.get(field) instanceof Number number) || !Double.isFinite(number.doubleValue())
                || number.doubleValue() <= 0) {
            throw new ValidationException("Cloudinary no devolvió un video válido: duración, tamaño y dimensiones deben ser positivos. Revise el archivo antes de reemplazar la carga pendiente.");
        }
        return number;
    }

    private static final class MissingMetadata extends RuntimeException {
        private final String field;
        private MissingMetadata(String field) { this.field = field; }
    }

    @Override
    public void delete(String publicId) {
        configured();
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("public_id", publicId);
        parameters.put("timestamp", Long.toString(Instant.now().getEpochSecond()));
        parameters.put("invalidate", "true");
        var body = new LinkedMultiValueMap<String, String>();
        parameters.forEach(body::add);
        body.add("api_key", properties.getApiKey());
        body.add("signature", sign(parameters));
        try {
            Map<String, Object> response = client.post().uri("/{cloud}/video/destroy", properties.getCloudName())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(body).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !("ok".equals(response.get("result")) || "not found".equals(response.get("result")))) {
                throw new MediaProviderException("No se pudo eliminar el video en Cloudinary.");
            }
        } catch (RestClientException exception) {
            throw CloudinaryFailures.failure("ELIMINAR_VIDEO", exception);
        }
    }
}
