package com.hellfire.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedStringConverterTest {

    private static final String KEY_A = "unit-test-key-a-0123456789-0123456789";
    private static final String KEY_B = "unit-test-key-b-0123456789-0123456789";

    private final EncryptedStringConverter converter = new EncryptedStringConverter(ring(KEY_A, ""));

    @Test
    void roundTripsWithVersionedPrefixAndFingerprint() {
        String stored = converter.convertToDatabaseColumn("123456789012");
        assertTrue(stored.startsWith("enc:v2:"));
        assertFalse(stored.contains("123456789012"));
        assertTrue(converter.isEncryptedWithCurrentKey(stored));
        assertEquals("123456789012", converter.convertToEntityAttribute(stored));
    }

    @Test
    void usesFreshIvPerEncryption() {
        assertNotEquals(converter.convertToDatabaseColumn("same"), converter.convertToDatabaseColumn("same"));
    }

    @Test
    void passesThroughNullAndLegacyPlaintext() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals("plain-legacy-value", converter.convertToEntityAttribute("plain-legacy-value"));
        assertFalse(converter.isEncryptedWithCurrentKey("plain-legacy-value"));
    }

    @Test
    void rotationReadsOldKeyAndWritesNewKey() {
        String storedUnderA = converter.convertToDatabaseColumn("secret");

        // New deployment: B is current, A kept as a previous key.
        EncryptedStringConverter rotated = new EncryptedStringConverter(ring(KEY_B, KEY_A));
        assertEquals("secret", rotated.convertToEntityAttribute(storedUnderA));
        assertFalse(rotated.isEncryptedWithCurrentKey(storedUnderA));

        String reencrypted = rotated.convertToDatabaseColumn(rotated.convertToEntityAttribute(storedUnderA));
        assertTrue(rotated.isEncryptedWithCurrentKey(reencrypted));
        assertEquals("secret", rotated.convertToEntityAttribute(reencrypted));
    }

    @Test
    void unknownKeyIsReportedClearly() {
        String storedUnderA = converter.convertToDatabaseColumn("secret");
        EncryptedStringConverter other = new EncryptedStringConverter(ring(KEY_B, ""));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> other.convertToEntityAttribute(storedUnderA));
        assertTrue(ex.getMessage().contains("DATA_ENCRYPTION_PREVIOUS_KEYS"));
    }

    @Test
    void fingerprintDoesNotLeakKeyBytes() {
        EncryptionKeyRing.Key key = EncryptionKeyRing.derive(KEY_A);
        String keyHex = java.util.HexFormat.of().formatHex(key.secret().getEncoded());
        assertFalse(keyHex.startsWith(key.fingerprint()));
        assertEquals(8, key.fingerprint().length());
    }

    @Test
    void weakKeyPolicy() {
        assertFalse(ring("short", "").isStrong());
        assertFalse(ring("local-dev-only-encryption-key-change-me-0123456789", "").isStrong());
        assertTrue(ring(KEY_A, "").isStrong());
        assertThrows(IllegalStateException.class, () -> new EncryptionKeyRing("short", "", true));
        assertThrows(IllegalStateException.class, () -> new EncryptionKeyRing(" ", "", false));
    }

    private static EncryptionKeyRing ring(String current, String previous) {
        return new EncryptionKeyRing(current, previous, false);
    }
}
