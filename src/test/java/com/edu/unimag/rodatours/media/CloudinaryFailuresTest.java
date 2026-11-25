package com.edu.unimag.rodatours.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class CloudinaryFailuresTest {
    @Test
    void neverLogsRemoteBodyOrExceptionMessage(CapturedOutput output) {
        var remote = new RestClientResponseException("private-secret signed-url", 401, "Unauthorized", null,
                "api_secret=private-secret".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        var result = CloudinaryFailures.failure("ELIMINAR_VIDEO", remote);
        assertTrue(result.getMessage().contains("credenciales"));
        assertNull(result.getCause());
        assertTrue(output.getAll().contains("categoria=AUTENTICACION http=401"));
        assertFalse(output.getAll().contains("private-secret"));
        assertFalse(output.getAll().contains("signed-url"));
    }

    @Test
    void classifiesNetworkAndRateLimitFailures() {
        assertTrue(CloudinaryFailures.failure("VERIFICAR_VIDEO", new ResourceAccessException("private"))
                .getMessage().contains("conectar"));
        assertTrue(CloudinaryFailures.failure("VERIFICAR_VIDEO",
                new RestClientResponseException("private", 429, "limit", null, null, null))
                .getMessage().contains("límite"));
    }

    @Test
    void rejectsPlaceholderSecretsWithoutLoggingValues(CapturedOutput output) {
        var properties = new CloudinaryProperties();
        properties.setCloudName("test-cloud");
        properties.setApiKey("123");
        properties.setApiSecret("<private-secret>");
        assertThrows(MediaProviderException.class, () -> CloudinaryFailures.configured(properties, "signed-preset"));
        assertTrue(output.getAll().contains("categoria=CONFIGURACION"));
        assertFalse(output.getAll().contains("private-secret"));
    }
}
