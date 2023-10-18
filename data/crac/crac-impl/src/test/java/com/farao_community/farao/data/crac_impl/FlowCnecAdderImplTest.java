/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class FlowCnecAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);
    private final String contingency1Id = "condId1";
    private CracImpl crac;
    private Contingency contingency1;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac");
        crac.addInstant(INSTANT_PREV);
        crac.addInstant(INSTANT_OUTAGE);
        crac.addInstant(INSTANT_AUTO);
        crac.addInstant(INSTANT_CURATIVE);
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    @Test
    void testAdd() {
        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withOperator("cnec1Operator")
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(1000.0).withMin(-1000.0).add()
            .add();
        FlowCnec cnec2 = crac.newFlowCnec()
            .withId("cnecId2")
            .withInstantId(INSTANT_PREV.getId())
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(500.0).add()
            .add();
        assertEquals(2, crac.getFlowCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getFlowCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(INSTANT_OUTAGE, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("neName1", cnec1.getNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getFlowCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(INSTANT_PREV, cnec2.getState().getInstant());
        assertEquals("cnec2Operator", cnec2.getOperator());
        assertEquals(Optional.empty(), cnec2.getState().getContingency());
        assertEquals("neId2", cnec2.getNetworkElement().getName());
        assertEquals(500.0, cnec2.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec2.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());

        // Verify that network elements were created
        crac.newFlowCnec()
            .withId("cnecId3")
            .withInstantId(INSTANT_PREV.getId())
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2") // same as cnec2
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(500.0).add()
            .add();
        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neId1"));
        assertNotNull(crac.getNetworkElement("neId2"));

        // Verify states were created
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertNotNull(crac.getState(contingency1Id, INSTANT_OUTAGE));
    }

    @Test
    void testFrmHandling() {
        double maxValueInMw = 100.0;
        double frmInMw = 5.0;
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(maxValueInMw).withMin(-maxValueInMw).add()
            .withReliabilityMargin(frmInMw)
            .add();
        assertEquals(maxValueInMw - frmInMw, cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(FaraoException::new), 0.0);
        assertEquals(frmInMw - maxValueInMw, cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(FaraoException::new), 0.0);
    }

    @Test
    void testNotOptimizedMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withMonitored()
            .add();
        assertFalse(cnec.isOptimized());
        assertTrue(cnec.isMonitored());
    }

    @Test
    void testOptimizedNotMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withOptimized()
            .add();
        assertTrue(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNotOptimizedNotMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(380.)
            .withIMax(1200., LEFT)
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNotOptimizedNotMonitored2() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.AMPERE).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .withNominalVoltage(380.)
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void checkThresholdSideInitialisation1() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMax(1.).withMin(-100.0).add()
            .newThreshold().withUnit(Unit.AMPERE).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .newThreshold().withUnit(Unit.AMPERE).withSide(LEFT).withMax(200.0).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .withNominalVoltage(380., LEFT)
            .withNominalVoltage(220., RIGHT)
            .withIMax(2000.)
            .add();

        assertEquals(3, cnec.getThresholds().size());
        assertTrue(cnec.getThresholds().stream().allMatch(th -> th.getSide().equals(LEFT)));
    }

    @Test
    void checkThresholdSideInitialisation2() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(RIGHT).withMax(100.0).withMin(-200.0).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(RIGHT).withMax(100.0).withMin(-100.0).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(RIGHT).withMax(1.).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .withNominalVoltage(380., LEFT)
            .withNominalVoltage(220., RIGHT)
            .withIMax(2000.)
            .add();

        assertEquals(3, cnec.getThresholds().size());
        assertTrue(cnec.getThresholds().stream().allMatch(th -> th.getSide().equals(RIGHT)));
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new FlowCnecAdderImpl(null));
    }

    @Test
    void testUniqueNetworkElement() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withNetworkElement("neId1", "neName1");
        assertThrows(FaraoException.class, () -> flowCnecAdder.withNetworkElement("neId2", "neName2"));
    }

    @Test
    void testNoIdFail() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withName("cnecName1")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(1000.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testNoStateInstantFail() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withContingency(contingency1Id)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.0).withSide(LEFT).add()
            .withNetworkElement("neId1", "neName1")
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testNoNetworkElementFail() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .newThreshold().withSide(LEFT).withUnit(Unit.MEGAWATT).withMax(1000.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testNoThresholdFail() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId1", "neName1")
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testAddTwiceError() {
        crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_PREV.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.)
            .add();
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_PREV.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testAddPreventiveCnecWithContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_PREV.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testAddOutageCnecWithNoContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testAddAutoCnecWithNoContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_AUTO.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testAddCurativeCnecWithNoContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testAddCurativeCnecWithAbsentContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency("absent-from-crac-contingency")
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testThresholdInPercentImaxButNoIMax1() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_PREV.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMax(1.).withMin(-1.).add()
            .withNominalVoltage(220.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testThresholdInPercentImaxButNoIMax2() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_PREV.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMax(1.).withMin(-1.).add()
            .withNominalVoltage(220.)
            .withIMax(1000., RIGHT); // threshold on left side cannot be interpreted
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testThresholdInAmpereButNoNominalVoltage() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_PREV.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.AMPERE).withSide(LEFT).withMax(1000.).add()
            .withIMax(1000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testThresholdInPercentImaxButNoNominalVoltage() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_PREV.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMax(1000.).add()
            .withNominalVoltage(220., LEFT) // should be defined on both side
            .withIMax(1000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testThresholdWithUnitKiloVolt() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstantId(INSTANT_PREV.getId())
            .withNetworkElement("Network Element ID")
            .newThreshold();
        assertThrows(FaraoException.class, () -> branchThresholdAdder.withUnit(Unit.KILOVOLT));
    }
}
