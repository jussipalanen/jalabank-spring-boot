package jussinet.jalabank.release.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

/**
 * The demo authenticator: a random 6-digit code for each customer and each hour of the day.
 * <p>
 * Codes are created when they are first needed and kept in memory only. Each customer gets a code for the current
 * hour and the next few hours, so the code list stays the same while the hour lasts. A code is valid during its
 * own hour, and for a short grace period after it, so a code copied at 13:59 still works at 14:00.
 */
@Service
public class AuthenticatorService {

    /** How many upcoming hours the code list shows after the current one. */
    public static final int UPCOMING_HOURS = 5;

    /** How long an hour's code still works after the hour has ended. */
    static final Duration GRACE_PERIOD = Duration.ofMinutes(2);

    /** One hour's code. {@code current} is true for the code that is valid now. */
    public record HourlyCode(LocalDateTime validFrom, LocalDateTime validUntil, String code, boolean current) {
    }

    private record CodeKey(long customerId, LocalDateTime hour) {
    }

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<CodeKey, String> codes = new ConcurrentHashMap<>();

    public AuthenticatorService(Clock clock) {
        this.clock = clock;
    }

    /**
     * The customer's code for the current hour, followed by the codes of the next {@value #UPCOMING_HOURS} hours.
     */
    public List<HourlyCode> codes(long customerId) {
        LocalDateTime currentHour = currentHour();
        removeExpired(currentHour);
        return IntStream.rangeClosed(0, UPCOMING_HOURS)
                .mapToObj(i -> {
                    LocalDateTime hour = currentHour.plusHours(i);
                    return new HourlyCode(hour, hour.plusHours(1), codeFor(customerId, hour), i == 0);
                })
                .toList();
    }

    /**
     * Whether {@code code} is the customer's code for the current hour, or for the previous hour during the grace
     * period.
     */
    public boolean verify(long customerId, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime currentHour = now.truncatedTo(ChronoUnit.HOURS);
        removeExpired(currentHour);
        if (matches(codeFor(customerId, currentHour), code)) {
            return true;
        }
        // Never create a code for the previous hour here: one nobody has seen cannot be typed in
        String previous = codes.get(new CodeKey(customerId, currentHour.minusHours(1)));
        return previous != null && now.isBefore(currentHour.plus(GRACE_PERIOD)) && matches(previous, code);
    }

    /** Time left until the current code expires (not counting the grace period). */
    public Duration timeLeft() {
        LocalDateTime now = LocalDateTime.now(clock);
        return Duration.between(now, now.truncatedTo(ChronoUnit.HOURS).plusHours(1));
    }

    /** The time zone the code hours are in, such as UTC on most hosts. */
    public String zone() {
        ZoneId zone = clock.getZone();
        return zone.normalized().equals(ZoneOffset.UTC) ? "UTC" : zone.getId();
    }

    private LocalDateTime currentHour() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.HOURS);
    }

    private String codeFor(long customerId, LocalDateTime hour) {
        return codes.computeIfAbsent(new CodeKey(customerId, hour), key -> "%06d".formatted(random.nextInt(1_000_000)));
    }

    /** Forgets codes from before the previous hour, so the map does not grow without limit. */
    private void removeExpired(LocalDateTime currentHour) {
        LocalDateTime oldest = currentHour.minusHours(1);
        codes.keySet().removeIf(key -> key.hour().isBefore(oldest));
    }

    private static boolean matches(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }
}
