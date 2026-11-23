package com.edu.unimag.rodatours.media;

import java.time.Instant;
import java.util.Map;

public interface VideoProvider {
    UploadAuthorization authorize(String publicId, Instant issuedAt);
    VideoMetadata inspect(String publicId);
    void delete(String publicId);

    record UploadAuthorization(String uploadUrl, String apiKey, Map<String, String> parameters, String signature) {}
    record VideoMetadata(String publicId, String secureUrl, String format, long bytes,
                         double duration, int width, int height) {}
}
