package ca.bc.gov.open.digitalformsapi.viirp.utils;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;

/**
 * Public API booleans accept only the JSON literals {@code true} and {@code false}; Jackson would
 * otherwise coerce strings such as {@code "true"} and numbers.
 */
public final class BooleanJson {

    public static final String MESSAGE = "Must be true or false.";

    private BooleanJson() {
    }

    public static JacksonModule publicModule() {
        return new SimpleModule("viirp-public-boolean").addDeserializer(Boolean.class, new StrictDeserializer());
    }

    /** The validation message for a boolean type, or {@code null} for any other type. */
    public static String message(Class<?> type) {
        return Boolean.class.equals(type) || boolean.class.equals(type) ? MESSAGE : null;
    }

    private static final class StrictDeserializer extends StdScalarDeserializer<Boolean> {

        StrictDeserializer() {
            super(Boolean.class);
        }

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctxt) {
            return switch (p.currentToken()) {
                case VALUE_TRUE -> Boolean.TRUE;
                case VALUE_FALSE -> Boolean.FALSE;
                default -> throw MismatchedInputException.from(p, Boolean.class, MESSAGE);
            };
        }
    }
}
