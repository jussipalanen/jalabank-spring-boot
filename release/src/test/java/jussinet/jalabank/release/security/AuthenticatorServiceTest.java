package jussinet.jalabank.release.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jussinet.jalabank.release.security.AuthenticatorService.HourlyCode;

class AuthenticatorServiceTest {

    private static final long JOHN = 1;
    private static final long JANE = 2;

    /** A clock the tests can move forward */
    private static final class TestClock extends Clock {
        private Instant now;

        TestClock(String dateTime) {
            set(dateTime);
        }

        void set(String dateTime) {
            now = LocalDateTime.parse(dateTime).toInstant(ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private TestClock clock;
    private AuthenticatorService service;

    @BeforeEach
    void setUp() {
        clock = new TestClock("2026-03-10T13:20:00");
        service = new AuthenticatorService(clock);
    }

    @Test
    void listsTheCurrentAndUpcomingHours() {
        List<HourlyCode> codes = service.codes(JOHN);

        assertThat(codes).hasSize(AuthenticatorService.UPCOMING_HOURS + 1);
        assertThat(codes).allSatisfy(c -> assertThat(c.code()).matches("\\d{6}"));
        assertThat(codes.getFirst().current()).isTrue();
        assertThat(codes.getFirst().validFrom()).isEqualTo(LocalDateTime.parse("2026-03-10T13:00"));
        assertThat(codes.getFirst().validUntil()).isEqualTo(LocalDateTime.parse("2026-03-10T14:00"));
        assertThat(codes.get(1).current()).isFalse();
        assertThat(codes.get(1).validFrom()).isEqualTo(LocalDateTime.parse("2026-03-10T14:00"));
        assertThat(service.timeLeft().toMinutes()).isEqualTo(40);
        assertThat(service.zone()).isEqualTo("UTC");
    }

    @Test
    void codesStayTheSameDuringTheHour() {
        List<HourlyCode> first = service.codes(JOHN);
        clock.set("2026-03-10T13:59:00");

        assertThat(service.codes(JOHN)).isEqualTo(first);
    }

    @Test
    void acceptsOnlyTheCurrentCodeOfTheSameCustomer() {
        String johns = service.codes(JOHN).getFirst().code();
        String johnsNext = service.codes(JOHN).get(1).code();

        assertThat(service.verify(JOHN, johns)).isTrue();
        if (!johnsNext.equals(johns)) {
            assertThat(service.verify(JOHN, johnsNext)).as("next hour's code").isFalse();
        }
        if (!service.codes(JANE).getFirst().code().equals(johns)) {
            assertThat(service.verify(JANE, johns)).as("another customer's code").isFalse();
        }
    }

    @Test
    void rejectsMalformedCodes() {
        assertThat(service.verify(JOHN, null)).isFalse();
        assertThat(service.verify(JOHN, "")).isFalse();
        assertThat(service.verify(JOHN, "12345")).isFalse();
        assertThat(service.verify(JOHN, "1234567")).isFalse();
        assertThat(service.verify(JOHN, "12a456")).isFalse();
    }

    @Test
    void theNextHoursCodeBecomesCurrentWhenTheHourChanges() {
        String nextHour = service.codes(JOHN).get(1).code();
        clock.set("2026-03-10T14:05:00");

        HourlyCode current = service.codes(JOHN).getFirst();
        assertThat(current.code()).isEqualTo(nextHour);
        assertThat(current.validFrom()).isEqualTo(LocalDateTime.parse("2026-03-10T14:00"));
        assertThat(service.verify(JOHN, nextHour)).isTrue();
    }

    @Test
    void thePreviousCodeWorksOnlyDuringTheGracePeriod() {
        String code = service.codes(JOHN).getFirst().code();
        String nextHour = service.codes(JOHN).get(1).code();
        // Only meaningful when the two random codes differ
        if (code.equals(nextHour)) {
            return;
        }

        clock.set("2026-03-10T14:01:00");
        assertThat(service.verify(JOHN, code)).as("one minute into the next hour").isTrue();

        clock.set("2026-03-10T14:02:00");
        assertThat(service.verify(JOHN, code)).as("after the grace period").isFalse();
    }
}
