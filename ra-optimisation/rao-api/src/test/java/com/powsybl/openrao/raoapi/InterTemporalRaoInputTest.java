/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class InterTemporalRaoInputTest {

    private OffsetDateTime timestamp1;
    private OffsetDateTime timestamp2;
    private OffsetDateTime timestamp3;
    private TemporalData<RaoInput> temporalData;

    @BeforeEach
    void setUp() {
        RaoInput raoInput1 = Mockito.mock(RaoInput.class);
        RaoInput raoInput2 = Mockito.mock(RaoInput.class);
        RaoInput raoInput3 = Mockito.mock(RaoInput.class);
        timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
        timestamp2 = OffsetDateTime.of(2024, 12, 10, 17, 21, 0, 0, ZoneOffset.UTC);
        timestamp3 = OffsetDateTime.of(2024, 12, 10, 18, 21, 0, 0, ZoneOffset.UTC);
        temporalData = new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3));
    }

    @Test
    void testInstantiateInterTemporalRaoInput() {
        InterTemporalRaoInput input = new InterTemporalRaoInput(temporalData, Set.of(timestamp1, timestamp3));
        assertEquals(temporalData, input.getRaoInputs());
        assertEquals(Set.of(timestamp1, timestamp3), input.getTimestampsToRun());
    }

    @Test
    void testInstantiateInterTemporalRaoInputAllTimestamps() {
        InterTemporalRaoInput input = new InterTemporalRaoInput(temporalData);
        assertEquals(temporalData, input.getRaoInputs());
        assertEquals(Set.of(timestamp1, timestamp2, timestamp3), input.getTimestampsToRun());
    }
}
