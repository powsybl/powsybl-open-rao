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
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        VoltageResult emptyVoltageResult = VoltageResult.builder().build();
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

        VoltageResult.VoltageResultBuilder voltageResultBuilder = VoltageResult.builder();

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

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> VoltageResult.builder().addMeasurement(410.0, 420.0, null, voltageCnec, Unit.MEGAWATT));
        assertEquals("VoltageCNEC results are only allowed for kilovolts.", exception.getMessage());
    }

    private static VoltageCnec mockVoltageCnec(Instant instant) {
        VoltageCnec voltageCnec = Mockito.mock(VoltageCnec.class);
        State state = Mockito.mock(State.class);
        Mockito.when(state.getInstant()).thenReturn(instant);
        Mockito.when(voltageCnec.getState()).thenReturn(state);
        Mockito.when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(440.0));
        Mockito.when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.0));
        return voltageCnec;
    }

    @Test
    void testIsSecureWithoutResults() {
        assertTrue(VoltageResult.build().build().isSecure());
    }

    @Test
    void testIsSecure() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        Instant curativeInstant = Mockito.mock(Instant.class);
        VoltageCnec preventiveCnec = mockVoltageCnec(preventiveInstant);
        VoltageCnec curativeCnec = mockVoltageCnec(curativeInstant);

        // only the margin at the CNEC's own instant matters (other instants are negative)
        VoltageResult voltageResult = VoltageResult.build()
            .addMeasurement(370.0, 400.0, null, preventiveCnec, Unit.KILOVOLT)
            .addMeasurement(390.0, 430.0, preventiveInstant, preventiveCnec, Unit.KILOVOLT)
            .addMeasurement(400.0, 450.0, preventiveInstant, curativeCnec, Unit.KILOVOLT)
            .addMeasurement(400.0, 420.0, curativeInstant, curativeCnec, Unit.KILOVOLT)
            .build();
        assertTrue(voltageResult.isSecure());
    }

    @Test
    void testIsNotSecureWithUnderVoltage() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        VoltageCnec secureCnec = mockVoltageCnec(preventiveInstant);
        VoltageCnec unsecureCnec = mockVoltageCnec(preventiveInstant);

        VoltageResult voltageResult = VoltageResult.build()
            .addMeasurement(400.0, 420.0, preventiveInstant, secureCnec, Unit.KILOVOLT)
            .addMeasurement(375.0, 420.0, preventiveInstant, unsecureCnec, Unit.KILOVOLT)
            .build();
        assertFalse(voltageResult.isSecure());
    }

    @Test
    void testIsNotSecureWithOverVoltage() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        VoltageCnec voltageCnec = mockVoltageCnec(preventiveInstant);

        VoltageResult voltageResult = VoltageResult.build().addMeasurement(400.0, 445.0, preventiveInstant, voltageCnec, Unit.KILOVOLT).build();
        assertFalse(voltageResult.isSecure());
    }

    @Test
    void testIstSecureWithNullMargin() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        VoltageCnec voltageCnec = mockVoltageCnec(preventiveInstant);

        VoltageResult voltageResult = VoltageResult.build().addMeasurement(380.0, 440.0, preventiveInstant, voltageCnec, Unit.KILOVOLT).build();
        assertTrue(voltageResult.isSecure());
    }
}
