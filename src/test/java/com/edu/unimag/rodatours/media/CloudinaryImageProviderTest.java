package com.edu.unimag.rodatours.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudinaryImageProviderTest {
    private CloudinaryImageProvider provider;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        var properties = new CloudinaryProperties();
        properties.setCloudName("test-cloud");
        properties.setApiKey("123");
        properties.setApiSecret("private-secret");
        properties.setImageUploadPreset("photos");
        var builder = RestClient.builder().baseUrl("https://api.cloudinary.com/v1_1");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new CloudinaryImageProvider(properties, builder.build());
    }

    @Test
    void acceptsMissingRemoteFileOnDelete() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/destroy"))
                .andRespond(withSuccess("{\"result\":\"not found\"}", MediaType.APPLICATION_JSON));
        assertDoesNotThrow(() -> provider.delete("pending-image"));
        server.verify();
    }

    @Test
    void reportsAuthenticationWithoutForwardingSecrets() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("private-secret"));
        var error = assertThrows(MediaProviderException.class, () -> provider.delete("pending-image"));
        assertTrue(error.getMessage().contains("credenciales"));
        assertFalse(error.getMessage().contains("private-secret"));
        assertNull(error.getCause());
        server.verify();
    }

    @Test
    void reportsMissingUploadSeparately() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        var error = assertThrows(MediaProviderException.class, () -> provider.inspect("pending-image"));
        assertTrue(error.getMessage().contains("aún no existe"));
        server.verify();
    }
}
