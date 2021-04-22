/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.CracImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PerimetersTest {

    private CracImpl crac;
    private StateTree stateTree;

    @Before
    public void setUp() {
        crac = new CracImpl("crac-id");
        crac.newContingency().withId("contingency-1").add();
        crac.newContingency().withId("contingency-2").add();
        crac.newContingency().withId("contingency-3").add();
        crac.newFlowCnec()
            .withInstant(Instant.PREVENTIVE)
            .withId("cnec1-preventive")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingency-1")
            .withId("cnec1-outage1")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(400.).withMin(-400.).add()
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-1")
            .withId("cnec1-curative1")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingency-2")
            .withId("cnec1-outage2")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(500.).withMin(-500.).add()
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-2")
            .withId("cnec1-curative2")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingency-3")
            .withId("cnec1-outage3")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-3")
            .withId("cnec1-curative3")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .add();
    }

    @Test
    public void testCreatePerimetersWithNoRemedialActions() {
        stateTree = new StateTree(crac, crac.getPreventiveState());
        assertEquals(1, stateTree.getOptimizedStates().size());
        assertEquals(7, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }

    @Test
    public void testCreatePerimetersWithOneRemedialActionOnCurative() {
        crac.newPstRangeAction()
                .withId("pst-ra")
                .withNetworkElement("pst1")
                .newOnStateUsageRule().withContingency("contingency-1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        stateTree = new StateTree(crac, crac.getPreventiveState());
        assertEquals(2, stateTree.getOptimizedStates().size());
        assertEquals(6, stateTree.getPerimeter(crac.getPreventiveState()).size());
        assertEquals(1, stateTree.getPerimeter(crac.getState("contingency-1", Instant.CURATIVE)).size());
    }

    @Test
    public void testCreatePerimetersWithTwoRemedialActions() {
        crac.newPstRangeAction()
                .withId("pst-ra-1")
                .withNetworkElement("pst1")
                .newOnStateUsageRule().withContingency("contingency-1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        crac.newPstRangeAction()
                .withId("pst-ra-2")
                .withNetworkElement("pst2")
                .newOnStateUsageRule().withContingency("contingency-2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        stateTree = new StateTree(crac, crac.getPreventiveState());
        assertEquals(3, stateTree.getOptimizedStates().size());
        assertEquals(5, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }

    @Test
    public void testCreatePerimetersWithTwoRemedialActionsOnSameContingency() {
        crac.newPstRangeAction()
                .withId("pst-ra-1")
                .withNetworkElement("pst1")
                .newOnStateUsageRule().withContingency("contingency-2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        crac.newPstRangeAction()
                .withId("pst-ra-2")
                .withNetworkElement("pst2")
                .newOnStateUsageRule().withContingency("contingency-2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        stateTree = new StateTree(crac, crac.getPreventiveState());
        assertEquals(2, stateTree.getOptimizedStates().size());
        assertEquals(6, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }
}
