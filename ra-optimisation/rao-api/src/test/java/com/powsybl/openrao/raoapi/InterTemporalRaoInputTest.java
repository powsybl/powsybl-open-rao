/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.intertemporalconstraints.IntertemporalConstraints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class InterTemporalRaoInputTest {
    private OffsetDateTime timestamp1;
    private OffsetDateTime timestamp2;
    private OffsetDateTime timestamp3;
    private TemporalData<RaoInput> temporalData;
    private IntertemporalConstraints intertemporalConstraints;

    @BeforeEach
    void setUp() {
        RaoInput raoInput1 = Mockito.mock(RaoInput.class);
        RaoInput raoInput2 = Mockito.mock(RaoInput.class);
        RaoInput raoInput3 = Mockito.mock(RaoInput.class);
        timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
        timestamp2 = OffsetDateTime.of(2024, 12, 10, 17, 21, 0, 0, ZoneOffset.UTC);
        timestamp3 = OffsetDateTime.of(2024, 12, 10, 18, 21, 0, 0, ZoneOffset.UTC);
        temporalData = new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3));
        intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("generator-1").withLeadTime(0.0).withLagTime(0.0).withPMin(400.0).withPMax(1000.0).withUpwardPowerGradient(200.0).build());
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("generator-2").withLeadTime(0.0).withLagTime(0.0).withPMin(400.0).withPMax(1000.0).withDownwardPowerGradient(-50.0).build());
    }

    @Test
    void testInstantiateInterTemporalRaoInput() {
        InterTemporalRaoInput input = new InterTemporalRaoInput(temporalData, Set.of(timestamp1, timestamp3), intertemporalConstraints);
        assertEquals(temporalData, input.getRaoInputs());
        assertEquals(Set.of(timestamp1, timestamp3), input.getTimestampsToRun());
        assertEquals(intertemporalConstraints.getGeneratorConstraints(), input.getIntertemporalConstraints().getGeneratorConstraints());
    }

    @Test
    void testInstantiateInterTemporalRaoInputAllTimestamps() {
        InterTemporalRaoInput input = new InterTemporalRaoInput(temporalData, intertemporalConstraints);
        assertEquals(temporalData, input.getRaoInputs());
        assertEquals(Set.of(timestamp1, timestamp2, timestamp3), input.getTimestampsToRun());
        assertEquals(intertemporalConstraints.getGeneratorConstraints(), input.getIntertemporalConstraints().getGeneratorConstraints());
    }

    @Test
    void testInstantiateWithMissingTimestamp() {
        final Set<OffsetDateTime> timestampsToRun = Set.of(OffsetDateTime.of(2024, 12, 11, 14, 29, 0, 0, ZoneOffset.UTC));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new InterTemporalRaoInput(temporalData, timestampsToRun, new IntertemporalConstraints()));
        assertEquals("Timestamp(s) '2024-12-11T14:29Z' are not defined in the inputs.", exception.getMessage());
    }

    @Test
    void testInstantiateWithMissingTimestamps() {
        final Set<OffsetDateTime> timestampsToRun = Set.of(OffsetDateTime.of(2024, 12, 11, 14, 29, 0, 0, ZoneOffset.UTC), OffsetDateTime.of(2024, 11, 11, 14, 29, 0, 0, ZoneOffset.UTC));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new InterTemporalRaoInput(temporalData, timestampsToRun, new IntertemporalConstraints()));
        assertEquals("Timestamp(s) '2024-11-11T14:29Z', '2024-12-11T14:29Z' are not defined in the inputs.", exception.getMessage());
    }
}
