/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.intertemporalconstraints.io.JsonIntertemporalConstraints;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonIntertemporalConstraintsTest {
    @Test
    void testSerialization() throws IOException {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();

        GeneratorConstraints generatorConstraints1 = GeneratorConstraints.create()
            .withGeneratorId("generator-1")
            .withLeadTime(1.15)
            .withLagTime(2.0)
            .withUpwardPowerGradient(100.0)
            .withDownwardPowerGradient(-50.0)
            .build();
        GeneratorConstraints generatorConstraints2 = GeneratorConstraints.create()
            .withGeneratorId("generator-2")
            .build();
        GeneratorConstraints generatorConstraints3 = GeneratorConstraints.create()
            .withGeneratorId("generator-3")
            .withLeadTime(0.5)
            .withLagTime(4.0)
            .withDownwardPowerGradient(-1000.0)
            .build();

        intertemporalConstraints.addGeneratorConstraints(generatorConstraints1);
        intertemporalConstraints.addGeneratorConstraints(generatorConstraints2);
        intertemporalConstraints.addGeneratorConstraints(generatorConstraints3);

        ByteArrayOutputStream expectedOutputStream = new ByteArrayOutputStream();
        Objects.requireNonNull(getClass().getResourceAsStream("/intertemporal-constraints.json")).transferTo(expectedOutputStream);

        ByteArrayOutputStream actualOutputStream = new ByteArrayOutputStream();
        JsonIntertemporalConstraints.write(intertemporalConstraints, actualOutputStream);

        assertJsonEquivalence(expectedOutputStream.toString(), actualOutputStream.toString());
    }

    @Test
    void testSerializationEmptyConstraints() throws IOException {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();

        ByteArrayOutputStream expectedOutputStream = new ByteArrayOutputStream();
        Objects.requireNonNull(getClass().getResourceAsStream("/empty-intertemporal-constraints.json")).transferTo(expectedOutputStream);

        ByteArrayOutputStream actualOutputStream = new ByteArrayOutputStream();
        JsonIntertemporalConstraints.write(intertemporalConstraints, actualOutputStream);

        assertJsonEquivalence(expectedOutputStream.toString(), actualOutputStream.toString());
    }

    @Test
    void testDeserialization() throws IOException {
        IntertemporalConstraints intertemporalConstraints = JsonIntertemporalConstraints.read(getClass().getResourceAsStream("/intertemporal-constraints.json"));
        List<GeneratorConstraints> generatorConstraints = intertemporalConstraints.getGeneratorConstraints().stream().sorted(Comparator.comparing(GeneratorConstraints::getGeneratorId)).toList();

        assertEquals(3, generatorConstraints.size());

        GeneratorConstraints generatorConstraints1 = generatorConstraints.get(0);
        assertEquals("generator-1", generatorConstraints1.getGeneratorId());
        assertEquals(Optional.of(1.15), generatorConstraints1.getLeadTime());
        assertEquals(Optional.of(2.0), generatorConstraints1.getLagTime());
        assertEquals(Optional.of(100.0), generatorConstraints1.getUpwardPowerGradient());
        assertEquals(Optional.of(-50.0), generatorConstraints1.getDownwardPowerGradient());

        GeneratorConstraints generatorConstraints2 = generatorConstraints.get(1);
        assertEquals("generator-2", generatorConstraints2.getGeneratorId());
        assertTrue(generatorConstraints2.getLeadTime().isEmpty());
        assertTrue(generatorConstraints2.getLagTime().isEmpty());
        assertTrue(generatorConstraints2.getUpwardPowerGradient().isEmpty());
        assertTrue(generatorConstraints2.getDownwardPowerGradient().isEmpty());

        GeneratorConstraints generatorConstraints3 = generatorConstraints.get(2);
        assertEquals("generator-3", generatorConstraints3.getGeneratorId());
        assertEquals(Optional.of(0.5), generatorConstraints3.getLeadTime());
        assertEquals(Optional.of(4.0), generatorConstraints3.getLagTime());
        assertTrue(generatorConstraints3.getUpwardPowerGradient().isEmpty());
        assertEquals(Optional.of(-1000.0), generatorConstraints3.getDownwardPowerGradient());
    }

    @Test
    void testDeserializationWithIllegalField() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> JsonIntertemporalConstraints.read(getClass().getResourceAsStream("/invalid-intertemporal-constraints.json")));
        assertEquals("Unexpected field 'unknownField' in JSON intertemporal constraints.", exception.getMessage());
    }

    @Test
    void testDeserializationWithIllegalFieldInGeneratorConstraints() {
        // error occurs during automatic deserialization of generator constraints and is reported as a JsonMappingException through a reference chain
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> JsonIntertemporalConstraints.read(getClass().getResourceAsStream("/intertemporal-constraints-with-invalid-generator-constraints.json")));
        assertEquals("Unexpected field 'unknownField' in JSON generator constraints.", exception.getMessage());
    }

    private static void assertJsonEquivalence(String expectedJson, String actualJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tree1 = mapper.readTree(expectedJson);
        JsonNode tree2 = mapper.readTree(actualJson);
        assertEquals(tree1, tree2);
    }
}
