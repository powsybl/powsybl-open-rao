/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json;

import com.networknt.schema.JsonSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.getAllSchemaFiles;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.getCracVersionFromSchema;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.getSchema;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.getValidationErrors;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.validateJsonCrac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonSchemaProviderTest {
    @Test
    void testGetAllSchemaFiles() {
        assertEquals(
            List.of(
                "crac-v2.5.json",
                "crac-v2.4.json",
                "crac-v2.3.json",
                "crac-v2.2.json",
                "crac-v2.1.json",
                "crac-v2.0.json",
                "crac-v1.9.json",
                "crac-v1.8.json",
                "crac-v1.7.json",
                "crac-v1.6.json",
                "crac-v1.5.json",
                "crac-v1.4.json",
                "crac-v1.3.json",
                "crac-v1.2.json",
                "crac-v1.1.json",
                "crac-v1.0.json"
            ),
            getAllSchemaFiles()
        );
    }

    @Test
    void testJsonValidationErrorMessages() throws IOException {
        JsonSchema jsonSchema = getSchema("crac-v2.5.json");
        List<String> validationErrors = getValidationErrors(jsonSchema, JsonSchemaProviderTest.class.getResourceAsStream("/cracWithErrors.json"));
        assertEquals(List.of(
            "/instants/3/kind: does not have a value in the enumeration [\"PREVENTIVE\", \"OUTAGE\", \"AUTO\", \"CURATIVE\"]",
            "/contingencies/1/networkElementsIds/0: integer found, string expected",
            "/contingencies/1/networkElementsIds/1: integer found, string expected",
            "/contingencies/2: required property 'networkElementsIds' not found"), validationErrors);
    }

    @Test
    void validateCrac1Point0() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.0.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.0.json")));
    }

    @Test
    void validateCrac1Point1() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.1.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.1.json")));
    }

    @Test
    void validateCrac1Point2() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.2.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.2.json")));
    }

    @Test
    void validateCrac1Point3() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.3.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.3.json")));
    }

    @Test
    void validateCrac1Point4() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.4.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.4.json")));
    }

    @Test
    void validateCrac1Point5() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.5.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.5.json")));
    }

    @Test
    void validateCrac1Point6() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.6.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.6.json")));
    }

    @Test
    void validateCrac1Point7() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.7.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.7.json")));
    }

    @Test
    void validateCrac1Point8() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.8.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.8.json")));
    }

    @Test
    void validateCrac1Point9() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v1.9.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.9.json")));
    }

    @Test
    void validateCrac2Point0() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v2.0.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.0.json")));
    }

    @Test
    void validateCrac2Point1() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v2.1.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.1.json")));
    }

    @Test
    void validateCrac2Point2() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v2.2.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.2.json")));
    }

    @Test
    void validateCrac2Point3() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v2.3.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.3.json")));
    }

    @Test
    void validateCrac2Point4() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v2.4.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.4.json")));
    }

    @Test
    void validateCrac2Point5() throws IOException {
        assertTrue(validateJsonCrac(getSchema("crac-v2.5.json"), JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.5.json")));
    }

    @Test
    void testGetCracVersionFromSchema() {
        assertEquals(Pair.of(1, 0), getCracVersionFromSchema("crac-v1.0.json"));
        assertEquals(Pair.of(1, 1), getCracVersionFromSchema("crac-v1.1.json"));
        assertEquals(Pair.of(1, 2), getCracVersionFromSchema("crac-v1.2.json"));
        assertEquals(Pair.of(1, 3), getCracVersionFromSchema("crac-v1.3.json"));
        assertEquals(Pair.of(1, 4), getCracVersionFromSchema("crac-v1.4.json"));
        assertEquals(Pair.of(1, 5), getCracVersionFromSchema("crac-v1.5.json"));
        assertEquals(Pair.of(1, 6), getCracVersionFromSchema("crac-v1.6.json"));
        assertEquals(Pair.of(1, 7), getCracVersionFromSchema("crac-v1.7.json"));
        assertEquals(Pair.of(1, 8), getCracVersionFromSchema("crac-v1.8.json"));
        assertEquals(Pair.of(1, 9), getCracVersionFromSchema("crac-v1.9.json"));
        assertEquals(Pair.of(2, 0), getCracVersionFromSchema("crac-v2.0.json"));
        assertEquals(Pair.of(2, 1), getCracVersionFromSchema("crac-v2.1.json"));
        assertEquals(Pair.of(2, 2), getCracVersionFromSchema("crac-v2.2.json"));
        assertEquals(Pair.of(2, 3), getCracVersionFromSchema("crac-v2.3.json"));
        assertEquals(Pair.of(2, 4), getCracVersionFromSchema("crac-v2.4.json"));
        assertEquals(Pair.of(2, 5), getCracVersionFromSchema("crac-v2.5.json"));
        assertNull(getCracVersionFromSchema("crac-v0.0.json"));
    }
}
