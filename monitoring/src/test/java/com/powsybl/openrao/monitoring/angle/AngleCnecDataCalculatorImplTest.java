/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import com.powsybl.openrao.monitoring.SecurityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class AngleCnecDataCalculatorImplTest {
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
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.).add()
            .add();
        Network networkMock1 = mockBusAngleInNetwork("exportingNetworkElement", 0., "importingNetworkElement", 300.);
        Network networkMock2 = mockBusAngleInNetwork("exportingNetworkElement", 900., "importingNetworkElement", 100.);

        assertEquals(-300., AngleCnecDataCalculator.computeAngle(cnec, networkMock1, Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(800., AngleCnecDataCalculator.computeAngle(cnec, networkMock2, Unit.DEGREE), DOUBLE_TOLERANCE);
    }

    @Test
    void checkComputeSecurityStatusReturnsSecure() {
        AngleCnec cnec = crac.newAngleCnec()
            .withId("cnec-1-id")
            .withExportingNetworkElement("BBE1AA1")
            .withImportingNetworkElement("BBE2AA1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(Unit.DEGREE).withMax(1000.).add()
            .add();
        Network networkMock = mockBusAngleInNetwork("BBE1AA1", 0., "BBE2AA1", 300.);

        assertEquals(SecurityStatus.SECURE, AngleCnecDataCalculator.computeSecurityStatus(cnec, networkMock, Unit.DEGREE));
    }

    @Test
    void checkComputeSecurityStatusReturnsHighConstraint() {
        AngleCnec cnec = crac.newAngleCnec()
            .withId("cnec-1-id")
            .withExportingNetworkElement("BBE1AA1")
            .withImportingNetworkElement("BBE2AA1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(Unit.DEGREE).withMax(1000.).add()
            .add();
        Network networkMock = mockBusAngleInNetwork("BBE1AA1", 1200., "BBE2AA1", 300.);

        assertEquals(SecurityStatus.SECURE, AngleCnecDataCalculator.computeSecurityStatus(cnec, networkMock, Unit.DEGREE));
    }

    // test threshold on branches whose nominal voltage is the same on both side

    @Test
    void testAngleCnecWithOneMaxThreshold() {

        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.).add()
            .add();

        // bounds
        assertEquals(500., cnec.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec.getLowerBound(Unit.DEGREE).isPresent());

        // margin
        Network networkMock1 = mockBusAngleInNetwork("exportingNetworkElement", 0., "importingNetworkElement", 300.);
        assertEquals(800., AngleCnecDataCalculator.computeMargin(cnec, networkMock1, Unit.DEGREE), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", 0.);
        assertEquals(200., AngleCnecDataCalculator.computeMargin(cnec, networkMock2, Unit.DEGREE), DOUBLE_TOLERANCE);
    }

    @Test
    void testAngleCnecWithSeveralThresholds() {
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.).add()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).add()
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.).add()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-300.).add()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-50.).withMax(150.).add()
            .add();

        assertEquals(100., cnec.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50., cnec.getLowerBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);

        Network networkMock1 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", 0.);
        assertEquals(-200, AngleCnecDataCalculator.computeMargin(cnec, networkMock1, Unit.DEGREE), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusAngleInNetwork("exportingNetworkElement", 0., "importingNetworkElement", 200.);
        assertEquals(-150., AngleCnecDataCalculator.computeMargin(cnec, networkMock2, Unit.DEGREE), DOUBLE_TOLERANCE);
    }

    @Test
    void marginsWithNegativeAndPositiveLimits() {
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).withMax(500.).add()
            .add();

        Network networkMock1 = mockBusAngleInNetwork("exportingNetworkElement", 0., "importingNetworkElement", 300.);
        assertEquals(-100, AngleCnecDataCalculator.computeMargin(cnec, networkMock1, Unit.DEGREE), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", 300.);
        assertEquals(200, AngleCnecDataCalculator.computeMargin(cnec, networkMock2, Unit.DEGREE), DOUBLE_TOLERANCE);

        Network networkMock3 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", -100.);
        assertEquals(100, AngleCnecDataCalculator.computeMargin(cnec, networkMock3, Unit.DEGREE), DOUBLE_TOLERANCE);

        Network networkMock4 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", -500.);
        assertEquals(-300, AngleCnecDataCalculator.computeMargin(cnec, networkMock4, Unit.DEGREE), DOUBLE_TOLERANCE);
    }

    @Test
    void testComputeSecurityStatus() {
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).withMax(500.).add()
            .add();
        Network networkMockWithBusAngleWithinThresholds = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", 0.);
        Network networkMockWithBusAngleLowerThanThresholds = mockBusAngleInNetwork("exportingNetworkElement", -300., "importingNetworkElement", 0.);
        Network networkMockWithBusAngleHigherThanThresholds = mockBusAngleInNetwork("exportingNetworkElement", 1300., "importingNetworkElement", 0.);

        assertEquals(SecurityStatus.SECURE, AngleCnecDataCalculator.computeSecurityStatus(cnec, networkMockWithBusAngleWithinThresholds, Unit.DEGREE));
        assertEquals(SecurityStatus.LOW_CONSTRAINT, AngleCnecDataCalculator.computeSecurityStatus(cnec, networkMockWithBusAngleLowerThanThresholds, Unit.DEGREE));
        assertEquals(SecurityStatus.HIGH_CONSTRAINT, AngleCnecDataCalculator.computeSecurityStatus(cnec, networkMockWithBusAngleHigherThanThresholds, Unit.DEGREE));
    }

    private AngleCnecAdder initPreventiveCnecAdder() {
        return crac.newAngleCnec()
            .withId("angle-cnec")
            .withName("angle-cnec-name")
            .withExportingNetworkElement("exportingNetworkElement")
            .withImportingNetworkElement("importingNetworkElement")
            .withOperator("FR")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOptimized(false);
    }

    private static Network mockBusAngleInNetwork(String exportingElement, double expAngle, String importingElement, double impAngle) {
        Network network = Mockito.mock(Network.class);
        VoltageLevel exportingVl = Mockito.mock(VoltageLevel.class);
        Mockito.when(exportingVl.getId()).thenReturn(exportingElement);
        Mockito.when(network.getVoltageLevel(exportingElement)).thenReturn(exportingVl);
        VoltageLevel.BusView bv = Mockito.mock(VoltageLevel.BusView.class);
        Mockito.when(exportingVl.getBusView()).thenReturn(bv);

        Bus bus = Mockito.mock(Bus.class);
        Mockito.when(bus.getAngle()).thenReturn(expAngle);
        Mockito.when(bv.getBusStream()).thenReturn(Stream.of(bus));

        Network.BusBreakerView busBreakerView = Mockito.mock(Network.BusBreakerView.class);
        Mockito.when(network.getBusBreakerView()).thenReturn(busBreakerView);
        Mockito.when(busBreakerView.getBus(exportingElement)).thenReturn(bus);
        Mockito.when(bus.getVoltageLevel()).thenReturn(exportingVl);

        // importing vl
        VoltageLevel importingVl = Mockito.mock(VoltageLevel.class);
        Mockito.when(importingVl.getId()).thenReturn(importingElement);
        Mockito.when(network.getVoltageLevel(importingElement)).thenReturn(importingVl);
        VoltageLevel.BusView bvI = Mockito.mock(VoltageLevel.BusView.class);
        Mockito.when(importingVl.getBusView()).thenReturn(bvI);

        Bus busI = Mockito.mock(Bus.class);
        Mockito.when(busI.getAngle()).thenReturn(impAngle);
        Mockito.when(bvI.getBusStream()).thenReturn(Stream.of(busI));

        Network.BusBreakerView busBreakerViewI = Mockito.mock(Network.BusBreakerView.class);
        Mockito.when(network.getBusBreakerView()).thenReturn(busBreakerViewI);
        Mockito.when(busBreakerViewI.getBus(importingElement)).thenReturn(busI);

        Mockito.when(busI.getVoltageLevel()).thenReturn(importingVl);
        return network;
    }
}
