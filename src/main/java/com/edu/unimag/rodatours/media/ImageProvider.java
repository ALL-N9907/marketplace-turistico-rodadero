package com.edu.unimag.rodatours.media;

public interface ImageProvider {
    VideoProvider.UploadAuthorization authorize(String publicId);
    ImageMetadata inspect(String publicId);
    void delete(String publicId);
    record ImageMetadata(String url, String format, long bytes, int width, int height) {}
}
