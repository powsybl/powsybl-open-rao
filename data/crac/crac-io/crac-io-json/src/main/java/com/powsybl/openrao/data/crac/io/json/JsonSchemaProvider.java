/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import com.powsybl.openrao.commons.OpenRaoException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class JsonSchemaProvider {
    private JsonSchemaProvider() {
    }

    private static final String SCHEMAS_DIRECTORY = "/schemas/crac/";
    private static final String SCHEMAS_NAME_PATTERN = "crac-v%s.%s.json";
    private static final String MINIMUM_VIABLE_CRAC_SCHEMA = "minimum-viable-crac.json";
    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private static final SchemaValidatorsConfig CONFIG = SchemaValidatorsConfig.builder().locale(Locale.UK).build();
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

    public static List<String> getValidationErrors(JsonSchema schema, InputStream cracInputStream) throws IOException {
        return schema.validate(MAPPER.readTree(cracInputStream)).stream().map(ValidationMessage::getMessage).toList();
    }

    public static boolean isCracFile(InputStream cracInputStream) throws IOException {
        return getValidationErrors(getSchema(getSchemaAsStream(MINIMUM_VIABLE_CRAC_SCHEMA)), cracInputStream).isEmpty();
    }

    public static JsonSchema getSchema(Version version) {
        InputStream schemaInputStream = getSchemaAsStream(SCHEMAS_NAME_PATTERN.formatted(version.majorVersion(), version.minorVersion()));
        if (schemaInputStream == null) {
            throw new OpenRaoException("v%s.%s is not a valid JSON CRAC version.".formatted(version.majorVersion(), version.minorVersion()));
        }
        return getSchema(schemaInputStream);
    }

    private static InputStream getSchemaAsStream(String schemaName) {
        return JsonSchemaProvider.class.getResourceAsStream(SCHEMAS_DIRECTORY + schemaName);
    }

    private static JsonSchema getSchema(InputStream schemaInputStream) {
        return SCHEMA_FACTORY.getSchema(schemaInputStream, CONFIG);
    }
}
