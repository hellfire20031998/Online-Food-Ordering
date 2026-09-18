package com.hellfire.onboarding;

import java.util.regex.Pattern;

/** Masking and validation helpers for payout account data. */
public final class BankDetails {

    private static final Pattern UPI = Pattern.compile("^[\\w.\\-]{2,256}@[A-Za-z]{2,64}$");

    private BankDetails() {
    }

    public static String maskAccountNumber(String number) {
        if (number == null || number.isBlank()) {
            return null;
        }
        if (number.length() <= 4) {
            return "****";
        }
        return "*".repeat(number.length() - 4) + number.substring(number.length() - 4);
    }

    public static String maskUpi(String upi) {
        if (upi == null || upi.isBlank()) {
            return null;
        }
        int at = upi.indexOf('@');
        if (at <= 0) {
            return "****";
        }
        String local = upi.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "****" + upi.substring(at);
    }

    /** Returns the trimmed UPI id, null when blank; throws when present but malformed. */
    public static String normalizeUpi(String upi) {
        if (upi == null || upi.isBlank()) {
            return null;
        }
        String trimmed = upi.trim();
        if (!UPI.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("upiId must look like name@bank");
        }
        return trimmed;
    }

    public static String normalizeIfsc(String ifsc) {
        return ifsc == null ? null : ifsc.trim().toUpperCase();
    }
}
