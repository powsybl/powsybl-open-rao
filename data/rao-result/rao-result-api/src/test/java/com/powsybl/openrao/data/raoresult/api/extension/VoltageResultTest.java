/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class VoltageResultTest {
    @Test
    void testVoltageExtension() {
        VoltageCnec voltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(440.0));
        Mockito.when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getOrder()).thenReturn(0);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.getOrder()).thenReturn(1);

        VoltageResult emptyVoltageResult = VoltageResult.build().build();
        assertEquals("voltage-results", emptyVoltageResult.getName());

        // initial results

        assertEquals(Double.NaN, emptyVoltageResult.getMinVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, emptyVoltageResult.getMaxVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, emptyVoltageResult.getMargin(null, voltageCnec, Unit.KILOVOLT));

        assertEquals(Double.NaN, emptyVoltageResult.getMinVoltage(preventiveInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, emptyVoltageResult.getMaxVoltage(preventiveInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, emptyVoltageResult.getMargin(preventiveInstant, voltageCnec, Unit.KILOVOLT));

        assertEquals(Double.NaN, emptyVoltageResult.getMinVoltage(curativeInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, emptyVoltageResult.getMaxVoltage(curativeInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, emptyVoltageResult.getMargin(curativeInstant, voltageCnec, Unit.KILOVOLT));

        // manually add results

        VoltageResult.VoltageResultBuilder voltageResultBuilder = VoltageResult.build();

        voltageResultBuilder.addMeasurement(400.0, 405.0, null, voltageCnec, Unit.KILOVOLT);
        voltageResultBuilder.addMeasurement(417.0, 437.0, preventiveInstant, voltageCnec, Unit.KILOVOLT);
        voltageResultBuilder.addMeasurement(370.0, 461.0, curativeInstant, voltageCnec, Unit.KILOVOLT);

        VoltageResult voltageResult = voltageResultBuilder.build();

        assertEquals(400.0, voltageResult.getMinVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(405.0, voltageResult.getMaxVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(20.0, voltageResult.getMargin(null, voltageCnec, Unit.KILOVOLT));

        assertEquals(417.0, voltageResult.getMinVoltage(preventiveInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(437.0, voltageResult.getMaxVoltage(preventiveInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(3.0, voltageResult.getMargin(preventiveInstant, voltageCnec, Unit.KILOVOLT));

        assertEquals(370.0, voltageResult.getMinVoltage(curativeInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(461.0, voltageResult.getMaxVoltage(curativeInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(-21.0, voltageResult.getMargin(curativeInstant, voltageCnec, Unit.KILOVOLT));

        // invalid unit

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> VoltageResult.build().addMeasurement(410.0, 420.0, null, voltageCnec, Unit.MEGAWATT));
        assertEquals("VoltageCNEC results are only allowed for kilovolts.", exception.getMessage());
    }
}
