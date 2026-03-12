
package com.powsybl.openrao.data.crac.io.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinimalCracParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinimalCracParser.class);

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

    private static final Map<String, List<Predicate<String>>> FIELDS = Map.of(
        "type",    List.of(isConst("CRAC")),
        "version", List.of(isTypeString(), isPattern(Pattern.compile("^[1-9]\\d*\\.\\d+$"))),
        "id",      List.of(isTypeString()),
        "name",    List.of(isTypeString())
    );
    private static final boolean ADDITIONAL_PROPERTIES = true;
    private static final Set<String> REQUIRED_FIELDS = Set.of("type", "version", "id", "name");

    private MinimalCracParser() {
      //empty
    }

    private static Predicate<String> isConst(String c) {
        return c::equals;
    }

    private static Predicate<String> isTypeString() {
        return v -> true;
    }

    private static Predicate<String> isPattern(Pattern pattern) {
        return v -> pattern.matcher(v).matches();
    }

    public static boolean parseMinimalCracFile(InputStream is) {
        try (JsonParser parser = MAPPER.createParser(is)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return false;
            }
            Set<String> found = new HashSet<>();
            JsonToken token;
            while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
                if (!validateToken(parser, token, found)) {
                    return false;
                }
            }
            if (hasFoundAll(found)) {
                return true;
            }
            LOGGER.debug("Required field missing. Found={}", found);
            return false;
        } catch (IOException e) {
            LOGGER.debug("Error parsing: {}", e.getMessage());
            return false;
        }
    }

    private static boolean validateToken(JsonParser parser, JsonToken token, Set<String> found) throws IOException {
        if (token == null) {
            LOGGER.debug("Unexpected EOF");
            return false;
        }
        String field = parser.currentName();
        parser.nextToken();
        if (isKnownField(field)) {
            var value = parser.getText();
            if (!validateKnownField(value, field, found)) {
                LOGGER.debug("Invalid value for field {}: {}", field, value);
                return false;
            }
        } else {
            if (ADDITIONAL_PROPERTIES) {
                parser.skipChildren();
            } else {
                LOGGER.debug("Unknown field: {}", field);
                return false;
            }
        }
        return true;
    }

    private static boolean hasFoundAll(Set<String> found) {
        return found.containsAll(REQUIRED_FIELDS);
    }

    private static boolean isKnownField(String field) {
        return FIELDS.containsKey(field);
    }

    private static boolean validateKnownField(String value, String field, Set<String> found) {
        if (isValidKnownField(field, value)) {
            found.add(field);
            return true;
        }
        return false;
    }

    private static boolean isValidKnownField(String field, String value) {
        var validators = FIELDS.get(field);
        if (validators == null) {
            throw new IllegalArgumentException("Not a known field: " + field);
        }
        if (REQUIRED_FIELDS.contains(field) && value == null) {
            return false;
        }
        return validators.stream().allMatch(v -> v.test(value));
    }

}
