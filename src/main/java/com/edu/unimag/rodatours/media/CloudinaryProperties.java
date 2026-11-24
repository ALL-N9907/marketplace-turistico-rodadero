package com.edu.unimag.rodatours.media;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "rodatours.cloudinary")
@Getter
@Setter
public class CloudinaryProperties {
    private String cloudName = "";
    private String apiKey = "";
    private String apiSecret = "";
    private String uploadPreset = "";
    private String imageUploadPreset = "";
    private long maxBytes = 52_428_800;
    private double maxDurationSeconds = 120;
}
