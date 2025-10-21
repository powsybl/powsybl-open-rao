/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.iidm;

import com.powsybl.iidm.network.HvdcConverterStation;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper.computeFlowOnHvdcLine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class IidmHvdcHelperTest {

    @Test
    void testAddWithInitialSetpointFromNetwork() {
        Network network = Mockito.mock(Network.class);
        HvdcLine hvdcLine = Mockito.mock(HvdcLine.class);
        when(network.getHvdcLine("hvdc")).thenReturn(hvdcLine);
        when(hvdcLine.getActivePowerSetpoint()).thenReturn(50.0);
        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        assertEquals(50, IidmHvdcHelper.getCurrentSetpoint(network, "hvdc"));

        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        assertEquals(-50, IidmHvdcHelper.getCurrentSetpoint(network, "hvdc"));
    }

    @Test
    void computeFlowOnHvdcLine() {
        // Mocks
        HvdcLine hvdcLine = Mockito.mock(HvdcLine.class);
        HvdcConverterStation station2 = Mockito.mock(HvdcConverterStation.class);
        Terminal terminal2 = Mockito.mock(Terminal.class);

        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        when(hvdcLine.getConverterStation2()).thenReturn(station2);
        when(station2.getTerminal()).thenReturn(terminal2);
        when(terminal2.getP()).thenReturn(123.45);

        double result = IidmHvdcHelper.computeFlowOnHvdcLine(hvdcLine);

        assertEquals(123.45, result, 1e-6);

        // Mocks
        HvdcConverterStation station1 = Mockito.mock(HvdcConverterStation.class);
        Terminal terminal1 = Mockito.mock(Terminal.class);

        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        when(hvdcLine.getConverterStation1()).thenReturn(station1);
        when(station1.getTerminal()).thenReturn(terminal1);
        when(terminal1.getP()).thenReturn(-55.0);

        result = IidmHvdcHelper.computeFlowOnHvdcLine(hvdcLine);

        assertEquals(-55.0, result, 1e-6);
    }

}
