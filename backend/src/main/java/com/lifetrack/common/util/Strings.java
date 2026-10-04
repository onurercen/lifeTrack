package com.lifetrack.common.util;

import java.util.Locale;

public final class Strings {

    private Strings() {
    }

    /** Trims the value; blank or null becomes null so optional columns stay empty. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Canonical form of an e-mail address: trimmed and lower-cased with
     * {@link Locale#ROOT} (a Turkish default locale would turn "I" into "ı").
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
