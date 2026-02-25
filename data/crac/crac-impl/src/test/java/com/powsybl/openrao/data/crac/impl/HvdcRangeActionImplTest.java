/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.range.StandardRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeActionAdder;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class HvdcRangeActionImplTest {
    private HvdcRangeActionAdder hvdcRangeActionAdder;
    private Network network;
    private Network networkWithAngleDroop;
    private HvdcLine hvdcLine;

    @BeforeEach
    public void setUp() {
        Crac crac = new CracImplFactory().create("cracId")
            .newInstant("preventive", InstantKind.PREVENTIVE);
        network = NetworkImportsUtil.import16NodesNetworkWithHvdc();
        networkWithAngleDroop = NetworkImportsUtil.import16NodesNetworkWithAngleDroopHvdcs();
        String networkElementId = "BBE2AA11 FFR3AA11 1";

        hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("hvdc-range-action-id")
            .withName("hvdc-range-action-name")
            .withNetworkElement("BBE2AA11 FFR3AA11 1")
            .withOperator("operator")
            .newOnInstantUsageRule().withInstant("preventive").add();

        hvdcLine = network.getHvdcLine(networkElementId);
    }

    @Test
    void getInitialSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        assertEquals(0, hvdcRa.getCurrentSetpoint(network), 1e-6);
        assertEquals(0, hvdcRa.getCurrentSetpoint(networkWithAngleDroop), 1e-6);
    }

    @Test
    void testSetInitialSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
            .add();
        hvdcRa.setInitialSetpoint(10.0);
        assertEquals(10.0, hvdcRa.getInitialSetpoint(), 1e-6);
    }

    @Test
    void applyPositiveSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        hvdcRa.apply(network, 5);
        assertEquals(5, hvdcRa.getCurrentSetpoint(network), 1e-6);

        // Not allowed to change HVDC line's active setpoint if the line is in AC emulation mode.
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> hvdcRa.apply(networkWithAngleDroop, 6));
        assertEquals("Unable to set an active power setpoint for HVDC line BBE2AA11 FFR3AA11 1 because it is operating in AC Emulation mode.", exception.getMessage());

    }

    @Test
    void applyNegativeSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        hvdcRa.apply(network, -3);
        assertEquals(-3, hvdcRa.getCurrentSetpoint(network), 1e-6);

        // Not allowed to change HVDC line's active setpoint if the line is in AC emulation mode.
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> hvdcRa.apply(networkWithAngleDroop, -4));
        assertEquals("Unable to set an active power setpoint for HVDC line BBE2AA11 FFR3AA11 1 because it is operating in AC Emulation mode.", exception.getMessage());
    }

    @Test
    void getPositiveSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        hvdcRa.apply(network, 5);
        hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        assertEquals(5, hvdcRa.getCurrentSetpoint(network), 1e-6);
    }

    @Test
    void getNegativeSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        hvdcRa.apply(network, 3);
        hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        assertEquals(-3, hvdcRa.getCurrentSetpoint(network), 1e-6);
    }

    @Test
    void applyOnUnknownHvdc() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .withNetworkElement("unknownNetworkElement").add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> hvdcRa.apply(network, 50));
        assertEquals("HvdcLine unknownNetworkElement does not exist in the current network.", exception.getMessage());
    }

    @Test
    void hvdcWithoutSpecificRange() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> hvdcRangeActionAdder.add());
        assertEquals("Cannot add HvdcRangeAction without a range. Please use newRange()", exception.getMessage());
    }

    @Test
    void hvdcWithSpecificRange() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();

        assertEquals(-5, hvdcRa.getMinAdmissibleSetpoint(0), 1e-3);
        assertEquals(10, hvdcRa.getMaxAdmissibleSetpoint(0), 1e-3);
    }

    @Test
    void hvdcWithNoMin() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMax(10);
        OpenRaoException exception = assertThrows(OpenRaoException.class, standardRangeAdder::add);
        assertEquals("StandardRange min value was not defined.", exception.getMessage());
    }

    @Test
    void hvdcWithNoMax() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMin(10);
        OpenRaoException exception = assertThrows(OpenRaoException.class, standardRangeAdder::add);
        assertEquals("StandardRange max value was not defined.", exception.getMessage());
    }

    @Test
    void testGetLocation() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        Set<Country> countries = hvdcRa.getLocation(network);
        assertEquals(Set.of(Country.BE, Country.FR), countries);
    }

    @Test
    void hvdcEquals() {
        HvdcRangeAction hvdcRa1 = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        HvdcRangeAction hvdcRa2 = hvdcRangeActionAdder.withId("anotherId").newRange().withMin(-5).withMax(10).add()
                .add();

        assertEquals(hvdcRa1.hashCode(), hvdcRa1.hashCode());
        assertEquals(hvdcRa1, hvdcRa1);
        assertNotEquals(hvdcRa1.hashCode(), hvdcRa2.hashCode());
        assertNotEquals(hvdcRa1, hvdcRa2);
    }

}
