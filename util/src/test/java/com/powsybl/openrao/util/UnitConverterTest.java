/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class UnitConverterTest {

    @Test
    void testGetFlowUnitMultiplier() {
        double nominalVoltage = 400; // kV

        // Test MW -> A
        assertEquals(1.44, UnitConverter.getFlowUnitMultiplier(nominalVoltage, Unit.MEGAWATT, Unit.AMPERE), 1e-2);

        // Test A -> MW
        assertEquals(0.69, UnitConverter.getFlowUnitMultiplier(nominalVoltage, Unit.AMPERE, Unit.MEGAWATT), 1e-2);

        // Test unsupported conversion
        Exception exception = assertThrows(OpenRaoException.class, () ->
            UnitConverter.getFlowUnitMultiplier(nominalVoltage, Unit.MEGAWATT, Unit.KILOVOLT));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
    }

    @Test
    void testConvertAToPercentImax() {
        double valueInA = 500;
        double fmax = 1000;

        assertEquals(0.5, UnitConverter.convertAToPercentImax(valueInA, fmax));
    }

    @Test
    void testConvertPercentImaxToA() {
        double valueInPercent = 0.5;
        double fmax = 1000;

        assertEquals(500, UnitConverter.convertPercentImaxToA(valueInPercent, fmax));
    }

    @Test
    void testConvertMWToA() {
        double valueInMW = 100;
        double nominalVoltage = 400; // kV

        assertEquals(144.3375673, UnitConverter.convertMWToA(valueInMW, nominalVoltage), 1e-7);
    }

    @Test
    void testConvertAToMW() {
        double valueInA = 144.3375673;
        double nominalVoltage = 400; // kV

        assertEquals(100, UnitConverter.convertAToMW(valueInA, nominalVoltage), 1e-7);
    }
}
