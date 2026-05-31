package com.invoiceai.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private DateUtils() {
    }

    public static String format(LocalDate date) {
        return date != null ? date.format(FORMATTER) : null;
    }

    public static LocalDate parse(String value) {
        return value != null ? LocalDate.parse(value, FORMATTER) : null;
    }
}




