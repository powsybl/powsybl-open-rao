/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import org.junit.Before;
import org.junit.Test;

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
    public void getMinMaxThreshold() throws SynchronizationException {
        // for now, method always returns empty
        assertFalse(voltageThreshold.getMinThreshold().isPresent());
        assertFalse(voltageThreshold.getMaxThreshold().isPresent());
    }

    @Test
    public void getMinMaxThresholdWithUnit() throws SynchronizationException {
        // for now, method always returns empty
        assertFalse(voltageThreshold.getMinThreshold(Unit.KILOVOLT).isPresent());
        assertFalse(voltageThreshold.getMaxThreshold(Unit.KILOVOLT).isPresent());
    }

    @Test
    public void getMinMaxThresholdWithUnauthorizedUnit() throws SynchronizationException {
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
