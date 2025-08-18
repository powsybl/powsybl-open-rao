/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import com.powsybl.openrao.monitoring.SecurityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class VoltageCnecDataCalculatorTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private Crac crac;

    @BeforeEach
    void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
    }

    @Test
    void testComputeValue() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(200.).withMax(500.).add()
            .add();
        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 400.);

        VoltageCnecDataCalculator voltageCnecDataCalculator = new VoltageCnecDataCalculator();
        assertEquals(400., (voltageCnecDataCalculator.computeValue(cnec, networkMock1, Unit.KILOVOLT)).minValue(), DOUBLE_TOLERANCE);
        assertEquals(400., (voltageCnecDataCalculator.computeValue(cnec, networkMock1, Unit.KILOVOLT)).maxValue(), DOUBLE_TOLERANCE);
    }

    @Test
    void testComputeSecurityStatus() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(200.).withMax(500.).add()
            .add();
        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 400.);
        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", 700.);
        Network networkMock3 = mockBusVoltagesInNetwork("networkElement", 100.);

        VoltageCnecDataCalculator voltageCnecDataCalculator = new VoltageCnecDataCalculator();
        assertEquals(SecurityStatus.SECURE, voltageCnecDataCalculator.computeSecurityStatus(cnec, networkMock1, Unit.KILOVOLT));
        assertEquals(SecurityStatus.HIGH_CONSTRAINT, voltageCnecDataCalculator.computeSecurityStatus(cnec, networkMock2, Unit.KILOVOLT));
        assertEquals(SecurityStatus.LOW_CONSTRAINT, voltageCnecDataCalculator.computeSecurityStatus(cnec, networkMock3, Unit.KILOVOLT));
    }

    @Test
    void testVoltageCnecWithOneMaxThreshold() {

        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.).add()
            .add();

        // bounds
        assertEquals(500., cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec.getLowerBound(Unit.KILOVOLT).isPresent());

        VoltageCnecDataCalculator voltageCnecDataCalculator = new VoltageCnecDataCalculator();

        // margin
        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 400.);
        assertEquals(100., voltageCnecDataCalculator.computeMargin(cnec, networkMock1, Unit.KILOVOLT), DOUBLE_TOLERANCE); // bound: 500 MW

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", -300.);
        assertEquals(800., voltageCnecDataCalculator.computeMargin(cnec, networkMock2, Unit.KILOVOLT), DOUBLE_TOLERANCE); // bound: 760 A
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

        VoltageCnecDataCalculator voltageCnecDataCalculator = new VoltageCnecDataCalculator();

        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 300.);
        assertEquals(-200., voltageCnecDataCalculator.computeMargin(cnec, networkMock1, Unit.KILOVOLT), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", -200.);
        assertEquals(-150., voltageCnecDataCalculator.computeMargin(cnec, networkMock2, Unit.KILOVOLT), DOUBLE_TOLERANCE);
    }

    @Test
    void marginsWithNegativeAndPositiveLimits() {

        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();

        VoltageCnecDataCalculator voltageCnecDataCalculator = new VoltageCnecDataCalculator();

        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", -300.);
        assertEquals(-100, voltageCnecDataCalculator.computeMargin(cnec, networkMock1, Unit.KILOVOLT), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", 0.);
        assertEquals(200, voltageCnecDataCalculator.computeMargin(cnec, networkMock2, Unit.KILOVOLT), DOUBLE_TOLERANCE);

        Network networkMock3 = mockBusVoltagesInNetwork("networkElement", 400.);
        assertEquals(100, voltageCnecDataCalculator.computeMargin(cnec, networkMock3, Unit.KILOVOLT), DOUBLE_TOLERANCE);

        Network networkMock4 = mockBusVoltagesInNetwork("networkElement", 800.);
        assertEquals(-300, voltageCnecDataCalculator.computeMargin(cnec, networkMock4, Unit.KILOVOLT), DOUBLE_TOLERANCE);
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
