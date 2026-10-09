/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class VoltageCnecMonitoringResultTest {

    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private Crac crac;

    @BeforeEach
    void setUp() {
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
    void testComputeValue() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(200.).withMax(500.).add()
            .add();
        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 400.);
        VoltageCnecMonitoringResult voltageCnecResult1 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock1);
        assertEquals(400., voltageCnecResult1.getMinVoltage(), DOUBLE_TOLERANCE);
        assertEquals(400., voltageCnecResult1.getMaxVoltage(), DOUBLE_TOLERANCE);
    }

    @Test
    void testComputeSecurityStatus() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(200.).withMax(500.).add()
            .add();
        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", 400.);
        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", 700.);
        Network networkMock3 = mockBusVoltagesInNetwork("networkElement", 100.);
        VoltageCnecMonitoringResult voltageCnecResult1 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock1);
        assertEquals(Cnec.SecurityStatus.SECURE, voltageCnecResult1.getCnecSecurityStatus());
        VoltageCnecMonitoringResult voltageCnecResult2 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock2);
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, voltageCnecResult2.getCnecSecurityStatus());
        VoltageCnecMonitoringResult voltageCnecResult3 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock3);
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, voltageCnecResult3.getCnecSecurityStatus());
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
        VoltageCnecMonitoringResult voltageCnecResult1 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock1);
        assertEquals(100., voltageCnecResult1.getMargin(), DOUBLE_TOLERANCE); // bound: 500 MW

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", -300.);
        VoltageCnecMonitoringResult voltageCnecResult2 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock2);
        assertEquals(800., voltageCnecResult2.getMargin(), DOUBLE_TOLERANCE); // bound: 760 A
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
        VoltageCnecMonitoringResult voltageCnecResult1 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock1);
        assertEquals(-200., voltageCnecResult1.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", -200.);
        VoltageCnecMonitoringResult voltageCnecResult2 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock2);
        assertEquals(-150., voltageCnecResult2.getMargin(), DOUBLE_TOLERANCE);
    }

    @Test
    void marginsWithNegativeAndPositiveLimits() {

        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();

        Network networkMock1 = mockBusVoltagesInNetwork("networkElement", -300.);
        VoltageCnecMonitoringResult voltageCnecResult1 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock1);
        assertEquals(-100, voltageCnecResult1.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusVoltagesInNetwork("networkElement", 0.);
        VoltageCnecMonitoringResult voltageCnecResult2 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock2);
        assertEquals(200, voltageCnecResult2.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock3 = mockBusVoltagesInNetwork("networkElement", 400.);
        VoltageCnecMonitoringResult voltageCnecResult3 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock3);
        assertEquals(100, voltageCnecResult3.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock4 = mockBusVoltagesInNetwork("networkElement", 800.);
        VoltageCnecMonitoringResult voltageCnecResult4 = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock4);
        assertEquals(-300, voltageCnecResult4.getMargin(), DOUBLE_TOLERANCE);
    }

    @Test
    void testConstructor() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();
        VoltageCnecMonitoringResult voltageCnecResult = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, 100., 200.);
        assertEquals(300., voltageCnecResult.getMargin());
        voltageCnecResult = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, Double.NaN, Double.NaN);
        assertEquals(Double.NaN, voltageCnecResult.getMargin());
        voltageCnecResult = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, 0., Double.NaN);
        assertEquals(Double.NaN, voltageCnecResult.getMargin());
        voltageCnecResult = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, Double.NaN, 0.);
        assertEquals(Double.NaN, voltageCnecResult.getMargin());
    }

    @Test
    void testPrint() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();
        VoltageCnecMonitoringResult voltageCnecResult = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, 100., 200.);
        assertEquals("Network element networkElement at state preventive has a min voltage of 100.0 kV and a max voltage of 200.0 kV.", voltageCnecResult.print());
    }

    @Test
    void testFailureSecurityStatus() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();
        VoltageCnecMonitoringResult voltageCnecResult = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, Double.NaN, Double.NaN);
        assertEquals(Cnec.SecurityStatus.FAILURE, voltageCnecResult.getCnecSecurityStatus());
    }

    @Test
    void testHighAndLowConstraintSecurityStatus() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();
        VoltageCnecMonitoringResult voltageCnecResult = new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, -300., 600.);
        assertEquals(Cnec.SecurityStatus.HIGH_AND_LOW_CONSTRAINTS, voltageCnecResult.getCnecSecurityStatus());
    }

    @Test
    void testMissingVoltageLevel() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();
        Network networkMock = Mockito.mock(Network.class);
        Mockito.when(networkMock.getVoltageLevel(Mockito.anyString())).thenReturn(null);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, networkMock));
        assertEquals("Voltage level is missing on network element networkElement", exception.getMessage());
    }

    @Test
    void testCheckUnit() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new VoltageCnecMonitoringResult(cnec, Unit.DEGREE, 100., 100.));
        assertEquals("A voltageCnecMonitoringResult must be in KILOVOLT", exception.getMessage());
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
