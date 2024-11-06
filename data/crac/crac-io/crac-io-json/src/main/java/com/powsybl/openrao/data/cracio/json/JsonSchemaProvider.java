/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.powsybl.openrao.commons.OpenRaoException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class JsonSchemaProvider {
    private JsonSchemaProvider() {
    }

    private static final String SCHEMAS_DIRECTORY = "schemas/crac/";
    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private static final SchemaValidatorsConfig CONFIG = SchemaValidatorsConfig.builder().locale(Locale.UK).build();
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

    public static boolean validateJsonCrac(JsonSchema schema, InputStream cracInputStream) throws IOException {
        return getValidationErrors(schema, cracInputStream).isEmpty();
    }

    public static List<String> getValidationErrors(JsonSchema schema, InputStream cracInputStream) throws IOException {
        return schema.validate(MAPPER.readTree(cracInputStream)).stream().map(ValidationMessage::getMessage).toList();
    }

    public static JsonSchema getSchema(String schemaName) {
        return SCHEMA_FACTORY.getSchema(JsonSchemaProvider.class.getResourceAsStream("/" + SCHEMAS_DIRECTORY + schemaName), CONFIG);
    }

    public static Pair<Integer, Integer> getCracVersionFromSchema(String schemaName) {
        Pattern pattern = Pattern.compile("^crac-v([1-9]\\d*)\\.(\\d+)\\.json$");
        Matcher matcher = pattern.matcher(schemaName);
        return matcher.matches() ? Pair.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))) : null;
    }

    public static List<String> getAllSchemaFiles() {
        try (Stream<Path> files = Files.walk(Paths.get(Objects.requireNonNull(JsonSchemaProvider.class.getClassLoader().getResource(SCHEMAS_DIRECTORY)).toURI()))) {
            return files.filter(path -> !Files.isDirectory(path))
                .map(Path::getFileName)
                .map(Path::toString)
                .sorted(JsonSchemaProvider::reverseCompareStrings)
                .toList();
        } catch (IOException | URISyntaxException e) {
            throw new OpenRaoException("Could not fetch JSON CRAC schema files. Reason: %s".formatted(e.getMessage()));
        }
    }

    private static int reverseCompareStrings(String s1, String s2) {
        return s2.compareTo(s1);
    }
}
