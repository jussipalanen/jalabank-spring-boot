package jussinet.boobank.release.web;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Formats euro amounts. Exposed to the templates as {@code @money}.
 */
@Component("money")
public class MoneyFormatter {

    private static final Locale LOCALE = Locale.forLanguageTag("fi-FI");

    public String format(BigDecimal amount) {
        // NumberFormat is not thread-safe, so create one per call
        NumberFormat format = NumberFormat.getCurrencyInstance(LOCALE);
        format.setCurrency(Currency.getInstance("EUR"));
        return format.format(amount);
    }
}
