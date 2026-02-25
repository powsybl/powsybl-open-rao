/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json;

import com.networknt.schema.JsonSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonSchemaProviderTest {
    @Test
    void testJsonValidationErrorMessages() throws IOException {
        JsonSchema jsonSchema = JsonSchemaProvider.getSchema(new Version(2, 5));
        List<String> validationErrors = JsonSchemaProvider.getValidationErrors(jsonSchema, JsonSchemaProviderTest.class.getResourceAsStream("/cracWithErrors.json"));
        assertEquals(List.of(
            "/instants/3/kind: does not have a value in the enumeration [\"PREVENTIVE\", \"OUTAGE\", \"AUTO\", \"CURATIVE\"]",
            "/contingencies/1/networkElementsIds/0: integer found, string expected",
            "/contingencies/1/networkElementsIds/1: integer found, string expected",
            "/contingencies/2: required property 'networkElementsIds' not found"), validationErrors);
    }

    @Test
    void testMinimumViableCracFile() throws IOException {
        Assertions.assertTrue(JsonSchemaProvider.isCracFile(JsonSchemaProviderTest.class.getResourceAsStream("/cracHeader.json")));
    }

    @Test
    void testNonCracFile() throws IOException {
        Assertions.assertFalse(JsonSchemaProvider.isCracFile(JsonSchemaProviderTest.class.getResourceAsStream("/invalidCrac.json")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1.0", "v1.1", "v1.2", "v1.3", "v1.4", "v1.5", "v1.6", "v1.7", "v1.8", "v1.9", "v2.0", "v2.1", "v2.2", "v2.3", "v2.4", "v2.5"})
    void validateCrac(String version) throws IOException {
        String majorVersion = version.substring(1, 2);
        String minorVersion = version.substring(3);
        String cracFile = "/retrocompatibility/v%s/crac-v%s.%s.json".formatted(majorVersion, majorVersion, minorVersion);
        Assertions.assertTrue(JsonSchemaProvider.isCracFile(JsonSchemaProviderTest.class.getResourceAsStream(cracFile)));
        Assertions.assertTrue(
            JsonSchemaProvider.getValidationErrors(
                JsonSchemaProvider.getSchema(new Version(Integer.parseInt(majorVersion), Integer.parseInt(minorVersion))),
                JsonSchemaProviderTest.class.getResourceAsStream(cracFile)
            ).isEmpty());
    }
}
