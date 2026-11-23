package com.edu.unimag.rodatours.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudinaryVideoProviderTest {
    private CloudinaryProperties properties;
    private CloudinaryVideoProvider provider;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        properties = new CloudinaryProperties();
        properties.setCloudName("test-cloud");
        properties.setApiKey("123");
        properties.setApiSecret("private-secret");
        properties.setUploadPreset("tour-videos");
        var builder = RestClient.builder().baseUrl("https://api.cloudinary.com/v1_1");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new CloudinaryVideoProvider(properties, builder.build());
    }

    @Test
    void signsOnlyServerControlledParametersWithoutExposingSecret() throws Exception {
        var authorization = provider.authorize("test-video", Instant.ofEpochSecond(1700000000));
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                ("overwrite=false&public_id=test-video&timestamp=1700000000&upload_preset=tour-videosprivate-secret")
                        .getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected, authorization.signature());
        assertEquals("https://api.cloudinary.com/v1_1/test-cloud/video/upload", authorization.uploadUrl());
        assertEquals("false", authorization.parameters().get("overwrite"));
        assertFalse(authorization.toString().contains("private-secret"));
        server.verify();
    }

    @Test
    void failsClosedWhenNotConfigured() {
        properties.setApiSecret("");
        assertThrows(MediaProviderException.class, () -> provider.authorize("id", Instant.now()));
        server.verify();
    }

    @Test
    void verifiesVideoUsingAuthenticatedProviderMetadata() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/resources/video/upload/test-video"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Basic MTIzOnByaXZhdGUtc2VjcmV0"))
                .andRespond(withSuccess("""
                        {"public_id":"test-video","resource_type":"video","format":"mp4","bytes":1000,
                         "duration":12.5,"width":1280,"height":720,
                         "secure_url":"https://res.cloudinary.com/test-cloud/video/upload/v1/test-video.mp4"}
                        """, MediaType.APPLICATION_JSON));
        var metadata = provider.inspect("test-video");
        assertEquals(1000, metadata.bytes());
        assertEquals(12.5, metadata.duration());
        server.verify();
    }

    @Test
    void rejectsUntrustedProviderMetadata() {
        server.expect(anything()).andRespond(withSuccess("""
                {"public_id":"someone-elses-video","resource_type":"video"}
                """, MediaType.APPLICATION_JSON));
        assertThrows(MediaProviderException.class, () -> provider.inspect("test-video"));
        server.verify();
    }

    @Test
    void mapsProviderFailuresWithoutForwardingProviderBody() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .body("api_secret=private-secret"));
        var exception = assertThrows(MediaProviderException.class, () -> provider.inspect("test-video"));
        assertFalse(exception.getMessage().contains("private-secret"));
        server.verify();
    }

    @Test
    void deletesWithSignedRequestAndCacheInvalidation() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/video/destroy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("invalidate=true")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-secret"))))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));
        provider.delete("test-video");
        server.verify();
    }
}
