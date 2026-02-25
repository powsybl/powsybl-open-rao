/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GeneratorConstraintsTest {
    @Test
    void testBuildSimpleGeneratorConstraints() {
        GeneratorConstraints generatorConstraints = GeneratorConstraints.create()
            .withGeneratorId("generator")
            .withLeadTime(1.)
            .withLagTime(2.)
            .build();
        assertEquals("generator", generatorConstraints.getGeneratorId());
        assertEquals(Optional.of(1.), generatorConstraints.getLeadTime());
        assertEquals(Optional.of(2.), generatorConstraints.getLagTime());
        assertTrue(generatorConstraints.getUpwardPowerGradient().isEmpty());
        assertTrue(generatorConstraints.getDownwardPowerGradient().isEmpty());
    }

    @Test
    void testBuildComprehensiveGeneratorConstraints() {
        GeneratorConstraints generatorConstraints = GeneratorConstraints.create()
            .withGeneratorId("generator")
            .withLeadTime(1.)
            .withLagTime(2.)
            .withUpwardPowerGradient(50.)
            .withDownwardPowerGradient(-100.)
            .build();
        assertEquals("generator", generatorConstraints.getGeneratorId());
        assertEquals(Optional.of(1.), generatorConstraints.getLeadTime());
        assertEquals(Optional.of(2.), generatorConstraints.getLagTime());
        assertEquals(Optional.of(50.), generatorConstraints.getUpwardPowerGradient());
        assertEquals(Optional.of(-100.), generatorConstraints.getDownwardPowerGradient());
    }

    @Test
    void testBuildWithMissingId() {
        OpenRaoException exception = assertThrows(OpenRaoException.class,
            () -> GeneratorConstraints.create().withLeadTime(1.).withLagTime(1.).build()
        );
        assertEquals("The id of the generator is mandatory.", exception.getMessage());
    }

    @Test
    void testNegativeLeadTime() {
        OpenRaoException exception = assertThrows(OpenRaoException.class,
            () -> GeneratorConstraints.create().withGeneratorId("generator").withLeadTime(-1.).withLagTime(1.).build()
        );
        assertEquals("The lead time of the generator must be positive.", exception.getMessage());
    }

    @Test
    void testNegativeLagTime() {
        OpenRaoException exception = assertThrows(OpenRaoException.class,
            () -> GeneratorConstraints.create().withGeneratorId("generator").withLeadTime(1.).withLagTime(-1.).build()
        );
        assertEquals("The lag time of the generator must be positive.", exception.getMessage());
    }

    @Test
    void testNegativeUpwardPowerGradient() {
        OpenRaoException exception = assertThrows(OpenRaoException.class,
            () -> GeneratorConstraints.create().withGeneratorId("generator").withLeadTime(1.).withLagTime(1.).withUpwardPowerGradient(-100.).build()
        );
        assertEquals("The upward power gradient of the generator must be positive.", exception.getMessage());
    }

    @Test
    void testPositiveUpwardPowerGradient() {
        OpenRaoException exception = assertThrows(OpenRaoException.class,
            () -> GeneratorConstraints.create().withGeneratorId("generator").withLeadTime(1.).withLagTime(1.).withDownwardPowerGradient(100.).build()
        );
        assertEquals("The downward power gradient of the generator must be negative.", exception.getMessage());
    }
}
