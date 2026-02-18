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
import com.powsybl.openrao.data.timecouplingconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.timecouplingconstraints.TimeCouplingConstraints;
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
class TimeCoupledRaoInputTest {
    private OffsetDateTime timestamp1;
    private OffsetDateTime timestamp2;
    private OffsetDateTime timestamp3;
    private TemporalData<RaoInput> temporalData;
    private TimeCouplingConstraints timeCouplingConstraints;

    @BeforeEach
    void setUp() {
        RaoInput raoInput1 = Mockito.mock(RaoInput.class);
        RaoInput raoInput2 = Mockito.mock(RaoInput.class);
        RaoInput raoInput3 = Mockito.mock(RaoInput.class);
        timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
        timestamp2 = OffsetDateTime.of(2024, 12, 10, 17, 21, 0, 0, ZoneOffset.UTC);
        timestamp3 = OffsetDateTime.of(2024, 12, 10, 18, 21, 0, 0, ZoneOffset.UTC);
        temporalData = new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3));
        timeCouplingConstraints = new TimeCouplingConstraints();
        timeCouplingConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("generator-1").withLeadTime(0.0).withLagTime(0.0).withUpwardPowerGradient(200.0).build());
        timeCouplingConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("generator-2").withLeadTime(0.0).withLagTime(0.0).withDownwardPowerGradient(-50.0).build());
    }

    @Test
    void testInstantiateTimeCoupledRaoInput() {
        TimeCoupledRaoInput input = new TimeCoupledRaoInput(temporalData, Set.of(timestamp1, timestamp3), timeCouplingConstraints);
        assertEquals(temporalData, input.getRaoInputs());
        assertEquals(Set.of(timestamp1, timestamp3), input.getTimestampsToRun());
        assertEquals(timeCouplingConstraints.getGeneratorConstraints(), input.getTimeCouplingConstraints().getGeneratorConstraints());
    }

    @Test
    void testInstantiateTimeCoupledRaoInputAllTimestamps() {
        TimeCoupledRaoInput input = new TimeCoupledRaoInput(temporalData, timeCouplingConstraints);
        assertEquals(temporalData, input.getRaoInputs());
        assertEquals(Set.of(timestamp1, timestamp2, timestamp3), input.getTimestampsToRun());
        assertEquals(timeCouplingConstraints.getGeneratorConstraints(), input.getTimeCouplingConstraints().getGeneratorConstraints());
    }

    @Test
    void testInstantiateWithMissingTimestamp() {
        final Set<OffsetDateTime> timestampsToRun = Set.of(OffsetDateTime.of(2024, 12, 11, 14, 29, 0, 0, ZoneOffset.UTC));
        final TimeCouplingConstraints constraints = new TimeCouplingConstraints();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new TimeCoupledRaoInput(temporalData, timestampsToRun, constraints));
        assertEquals("Timestamp(s) '2024-12-11T14:29Z' are not defined in the inputs.", exception.getMessage());
    }

    @Test
    void testInstantiateWithMissingTimestamps() {
        final Set<OffsetDateTime> timestampsToRun = Set.of(OffsetDateTime.of(2024, 12, 11, 14, 29, 0, 0, ZoneOffset.UTC), OffsetDateTime.of(2024, 11, 11, 14, 29, 0, 0, ZoneOffset.UTC));
        final TimeCouplingConstraints constraints = new TimeCouplingConstraints();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new TimeCoupledRaoInput(temporalData, timestampsToRun, constraints));
        assertEquals("Timestamp(s) '2024-11-11T14:29Z', '2024-12-11T14:29Z' are not defined in the inputs.", exception.getMessage());
    }
}
