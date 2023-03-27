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
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
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
    private CracImpl crac;
    private final String contingency1Id = "condId1";
    private Contingency contingency1;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac");
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    @Test
    void testAdd() {
        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withOperator("cnec1Operator")
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(1000.0).withMin(-1000.0).add()
            .add();
        FlowCnec cnec2 = crac.newFlowCnec()
            .withId("cnecId2")
            .withInstant(Instant.PREVENTIVE)
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(500.0).add()
            .add();
        assertEquals(2, crac.getFlowCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getFlowCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(Instant.OUTAGE, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("neName1", cnec1.getNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getFlowCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(Instant.PREVENTIVE, cnec2.getState().getInstant());
        assertEquals("cnec2Operator", cnec2.getOperator());
        assertEquals(Optional.empty(), cnec2.getState().getContingency());
        assertEquals("neId2", cnec2.getNetworkElement().getName());
        assertEquals(500.0, cnec2.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec2.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());

        // Verify that network elements were created
        crac.newFlowCnec()
                .withId("cnecId3")
                .withInstant(Instant.PREVENTIVE)
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
        assertNotNull(crac.getState(contingency1Id, Instant.OUTAGE));
    }

    @Test
    void testFrmHandling() {
        double maxValueInMw = 100.0;
        double frmInMw = 5.0;
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
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
            .withInstant(Instant.OUTAGE)
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
            .withInstant(Instant.OUTAGE)
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
            .withInstant(Instant.OUTAGE)
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
                .withInstant(Instant.OUTAGE)
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
            .withInstant(Instant.OUTAGE)
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
            .withInstant(Instant.OUTAGE)
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
            .withInstant(Instant.OUTAGE)
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
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.0).add()
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
            .withInstant(Instant.OUTAGE)
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
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId1", "neName1")
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testAddTwiceError() {
        crac.newFlowCnec().withId("Cnec ID")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.)
            .add();
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        assertThrows(FaraoException.class, flowCnecAdder::add);
    }

    @Test
    void testAddPreventiveCnecWithContingencyError() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withContingency(contingency1Id)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
                .withNominalVoltage(220)
                .withIMax(2000.)
                .add());
    }

    @Test
    void testAddOutageCnecWithNoContingencyError() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.OUTAGE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
                .withNominalVoltage(220)
                .withIMax(2000.)
                .add());
    }

    @Test
    void testAddAutoCnecWithNoContingencyError() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.AUTO)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
                .withNominalVoltage(220)
                .withIMax(2000.)
                .add());
    }

    @Test
    void testAddCurativeCnecWithNoContingencyError() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.CURATIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
                .withNominalVoltage(220)
                .withIMax(2000.)
                .add());
    }

    @Test
    void testAddCurativeCnecWithAbsentContingencyError() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.CURATIVE)
                .withContingency("absent-from-crac-contingency")
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMax(100.0).withMin(-100.0).add()
                .withNominalVoltage(220)
                .withIMax(2000.)
                .add());
    }

    @Test
    void testThresholdInPercentImaxButNoIMax1() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMax(1.).withMin(-1.).add()
                .withNominalVoltage(220.)
                .add());
    }

    @Test
    void testThresholdInPercentImaxButNoIMax2() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMax(1.).withMin(-1.).add()
                .withNominalVoltage(220.)
                .withIMax(1000., RIGHT) // threshold on left side cannot be interpreted
                .add());
    }

    @Test
    void testThresholdInAmpereButNoNominalVoltage() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.AMPERE).withSide(LEFT).withMax(1000.).add()
                .withIMax(1000.)
                .add());
    }

    @Test
    void testThresholdInPercentImaxButNoNominalVoltage() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMax(1000.).add()
                .withNominalVoltage(220., LEFT) // should be defined on both side
                .withIMax(1000.)
                .add());
    }

    @Test
    void testThresholdWithUnitKiloVolt() {
        assertThrows(FaraoException.class, () ->
            crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.KILOVOLT).withSide(LEFT).withMax(100.).add()
                .withNominalVoltage(220.)
                .withIMax(1000.)
                .add());
    }
}
