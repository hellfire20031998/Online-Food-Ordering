package com.hellfire.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts a String column at rest with AES-256-GCM using the {@link EncryptionKeyRing}.
 * <p>
 * Stored form: {@code enc:v2:<key fingerprint>:<base64(iv || ciphertext || tag)>}. A fresh 96-bit IV
 * is used per value, so equal plaintexts never produce equal ciphertexts. Older rows may carry the
 * v1 form {@code enc:<base64>} (no fingerprint; tried against every key) or plaintext (returned as
 * is), so rollout and key rotation are safe. {@link com.hellfire.security.KeyRotationService}
 * re-encrypts everything under the current key.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    static final String PREFIX_V1 = "enc:";
    static final String PREFIX_V2 = "enc:v2:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final EncryptionKeyRing keyRing;
    private final SecureRandom random = new SecureRandom();

    public EncryptedStringConverter(EncryptionKeyRing keyRing) {
        this.keyRing = keyRing;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        EncryptionKeyRing.Key key = keyRing.current();
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key.secret(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext);
            return PREFIX_V2 + key.fingerprint() + ":" + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || !dbData.startsWith(PREFIX_V1)) {
            return dbData; // legacy plaintext
        }
        if (dbData.startsWith(PREFIX_V2)) {
            String rest = dbData.substring(PREFIX_V2.length());
            int sep = rest.indexOf(':');
            if (sep <= 0) {
                throw new IllegalStateException("Malformed encrypted value");
            }
            String fingerprint = rest.substring(0, sep);
            EncryptionKeyRing.Key key = keyRing.byFingerprint(fingerprint).orElseThrow(() -> new IllegalStateException(
                    "Encrypted with key " + fingerprint + " which is not configured; add it to DATA_ENCRYPTION_PREVIOUS_KEYS"));
            return decrypt(key, rest.substring(sep + 1));
        }
        // v1: no fingerprint, try every key (current first)
        String payload = dbData.substring(PREFIX_V1.length());
        for (EncryptionKeyRing.Key key : keyRing.all()) {
            try {
                return decrypt(key, payload);
            } catch (IllegalStateException e) {
                if (!(e.getCause() instanceof AEADBadTagException)) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Decryption failed; was the encryption key changed without keeping the previous one?");
    }

    /** True when the stored value is already encrypted under the current key. */
    public boolean isEncryptedWithCurrentKey(String dbData) {
        return dbData != null && dbData.startsWith(PREFIX_V2 + keyRing.current().fingerprint() + ":");
    }

    private static String decrypt(EncryptionKeyRing.Key key, String base64) {
        try {
            byte[] payload = Base64.getDecoder().decode(base64);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_BYTES);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key.secret(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
