/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import org.junit.Before;
import org.junit.Test;

import static com.farao_community.farao.commons.Unit.KILOVOLT;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguino at rte-france.com>}
 */
public class VoltageThresholdTest {

    private static final double DOUBLE_TOL = 0.5;

    private VoltageThreshold voltageThreshold;

    @Before
    public void setUp() {
        voltageThreshold = new VoltageThreshold(380.0, 425.0);
    }

    @Test
    public void getMinMaxValue() {
        assertTrue(voltageThreshold.getMaxValue().isPresent());
        assertEquals(425.0, voltageThreshold.getMaxValue().getAsDouble(), 0.1);
        assertTrue(voltageThreshold.getMinValue().isPresent());
        assertEquals(380.0, voltageThreshold.getMinValue().getAsDouble(), 0.1);
    }

    @Test
    public void getMinMaxThreshold() {
        // for now, method always returns empty
        assertFalse(voltageThreshold.getMinThreshold(KILOVOLT).isPresent());
        assertFalse(voltageThreshold.getMaxThreshold(KILOVOLT).isPresent());
    }

    @Test
    public void getMinMaxThresholdWithUnit() {
        // for now, method always returns empty
        assertFalse(voltageThreshold.getMinThreshold(KILOVOLT).isPresent());
        assertFalse(voltageThreshold.getMaxThreshold(KILOVOLT).isPresent());
    }

    @Test
    public void getMinMaxThresholdWithUnauthorizedUnit() {
        try {
            voltageThreshold.getMaxThreshold(Unit.AMPERE);
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("KILOVOLT"));
        }

        try {
            voltageThreshold.getMinThreshold(Unit.MEGAWATT);
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("KILOVOLT"));
        }
    }
}
