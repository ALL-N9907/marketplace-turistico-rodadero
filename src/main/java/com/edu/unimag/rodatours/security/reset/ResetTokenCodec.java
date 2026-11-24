package com.edu.unimag.rodatours.security.reset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class ResetTokenCodec {
    private final PasswordResetProperties properties;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 no disponible."); }
    }
    public byte[] key() {
        try { byte[] key = Base64.getDecoder().decode(properties.getEncryptionKey()); if (key.length == 32) return key; }
        catch (IllegalArgumentException ignored) { }
        throw new IllegalStateException("RESET_ENCRYPTION_KEY debe contener 32 bytes aleatorios en Base64.");
    }
    public String encrypt(String token) {
        try {
            byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, iv));
            return Base64.getEncoder().encodeToString(iv) + "." + Base64.getEncoder().encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("No fue posible proteger el token de recuperación."); }
    }
    public String decrypt(String encrypted) {
        try {
            var parts = encrypted.split("\\.");
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("No fue posible abrir el correo de recuperación."); }
    }
}
