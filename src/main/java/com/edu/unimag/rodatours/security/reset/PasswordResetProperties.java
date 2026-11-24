package com.edu.unimag.rodatours.security.reset;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rodatours.password-reset")
@Getter @Setter
public class PasswordResetProperties {
    private boolean enabled;
    private String encryptionKey = "";
    private String frontendUrl = "";
}
