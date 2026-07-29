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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class IidmHvdcHelperTest {

    @Test
    void testGetCurrentSetpoint() {

        Network network = Mockito.mock(Network.class);
        HvdcLine hvdcLine = Mockito.mock(HvdcLine.class);
        when(network.getHvdcLine("hvdc")).thenReturn(hvdcLine);
        when(hvdcLine.getActivePowerSetpoint()).thenReturn(50.0);
        // If the converter mode is set to SIDE_1_RECTIFIER_SIDE_2_INVERTER, the setpoint is positive
        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        assertEquals(50, IidmHvdcHelper.getCurrentSetpoint(network, "hvdc"));

        // If the converter mode is set to SIDE_1_INVERTER_SIDE_2_RECTIFIER, the setpoint is negative
        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        assertEquals(-50, IidmHvdcHelper.getCurrentSetpoint(network, "hvdc"));
    }

    @Test
    void computeActivePowerSetpointOnHvdcLine() {
        // Mocks
        HvdcLine hvdcLine = Mockito.mock(HvdcLine.class);
        HvdcConverterStation station1 = Mockito.mock(HvdcConverterStation.class);
        Terminal terminal1 = Mockito.mock(Terminal.class);

        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        when(hvdcLine.getConverterStation1()).thenReturn(station1);
        when(station1.getTerminal()).thenReturn(terminal1);
        when(terminal1.getP()).thenReturn(123.45);

        double result = IidmHvdcHelper.computeHvdcAngleDroopActivePowerControlSetPoint(hvdcLine);

        assertEquals(123.45, result, 1e-6);

        // No matter the converter mode we should read from converter station 1
        when(hvdcLine.getConvertersMode()).thenReturn(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);

        result = IidmHvdcHelper.computeHvdcAngleDroopActivePowerControlSetPoint(hvdcLine);

        assertEquals(123.45, result, 1e-6);
    }

    @Test
    void setActivePowerSetpointOnHvdcLine() {
        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        HvdcLine hvdcLine = network.getHvdcLine("BBE2AA11 FFR3AA11 1");
        // Positive setpoint => side 1 is the rectifier, side 2 is the inverter
        IidmHvdcHelper.setActivePowerSetpointOnHvdcLine(hvdcLine, 60.0);
        assertEquals(60.0, hvdcLine.getActivePowerSetpoint(), 60.0);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, hvdcLine.getConvertersMode());
        // Negative setpoint => side 1 is the inverter, side 2 is the rectifier
        IidmHvdcHelper.setActivePowerSetpointOnHvdcLine(hvdcLine, -60.0);
        assertEquals(60, hvdcLine.getActivePowerSetpoint());
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER, hvdcLine.getConvertersMode());
    }
}
