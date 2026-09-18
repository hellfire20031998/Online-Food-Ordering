package com.hellfire.security;

import com.hellfire.config.EncryptedStringConverter;
import com.hellfire.config.EncryptionKeyRing;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Re-encrypts every encrypted column under the current key. Run after rotating
 * {@code DATA_ENCRYPTION_KEY}; rows already on the current key are left untouched.
 * Works directly on the stored ciphertext so it never depends on entity dirty-checking.
 */
@Service
@RequiredArgsConstructor
public class KeyRotationService {

    private static final Logger log = LoggerFactory.getLogger(KeyRotationService.class);

    /** Every encrypted column in the schema. Keep in sync with the @Convert usages. */
    static final List<String[]> ENCRYPTED_COLUMNS = List.of(
            new String[]{"restaurant_applications", "bank_account_number"},
            new String[]{"restaurant_applications", "upi_id"},
            new String[]{"restaurant_bank_accounts", "account_number"},
            new String[]{"restaurant_bank_accounts", "upi_id"},
            new String[]{"customer_bank_accounts", "account_number"},
            new String[]{"customer_bank_accounts", "upi_id"},
            new String[]{"refunds", "account_number"},
            new String[]{"refunds", "upi_id"},
            new String[]{"payouts", "account_number"},
            new String[]{"payments", "provider_client_secret"});

    private final JdbcTemplate jdbcTemplate;
    private final EncryptedStringConverter converter;
    private final EncryptionKeyRing keyRing;

    public record Status(String currentKeyFingerprint, int previousKeys, boolean strongKey,
                         Map<String, Long> rowsNotOnCurrentKey) {
    }

    public record Report(String currentKeyFingerprint, Map<String, Integer> reencrypted) {
    }

    @Transactional(readOnly = true)
    public Status status() {
        Map<String, Long> stale = new LinkedHashMap<>();
        for (String[] target : ENCRYPTED_COLUMNS) {
            long count = jdbcTemplate.query("select " + target[1] + " from " + target[0] + " where " + target[1] + " is not null",
                            (rs, i) -> rs.getString(1)).stream()
                    .filter(v -> !converter.isEncryptedWithCurrentKey(v))
                    .count();
            if (count > 0) {
                stale.put(target[0] + "." + target[1], count);
            }
        }
        return new Status(keyRing.current().fingerprint(), keyRing.all().size() - 1, keyRing.isStrong(), stale);
    }

    @Transactional
    public Report reencryptAll(String actor) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String[] target : ENCRYPTED_COLUMNS) {
            String table = target[0];
            String column = target[1];
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select id, " + column + " from " + table + " where " + column + " is not null");
            int changed = 0;
            for (Map<String, Object> row : rows) {
                String stored = (String) row.get(column);
                if (stored == null || converter.isEncryptedWithCurrentKey(stored)) {
                    continue;
                }
                String plaintext = converter.convertToEntityAttribute(stored);
                jdbcTemplate.update("update " + table + " set " + column + " = ? where id = ?",
                        converter.convertToDatabaseColumn(plaintext), row.get("id"));
                changed++;
            }
            counts.put(table + "." + column, changed);
        }
        log.info("AUDIT encryption key rotation: re-encrypted {} by {}", counts, actor);
        return new Report(keyRing.current().fingerprint(), counts);
    }
}
