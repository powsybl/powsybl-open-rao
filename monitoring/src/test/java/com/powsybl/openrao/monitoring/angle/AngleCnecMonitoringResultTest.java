/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class AngleCnecMonitoringResultTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private Crac crac;

    @BeforeEach
    void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
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

    @Test
    void testComputeValue() {

        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.).add()
            .add();
        Network networkMock1 = mockBusAngleInNetwork("exportingNetworkElement", 0., "importingNetworkElement", 300.);
        Network networkMock2 = mockBusAngleInNetwork("exportingNetworkElement", 900., "importingNetworkElement", 100.);

        AngleCnecMonitoringResult angleCnecResult1 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock1);
        AngleCnecMonitoringResult angleCnecResult2 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock2);

        assertEquals(-300., angleCnecResult1.getAngle(), DOUBLE_TOLERANCE);
        assertEquals(800., angleCnecResult2.getAngle(), DOUBLE_TOLERANCE);
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

        AngleCnecMonitoringResult angleCnecResult = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock);

        assertEquals(Cnec.SecurityStatus.SECURE, angleCnecResult.getCnecSecurityStatus());
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
        AngleCnecMonitoringResult angleCnecResult = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock);

        assertEquals(Cnec.SecurityStatus.SECURE, angleCnecResult.getCnecSecurityStatus());
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
        AngleCnecMonitoringResult angleCnecResult1 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock1);
        assertEquals(800., angleCnecResult1.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", 0.);
        AngleCnecMonitoringResult angleCnecResult2 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock2);

        assertEquals(200., angleCnecResult2.getMargin(), DOUBLE_TOLERANCE);

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
        AngleCnecMonitoringResult angleCnecResult1 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock1);
        assertEquals(-200, angleCnecResult1.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusAngleInNetwork("exportingNetworkElement", 0., "importingNetworkElement", 200.);
        AngleCnecMonitoringResult angleCnecResult2 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock2);
        assertEquals(-150., angleCnecResult2.getMargin(), DOUBLE_TOLERANCE);

    }

    @Test
    void marginsWithNegativeAndPositiveLimits() {

        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).withMax(500.).add()
            .add();

        Network networkMock1 = mockBusAngleInNetwork("exportingNetworkElement", 0., "importingNetworkElement", 300.);
        AngleCnecMonitoringResult angleCnecResult1 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock1);
        assertEquals(-100, angleCnecResult1.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock2 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", 300.);
        AngleCnecMonitoringResult angleCnecResult2 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock2);
        assertEquals(200, angleCnecResult2.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock3 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", -100.);
        AngleCnecMonitoringResult angleCnecResult3 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock3);
        assertEquals(100, angleCnecResult3.getMargin(), DOUBLE_TOLERANCE);

        Network networkMock4 = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", -500.);
        AngleCnecMonitoringResult angleCnecResult4 = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMock4);
        assertEquals(-300, angleCnecResult4.getMargin(), DOUBLE_TOLERANCE);
    }

    @Test
    void testComputeSecurityStatus() {
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).withMax(500.).add()
            .add();
        Network networkMockWithBusAngleWithinThresholds = mockBusAngleInNetwork("exportingNetworkElement", 300., "importingNetworkElement", 0.);
        Network networkMockWithBusAngleLowerThanThresholds = mockBusAngleInNetwork("exportingNetworkElement", -300., "importingNetworkElement", 0.);
        Network networkMockWithBusAngleHigherThanThresholds = mockBusAngleInNetwork("exportingNetworkElement", 1300., "importingNetworkElement", 0.);

        AngleCnecMonitoringResult angleCnecResultWithBusAngleWithinThresholds = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMockWithBusAngleWithinThresholds);
        AngleCnecMonitoringResult angleCnecResultWithBusAngleLowerThanThresholds = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMockWithBusAngleLowerThanThresholds);
        AngleCnecMonitoringResult angleCnecResultWithBusAngleHigherThanThresholds = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, networkMockWithBusAngleHigherThanThresholds);

        assertEquals(Cnec.SecurityStatus.SECURE, angleCnecResultWithBusAngleWithinThresholds.getCnecSecurityStatus());
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, angleCnecResultWithBusAngleLowerThanThresholds.getCnecSecurityStatus());
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, angleCnecResultWithBusAngleHigherThanThresholds.getCnecSecurityStatus());
    }

    @Test
    void testConstructor() {
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).withMax(500.).add()
            .add();
        AngleCnecMonitoringResult angleCnecResult = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, 100);
        assertEquals(100., angleCnecResult.getAngle());
        assertEquals(300., angleCnecResult.getMargin());
        assertEquals(Cnec.SecurityStatus.SECURE, angleCnecResult.getCnecSecurityStatus());

        angleCnecResult = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, Double.NaN);
        assertEquals(Double.NaN, angleCnecResult.getAngle());
        assertEquals(Double.NaN, angleCnecResult.getMargin());
        assertEquals(Cnec.SecurityStatus.FAILURE, angleCnecResult.getCnecSecurityStatus());
    }

    @Test
    void testPrint() {
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).withMax(500.).add()
            .add();
        AngleCnecMonitoringResult angleCnecResult = new AngleCnecMonitoringResult(cnec, Unit.DEGREE, 100);
        assertEquals(
            "AngleCnec angle-cnec (with importing network element importingNetworkElement and exporting " +
                "network element exportingNetworkElement) at state preventive has an angle of 100.0°.",
            angleCnecResult.print()
        );
    }

    @Test
    void testCheckUnit() {
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).withMax(500.).add()
            .add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new AngleCnecMonitoringResult(cnec, Unit.KILOVOLT, 100));
        assertEquals("An angleCnecMonitoringResult must be in DEGREE", exception.getMessage());
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
