package ca.bc.gov.open.digitalformsapi.viirp.utils.vips;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeDateUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "America/Vancouver, 2025-12-15T08:00:00Z, 2025-12-15T00:00:00.000-08:00",
            "America/Vancouver, 2026-03-08T09:59:59.123Z, 2026-03-08T01:59:59.123-08:00",
            "America/Vancouver, 2026-03-08T10:00:00Z, 2026-03-08T03:00:00.000-07:00",
            "America/Vancouver, 2026-11-05T07:00:00Z, 2026-11-05T00:00:00.000-07:00",
            "America/Vancouver, 2027-01-15T07:00:00Z, 2027-01-15T00:00:00.000-07:00"
    })
    void formatsSearchDatesWithMillisecondsInTheRuntimeZone(String zone, String instant, String expected) {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            assertEquals(expected, TimeDateUtils.getISO8601FromDate(Date.from(Instant.parse(instant))));
        } finally {
            TimeZone.setDefault(previous);
        }
    }
}
