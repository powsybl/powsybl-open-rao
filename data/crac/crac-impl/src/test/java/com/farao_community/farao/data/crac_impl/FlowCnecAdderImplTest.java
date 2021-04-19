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
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class FlowCnecAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private CracImpl crac;
    private String contingency1Id = "condId1";
    private Contingency contingency1;

    @Before
    public void setUp() {
        crac = new CracImpl("test-crac");
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        new FlowCnecAdderImpl(null);
    }

    @Test(expected = FaraoException.class)
    public void testUniqueNetworkElement() {
        crac.newFlowCnec()
                .withNetworkElement("neId1", "neName1")
                .withNetworkElement("neId2", "neName2");
    }

    @Test(expected = FaraoException.class)
    public void testNoIdFail() {
        crac.newFlowCnec()
            .withName("cnecName1")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(1000.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoStateInstantFail() {
        crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withContingency(contingency1Id)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.0).add()
            .withNetworkElement("neId1", "neName1")
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoNetworkElementFail() {
        crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .newThreshold().withRule(BranchThresholdRule.ON_HIGH_VOLTAGE_LEVEL).withUnit(Unit.MEGAWATT).withMax(1000.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoThresholdFail() {
        crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId1", "neName1")
            .add();
    }

    @Test
    public void testAdd() {
        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withOperator("cnec1Operator")
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(1000.0).withMin(-1000.0).add()
            .add();
        FlowCnec cnec2 = crac.newFlowCnec()
            .withId("cnecId2")
            .withInstant(Instant.PREVENTIVE)
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2")
            .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(500.0).add()
            .add();
        assertEquals(2, crac.getFlowCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getBranchCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(Instant.OUTAGE, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("neName1", cnec1.getNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getBranchCnec("cnecId2"));
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
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(500.0).add()
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
    public void testFrmHandling() {
        double maxValueInMw = 100.0;
        double frmInMw = 5.0;
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(maxValueInMw).withMin(-maxValueInMw).add()
            .withReliabilityMargin(frmInMw)
            .add();
        assertEquals(maxValueInMw - frmInMw, cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(FaraoException::new), 0.0);
        assertEquals(frmInMw - maxValueInMw, cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(FaraoException::new), 0.0);
    }

    @Test
    public void testNotOptimizedMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
            .withMonitored()
            .add();
        assertFalse(cnec.isOptimized());
        assertTrue(cnec.isMonitored());
    }

    @Test
    public void testOptimizedNotMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
            .withOptimized()
            .add();
        assertTrue(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    public void testNotOptimizedNotMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    public void testNotOptimizedNotMonitored2() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.OUTAGE)
                .withContingency(contingency1Id)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
                .withOptimized(false)
                .withMonitored(false)
                .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test(expected = FaraoException.class)
    public void testAddTwiceError() {
        crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
                .add();
        crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddPreventiveCnecWithContingencyError() {
        crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.PREVENTIVE)
                .withContingency(contingency1Id)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddOutageCnecWithNoContingencyError() {
        crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.OUTAGE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddAutoCnecWithNoContingencyError() {
        crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.AUTO)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddCurativeCnecWithNoContingencyError() {
        crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.CURATIVE)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddCurativeCnecWithAbsentContingencyError() {
        crac.newFlowCnec().withId("Cnec ID")
                .withInstant(Instant.CURATIVE)
                .withContingency("absent-from-crac-contingency")
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(100.0).withMin(-100.0).add()
                .add();
    }
}
