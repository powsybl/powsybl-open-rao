/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class ElementaryVoltageCnecResultTest {

    @Test
    void defaultValuesTest() {
        Instant preventiveInstant = mock(Instant.class);
        VoltageCnecResult defaultVoltageCnecResult = new VoltageCnecResult();
        assertEquals(Double.NaN, defaultVoltageCnecResult.getResult(null).getMinVoltage(Unit.KILOVOLT), 1e-3);
        assertEquals(Double.NaN, defaultVoltageCnecResult.getResult(null).getMaxVoltage(Unit.KILOVOLT), 1e-3);
        assertEquals(Double.NaN, defaultVoltageCnecResult.getResult(preventiveInstant).getMargin(Unit.KILOVOLT), 1e-3);
    }

    @Test
    void testGetAndCreateIfAbsent() {
        VoltageCnecResult voltageCnecResult = new VoltageCnecResult();
        assertEquals(Double.NaN, voltageCnecResult.getResult(null).getMinVoltage(Unit.KILOVOLT));
        assertEquals(Double.NaN, voltageCnecResult.getResult(null).getMaxVoltage(Unit.KILOVOLT));

        voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(null).setMinVoltage(100.0, Unit.KILOVOLT);
        voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(null).setMaxVoltage(200.0, Unit.KILOVOLT);
        assertEquals(100., voltageCnecResult.getResult(null).getMinVoltage(Unit.KILOVOLT), 1e-3);
        assertEquals(200., voltageCnecResult.getResult(null).getMaxVoltage(Unit.KILOVOLT));

        voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(null).setMargin(150., Unit.KILOVOLT);
        assertEquals(150., voltageCnecResult.getResult(null).getMargin(Unit.KILOVOLT), 1e-3);
    }

    @Test
    void testWrongUnit() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new VoltageCnecResult().getResult(null).getMinVoltage(Unit.AMPERE));
        assertEquals("Voltage results are only available in KILOVOLT", exception.getMessage());
    }
}
