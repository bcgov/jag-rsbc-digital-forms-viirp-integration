package ca.bc.gov.open.digitalformsapi.viirp.utils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.regex.Pattern;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;

/**
 * JSON formats for {@code format: date} ({@link LocalDate}) and {@code format: date-time}
 * ({@link OffsetDateTime}) values.
 *
 * <p>The public API accepts strict RFC 3339 values. VIPS WS reads and writes every date and
 * timestamp as a millisecond timestamp with a numeric offset, so {@link #vipsModule()} converts
 * dates to midnight in the JVM default zone (the container's B.C. regional zone) and back.</p>
 */
public final class TemporalJson {

    public static final String DATE_MESSAGE = "Must be a date in YYYY-MM-DD format.";
    public static final String DATE_TIME_MESSAGE =
            "Must be an RFC 3339 date-time with seconds and Z or a numeric UTC offset.";

    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DATE_TIME = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?(Z|[+-]\\d{2}:\\d{2})");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX");

    private TemporalJson() {
    }

    /** Strict request parsing and response formatting for the public API. */
    public static JacksonModule publicModule() {
        return new SimpleModule("viirp-public-temporal")
                .addDeserializer(LocalDate.class, new Deserializer<>(LocalDate.class, DATE_MESSAGE, TemporalJson::date))
                .addDeserializer(OffsetDateTime.class,
                        new Deserializer<>(OffsetDateTime.class, DATE_TIME_MESSAGE, TemporalJson::dateTime))
                .addSerializer(LocalDate.class, new Serializer<>(LocalDate.class, DateTimeFormatter.ISO_LOCAL_DATE::format))
                .addSerializer(OffsetDateTime.class, new Serializer<>(OffsetDateTime.class, TIMESTAMP::format));
    }

    /** The VIPS WS wire format, used only by the VIPS WS client. */
    public static JacksonModule vipsModule() {
        return new SimpleModule("viirp-vips-temporal")
                .addDeserializer(LocalDate.class, new Deserializer<>(LocalDate.class, DATE_MESSAGE, TemporalJson::vipsDate))
                .addDeserializer(OffsetDateTime.class,
                        new Deserializer<>(OffsetDateTime.class, DATE_TIME_MESSAGE, TemporalJson::dateTime))
                .addSerializer(LocalDate.class, new Serializer<>(LocalDate.class,
                        date -> TIMESTAMP.format(date.atStartOfDay(ZoneId.systemDefault()))))
                .addSerializer(OffsetDateTime.class, new Serializer<>(OffsetDateTime.class, TIMESTAMP::format));
    }

    /** The validation message for a temporal type, or {@code null} for any other type. */
    public static String message(Class<?> type) {
        if (LocalDate.class.equals(type)) {
            return DATE_MESSAGE;
        }
        return OffsetDateTime.class.equals(type) ? DATE_TIME_MESSAGE : null;
    }

    private static LocalDate date(String text) {
        return DATE.matcher(text).matches() ? LocalDate.parse(text) : null;
    }

    private static OffsetDateTime dateTime(String text) {
        return DATE_TIME.matcher(text).matches() ? OffsetDateTime.parse(text) : null;
    }

    // VIPS WS writes dates as B.C. midnight. Its local date part is the stored Oracle date, even if
    // VIPS WS used a different offset than VI/IRP, so take it as written rather than converting zones.
    private static LocalDate vipsDate(String text) {
        OffsetDateTime timestamp = dateTime(text);
        return timestamp == null ? date(text) : timestamp.toLocalDate();
    }

    private static final class Deserializer<T> extends StdScalarDeserializer<T> {

        private final Class<T> type;
        private final String message;
        private final Function<String, T> parser;

        Deserializer(Class<T> type, String message, Function<String, T> parser) {
            super(type);
            this.type = type;
            this.message = message;
            this.parser = parser;
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) {
            if (!p.hasToken(JsonToken.VALUE_STRING)) {
                throw MismatchedInputException.from(p, type, message);
            }
            String text = p.getString();
            try {
                T value = parser.apply(text);
                if (value != null) {
                    return value;
                }
            } catch (DateTimeException e) {
                // Matches the pattern but is not a real date or time, e.g. 2026-02-29 or 24:00:00.
            }
            throw InvalidFormatException.from(p, message, text, type);
        }
    }

    private static final class Serializer<T> extends StdScalarSerializer<T> {

        private final Function<T, String> formatter;

        Serializer(Class<T> type, Function<T, String> formatter) {
            super(type);
            this.formatter = formatter;
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(formatter.apply(value));
        }
    }
}
