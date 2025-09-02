/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnecAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class VoltageCnecImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
    }

    private VoltageCnecAdder initPreventiveCnecAdder() {
        return crac.newVoltageCnec()
            .withId("voltage-cnec")
            .withName("voltage-cnec-name")
            .withNetworkElement("networkElement")
            .withOperator("FR")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOptimized(false);
    }

    @Test
    void testGetLocation1() {

        Network network = NetworkImportsUtil.import12NodesNetwork();

        VoltageCnec cnec1 = crac.newVoltageCnec()
            .withId("cnec-1-id")
            .withNetworkElement("BBE1AA1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.).add()
            .add();

        VoltageCnec cnec2 = crac.newVoltageCnec()
            .withId("cnec-2-id")
            .withNetworkElement("DDE2AA1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.).add()
            .add();

        Set<Country> countries = cnec1.getLocation(network);
        assertEquals(Set.of(Country.BE), countries);

        countries = cnec2.getLocation(network);
        assertEquals(Set.of(Country.DE), countries);
    }

    @Test
    void testComputeValue() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(200.).withMax(500.).add()
            .add();
        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 400.);
        assertEquals(400., ((VoltageCnecValue) cnec.computeValue(networkMock1, Unit.KILOVOLT)).minValue(), DOUBLE_TOLERANCE);
        assertEquals(400., ((VoltageCnecValue) cnec.computeValue(networkMock1, Unit.KILOVOLT)).maxValue(), DOUBLE_TOLERANCE);
    }

    @Test
    void testComputeSecurityStatus() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(200.).withMax(500.).add()
            .add();
        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 400.);
        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", 700.);
        Network networkMock3 = mockBusVoltagesInNetwork("networkElement", 100.);

        assertEquals(Cnec.SecurityStatus.SECURE, cnec.computeSecurityStatus(networkMock1, Unit.KILOVOLT));
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, cnec.computeSecurityStatus(networkMock2, Unit.KILOVOLT));
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, cnec.computeSecurityStatus(networkMock3, Unit.KILOVOLT));
    }

    @Test
    void testVoltageCnecWithOneMaxThreshold() {

        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.).add()
            .add();

        // bounds
        assertEquals(500., cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec.getLowerBound(Unit.KILOVOLT).isPresent());

        // margin
        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 400.);
        assertEquals(100., cnec.computeMargin(networkMock1, Unit.KILOVOLT), DOUBLE_TOLERANCE); // bound: 500 MW

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", -300.);
        assertEquals(800., cnec.computeMargin(networkMock2, Unit.KILOVOLT), DOUBLE_TOLERANCE); // bound: 760 A
    }

    @Test
    void testVoltageCnecWithSeveralThresholds() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.).add()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).add()
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.).add()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-300.).add()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-50.).withMax(150.).add()
            .add();

        assertEquals(100., cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50., cnec.getLowerBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);

        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 300.);
        assertEquals(-200., cnec.computeMargin(networkMock1, Unit.KILOVOLT), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", -200.);
        assertEquals(-150., cnec.computeMargin(networkMock2, Unit.KILOVOLT), DOUBLE_TOLERANCE);
    }

    @Test
    void marginsWithNegativeAndPositiveLimits() {

        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();

        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", -300.);
        assertEquals(-100, cnec.computeMargin(networkMock1, Unit.KILOVOLT), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", 0.);
        assertEquals(200, cnec.computeMargin(networkMock2, Unit.KILOVOLT), DOUBLE_TOLERANCE);

        Network networkMock3 = mockBusVoltagesInNetwork("networkElement", 400.);
        assertEquals(100, cnec.computeMargin(networkMock3, Unit.KILOVOLT), DOUBLE_TOLERANCE);

        Network networkMock4 = mockBusVoltagesInNetwork("networkElement", 800.);
        assertEquals(-300, cnec.computeMargin(networkMock4, Unit.KILOVOLT), DOUBLE_TOLERANCE);
    }

    // other

    @Test
    void testEqualsAndHashCode() {
        VoltageCnec cnec1 = initPreventiveCnecAdder().newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.).add().add();
        VoltageCnec cnec2 = initPreventiveCnecAdder().withId("anotherId").newThreshold().withUnit(Unit.KILOVOLT).withMin(-1000.).add().add();

        assertEquals(cnec1, cnec1);
        assertNotEquals(cnec1, cnec2);
        assertNotNull(cnec1);
        assertNotEquals(1, cnec1);

        assertEquals(cnec1.hashCode(), cnec1.hashCode());
        assertNotEquals(cnec1.hashCode(), cnec2.hashCode());
    }

    private static Network mockBusVoltagesInNetwork(String elementId, double voltage) {
        Network network = Mockito.mock(Network.class);
        VoltageLevel voltageLevel = Mockito.mock(VoltageLevel.class);
        Mockito.when(network.getVoltageLevel(elementId)).thenReturn(voltageLevel);
        BusbarSection busbarSection = Mockito.mock(BusbarSection.class);
        Mockito.when(network.getBusbarSection(elementId)).thenReturn(busbarSection);
        Mockito.when(busbarSection.getV()).thenReturn(voltage);
        return network;
    }
}
