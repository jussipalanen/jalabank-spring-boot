package jussinet.jalabank.release.model;

import java.util.Locale;

public enum SortDirection {
    ASC, DESC;

    /**
     * Parses {@code asc} or {@code desc}, ignoring case.
     *
     * @throws IllegalArgumentException for any other value
     */
    public static SortDirection parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("sortDirection must be ASC or DESC");
        }
    }
}
