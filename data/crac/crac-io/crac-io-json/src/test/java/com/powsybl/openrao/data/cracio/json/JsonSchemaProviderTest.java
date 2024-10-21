/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json;

import com.powsybl.openrao.commons.OpenRaoException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.getCracVersion;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.getValidationErrors;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.validateJsonCrac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonSchemaProviderTest {
    @Test
    void missingVersion() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/invalidCrac.json"), 0, 0));
        assertEquals("No JSON Schema found for CRAC v0.0.", exception.getMessage());
    }

    @Test
    void testJsonValidationErrorMessages() throws IOException {
        Set<String> validationErrors = getValidationErrors(JsonSchemaProviderTest.class.getResourceAsStream("/cracWithErrors.json"), 2, 5);
        assertEquals(Set.of(
            "$.instants[3].kind: does not have a value in the enumeration [PREVENTIVE, OUTAGE, AUTO, CURATIVE]",
            "$.contingencies[2].networkElementsIds: is missing but it is required",
            "$.contingencies[1].networkElementsIds[0]: integer found, string expected",
            "$.contingencies[1].networkElementsIds[1]: integer found, string expected"), validationErrors);
    }

    @Test
    void validateCrac1Point0() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.0.json"), 1, 0));
    }

    @Test
    void validateCrac1Point1() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.1.json"), 1, 1));
    }

    @Test
    void validateCrac1Point2() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.2.json"), 1, 2));
    }

    @Test
    void validateCrac1Point3() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.3.json"), 1, 3));
    }

    @Test
    void validateCrac1Point4() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.4.json"), 1, 4));
    }

    @Test
    void validateCrac1Point5() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.5.json"), 1, 5));
    }

    @Test
    void validateCrac1Point6() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.6.json"), 1, 6));
    }

    @Test
    void validateCrac1Point7() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.7.json"), 1, 7));
    }

    @Test
    void validateCrac1Point8() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.8.json"), 1, 8));
    }

    @Test
    void validateCrac1Point9() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.9.json"), 1, 9));
    }

    @Test
    void validateCrac2Point0() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.0.json"), 2, 0));
    }

    @Test
    void validateCrac2Point1() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.1.json"), 2, 1));
    }

    @Test
    void validateCrac2Point2() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.2.json"), 2, 2));
    }

    @Test
    void validateCrac2Point3() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.3.json"), 2, 3));
    }

    @Test
    void validateCrac2Point4() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.4.json"), 2, 4));
    }

    @Test
    void validateCrac2Point5() throws IOException {
        assertTrue(validateJsonCrac(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.5.json"), 2, 5));
    }

    @Test
    void testGetCracVersion() throws IOException {
        assertNull(getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/invalidCrac.json"))));
        assertEquals(Pair.of(1, 0), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.0.json"))));
        assertEquals(Pair.of(1, 1), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.1.json"))));
        assertEquals(Pair.of(1, 2), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.2.json"))));
        assertEquals(Pair.of(1, 3), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.3.json"))));
        assertEquals(Pair.of(1, 4), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.4.json"))));
        assertEquals(Pair.of(1, 5), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.5.json"))));
        assertEquals(Pair.of(1, 6), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.6.json"))));
        assertEquals(Pair.of(1, 7), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.7.json"))));
        assertEquals(Pair.of(1, 8), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.8.json"))));
        assertEquals(Pair.of(1, 9), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v1/crac-v1.9.json"))));
        assertEquals(Pair.of(2, 0), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.0.json"))));
        assertEquals(Pair.of(2, 1), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.1.json"))));
        assertEquals(Pair.of(2, 2), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.2.json"))));
        assertEquals(Pair.of(2, 3), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.3.json"))));
        assertEquals(Pair.of(2, 4), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.4.json"))));
        assertEquals(Pair.of(2, 5), getCracVersion(Objects.requireNonNull(JsonSchemaProviderTest.class.getResourceAsStream("/retrocompatibility/v2/crac-v2.5.json"))));
    }
}
