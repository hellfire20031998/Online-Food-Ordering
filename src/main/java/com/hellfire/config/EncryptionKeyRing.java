package com.hellfire.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Holds the current data-encryption key plus any previous keys still needed to read old rows.
 * <p>
 * Key management expectations (RBI Master Direction on Digital Payment Security Controls, and the
 * IT framework's guidance on protecting sensitive customer data): AES-256, keys supplied from the
 * environment or a secrets manager and never committed, a distinct key per environment, periodic
 * rotation, and a way to re-encrypt existing data. Rotation here is: move the old value of
 * {@code DATA_ENCRYPTION_KEY} into {@code DATA_ENCRYPTION_PREVIOUS_KEYS}, set a new current key,
 * restart, then run the re-encryption job from the team console.
 * <p>
 * Each key gets a short fingerprint (a hash of a label plus the material, so it reveals nothing
 * about the key bytes) that is stored alongside ciphertext to pick the right key on decryption.
 */
@Component
public class EncryptionKeyRing {

    private static final Logger log = LoggerFactory.getLogger(EncryptionKeyRing.class);
    private static final int MIN_KEY_LENGTH = 32;
    private static final String DEV_KEY_PREFIX = "local-dev-only";

    public record Key(String fingerprint, SecretKey secret) {
    }

    private final Key current;
    private final List<Key> all;
    private final boolean strong;

    public EncryptionKeyRing(@Value("${app.encryption.key}") String currentMaterial,
                             @Value("${app.encryption.previous-keys:}") String previousMaterials,
                             @Value("${app.encryption.enforce-strong-key:false}") boolean enforceStrongKey) {
        if (currentMaterial == null || currentMaterial.isBlank()) {
            throw new IllegalStateException("app.encryption.key (DATA_ENCRYPTION_KEY) must not be empty");
        }
        this.current = derive(currentMaterial);
        List<Key> keys = new ArrayList<>();
        keys.add(current);
        if (previousMaterials != null) {
            for (String material : previousMaterials.split(",")) {
                if (!material.isBlank()) {
                    keys.add(derive(material.trim()));
                }
            }
        }
        this.all = Collections.unmodifiableList(keys);

        this.strong = currentMaterial.length() >= MIN_KEY_LENGTH && !currentMaterial.startsWith(DEV_KEY_PREFIX);
        if (!strong) {
            String message = "DATA_ENCRYPTION_KEY is weak or the development default; use a random value of at least "
                    + MIN_KEY_LENGTH + " characters in any real environment";
            if (enforceStrongKey) {
                throw new IllegalStateException(message);
            }
            log.warn(message);
        }
        log.info("Data encryption key ring loaded: current={} previous={}", current.fingerprint(), keys.size() - 1);
    }

    public Key current() {
        return current;
    }

    public List<Key> all() {
        return all;
    }

    public Optional<Key> byFingerprint(String fingerprint) {
        return all.stream().filter(k -> k.fingerprint().equals(fingerprint)).findFirst();
    }

    /** Whether the current key meets the minimum strength policy. */
    public boolean isStrong() {
        return strong;
    }

    static Key derive(String material) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(material.getBytes(StandardCharsets.UTF_8));
            byte[] fpBytes = MessageDigest.getInstance("SHA-256").digest(("fingerprint:" + material).getBytes(StandardCharsets.UTF_8));
            String fingerprint = HexFormat.of().formatHex(fpBytes, 0, 4);
            return new Key(fingerprint, new SecretKeySpec(keyBytes, "AES"));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot derive encryption key", e);
        }
    }
}
