/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.ucte;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class HvdcRangeActionHelperTest {

    @Test
    void testAddWithInitialSetpointFromNetwork() {
        Network network = Mockito.mock(Network.class);
        HvdcLine hvdcLine = Mockito.mock(HvdcLine.class);
        when(network.getHvdcLine("hvdc")).thenReturn(hvdcLine);
        when(hvdcLine.getActivePowerSetpoint()).thenReturn(50.0);
        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        assertEquals(50, HvdcRangeActionHelper.getCurrentSetpoint(network, "hvdc"));

        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        assertEquals(-50, HvdcRangeActionHelper.getCurrentSetpoint(network, "hvdc"));
    }
}
