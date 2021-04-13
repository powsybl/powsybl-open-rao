/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
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
        BranchCnec cnec1 = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withOperator("cnec1Operator")
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(1000.0).withMin(-1000.0).add()
            .add();
        BranchCnec cnec2 = crac.newFlowCnec()
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
    }

    @Test
    public void testFrmHandling() {
        double maxValueInMw = 100.0;
        double frmInMw = 5.0;
        FlowCnecAdder cnecAdder = crac.newFlowCnec();
        BranchCnec cnec = cnecAdder.withId("Cnec ID")
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
        FlowCnecAdder cnecAdder = crac.newFlowCnec();
        BranchCnec cnec = cnecAdder.withId("Cnec ID")
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
        FlowCnecAdder cnecAdder = crac.newFlowCnec();
        BranchCnec cnec = cnecAdder.withId("Cnec ID")
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
        FlowCnecAdder cnecAdder = crac.newFlowCnec();
        BranchCnec cnec = cnecAdder.withId("Cnec ID")
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
        FlowCnecAdder cnecAdder = crac.newFlowCnec();
        BranchCnec cnec = cnecAdder.withId("Cnec ID")
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
}
