/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonSchemaProviderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

    @Test
    void testJsonValidationErrorMessages() throws IOException {
        Schema jsonSchema = JsonSchemaProvider.getSchema(new Version(2, 5));
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
    @ValueSource(strings = {"v1.0", "v1.1", "v1.2", "v1.3", "v1.4", "v1.5", "v1.6", "v1.7", "v1.8", "v1.9", "v2.0", "v2.1", "v2.2",
        "v2.3", "v2.4", "v2.5", "v2.6", "v2.7", "v2.8", "v2.9", "v2.10", "v2.11", "v2.12"})
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

    private static Stream<String> versions() {
        return Stream.concat(IntStream.rangeClosed(0, 9).mapToObj(i -> "1." + i),
            IntStream.rangeClosed(0, 12).mapToObj(i -> "2." + i));
    }

    private static Version schemaVersion(String version) {
        String[] parts = version.split("\\.");
        return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private static ObjectNode fixture(String version) throws IOException {
        String resource = "/retrocompatibility/v%s/crac-v%s.json".formatted(version.substring(0, 1), version);
        try (InputStream input = JsonSchemaProviderTest.class.getResourceAsStream(resource)) {
            return (ObjectNode) MAPPER.readTree(input);
        }
    }

    private static List<String> errors(String version, ObjectNode crac) {
        return JsonSchemaProvider.getSchema(schemaVersion(version)).validate(crac).stream().map(Object::toString).toList();
    }

    private static void assertValid(String version, ObjectNode crac) {
        List<String> errors = errors(version, crac);
        Assertions.assertTrue(errors.isEmpty(), () -> version + ": " + errors);
    }

    private static void assertError(String version, ObjectNode crac, String location, String detail) {
        List<String> errors = errors(version, crac);
        Assertions.assertTrue(errors.stream().anyMatch(error -> error.contains(location) && error.contains(detail)),
            () -> version + ": expected " + location + " / " + detail + " in " + errors);
    }

    private static Stream<Arguments> cnecArrays() {
        return versions().flatMap(version -> Stream.of("flowCnecs", "angleCnecs", "voltageCnecs")
            .filter(array -> !version.startsWith("1.") || array.equals("flowCnecs")
                || Integer.parseInt(version.substring(2)) >= (array.equals("angleCnecs") ? 4 : 5))
            .map(array -> Arguments.of(version, array)));
    }

    @ParameterizedTest
    @MethodSource("cnecArrays")
    void cnecArrayElementsAreValidated(String version, String array) throws IOException {
        ObjectNode crac = fixture(version);
        assertValid(version, crac);
        ArrayNode cnecs = (ArrayNode) crac.get(array);
        int index = cnecs.size();
        cnecs.addObject();
        assertError(version, crac, "/" + array + "/" + index, "required property 'id'");
        cnecs.remove(index);
        cnecs.add(42);
        assertError(version, crac, "/" + array + "/" + index, "object expected");
    }

    private static Stream<String> angleVersions() {
        return versions().filter(version -> !version.startsWith("1.") || Integer.parseInt(version.substring(2)) >= 4);
    }

    @ParameterizedTest
    @MethodSource("angleVersions")
    void angleCnecRequiresImportingNetworkElement(String version) throws IOException {
        ObjectNode crac = fixture(version);
        assertValid(version, crac);
        ((ObjectNode) crac.withArray("angleCnecs").get(0)).remove("importingNetworkElementId");
        assertError(version, crac, "/angleCnecs/0", "required property 'importingNetworkElementId'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0", "1.1", "1.2", "1.3", "1.4", "1.5"})
    void legacyThresholdSidesAndRules(String version) throws IOException {
        ObjectNode crac = fixture(version);
        ObjectNode threshold = (ObjectNode) crac.withArray("flowCnecs").get(0).get("thresholds").get(0);
        for (String rule : List.of("onLeftSide", "onRightSide", "onHighVoltageLevel", "onLowVoltageLevel", "onRegulatedSide", "onNonRegulatedSide")) {
            threshold.remove("side");
            threshold.put("rule", rule);
            assertValid(version, crac);
        }
        threshold.remove("rule");
        for (String side : List.of("left", "right")) {
            threshold.put("side", side);
            assertValid(version, crac);
        }
        threshold.put("side", "invalid");
        assertError(version, crac, "/flowCnecs/0/thresholds/0/side", "enumeration");
        threshold.put("rule", "onLeftSide");
        assertError(version, crac, "/flowCnecs/0/thresholds/0/side", "enumeration");
        threshold.remove("side");
        threshold.put("rule", "invalid");
        assertError(version, crac, "/flowCnecs/0/thresholds/0/rule", "enumeration");
        threshold.remove("rule");
        assertError(version, crac, "/flowCnecs/0/thresholds/0", "required property");
    }

    private static Stream<Arguments> extensionLocations() {
        return IntStream.rangeClosed(0, 6).boxed().flatMap(minor -> Stream.of("", "flowCnecs", "pstRangeActions", "hvdcRangeActions", "injectionRangeActions", "networkActions")
            .filter(array -> minor >= 2 || !array.equals("injectionRangeActions"))
            .map(array -> Arguments.of("1." + minor, array)));
    }

    @ParameterizedTest
    @MethodSource("extensionLocations")
    void legacyExtensionsAcceptObjectsOnly(String version, String array) throws IOException {
        ObjectNode crac = fixture(version);
        ObjectNode owner = array.isEmpty() ? crac : (ObjectNode) crac.withArray(array).get(0);
        owner.putObject("extensions").putObject("custom").put("value", 1);
        assertValid(version, crac);
        owner.put("extensions", "invalid");
        String location = array.isEmpty() ? "/extensions" : "/" + array + "/0/extensions";
        assertError(version, crac, location, "object expected");
    }

    @ParameterizedTest
    @MethodSource("versions")
    void flowCnecExtensionsAcceptObjectsOnly(String version) throws IOException {
        ObjectNode crac = fixture(version);
        ObjectNode flowCnec = (ObjectNode) crac.withArray("flowCnecs").get(0);
        flowCnec.putObject("extensions").putArray("custom").add(1).add("value");
        assertValid(version, crac);
        flowCnec.putArray("extensions");
        assertError(version, crac, "/flowCnecs/0/extensions", "object expected");
    }

    @Test
    void removedTopLevelAndActionExtensionsAreRejected() throws IOException {
        ObjectNode crac = fixture("1.7");
        crac.putObject("extensions");
        assertError("1.7", crac, "extensions", "not defined");
        crac.remove("extensions");
        for (String array : List.of("pstRangeActions", "hvdcRangeActions", "injectionRangeActions", "networkActions")) {
            ObjectNode action = (ObjectNode) crac.withArray(array).get(0);
            action.putObject("extensions");
            assertError("1.7", crac, "/" + array + "/0", "extensions");
            action.remove("extensions");
        }
    }
}
