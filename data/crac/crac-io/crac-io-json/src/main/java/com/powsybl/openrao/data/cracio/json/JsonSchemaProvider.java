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
import com.networknt.schema.SpecVersion;
import com.powsybl.openrao.commons.OpenRaoException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class JsonSchemaProvider {
    private JsonSchemaProvider() {
    }

    private static final String SCHEMA_FILE_BASE_PATH = "/schemas/crac/crac-v%s.%s.json";
    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

    public static boolean validateJsonCrac(String cracFile, int majorVersion, int minorVersion) throws IOException {
        return getJsonCracSchema(majorVersion, minorVersion).validate(MAPPER.readTree(JsonSchemaProvider.class.getResourceAsStream(cracFile))).isEmpty();
    }

    private static JsonSchema getJsonCracSchema(int majorVersion, int minorVersion) {
        InputStream schemaInputStream = JsonSchemaProvider.class.getResourceAsStream(SCHEMA_FILE_BASE_PATH.formatted(majorVersion, minorVersion));
        if (schemaInputStream == null) {
            throw new OpenRaoException("No JSON Schema found for CRAC v%s.%s.".formatted(majorVersion, minorVersion));
        }
        return SCHEMA_FACTORY.getSchema(schemaInputStream);
    }
}
