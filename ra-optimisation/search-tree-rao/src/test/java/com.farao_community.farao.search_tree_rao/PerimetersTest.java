/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PerimetersTest {

    private SimpleCrac crac;
    private Network network;
    private StateTree stateTree;

    @Before
    public void setUp() {
        network = Mockito.mock(Network.class);

        crac = new SimpleCrac("crac-id");
        crac.newContingency().setId("contingency-1").add();
        crac.newContingency().setId("contingency-2").add();
        crac.newContingency().setId("contingency-3").add();
        crac.newBranchCnec()
            .setInstant(Instant.PREVENTIVE)
            .setId("cnec1-preventive")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.AMPERE).setMax(200.).setMin(-200.).add()
            .add();
        crac.newBranchCnec()
            .setInstant(Instant.OUTAGE)
            .setContingency(crac.getContingency("contingency-1"))
            .setId("cnec1-outage1")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.AMPERE).setMax(400.).setMin(-400.).add()
            .add();
        crac.newBranchCnec()
            .setInstant(Instant.CURATIVE)
            .setContingency(crac.getContingency("contingency-1"))
            .setId("cnec1-curative1")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.AMPERE).setMax(200.).setMin(-200.).add()
            .add();
        crac.newBranchCnec()
            .setInstant(Instant.OUTAGE).
            setContingency(crac.getContingency("contingency-2"))
            .setId("cnec1-outage2")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.AMPERE).setMax(500.).setMin(-500.).add()
            .add();
        crac.newBranchCnec()
            .setInstant(Instant.CURATIVE)
            .setContingency(crac.getContingency("contingency-2"))
            .setId("cnec1-curative2")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.AMPERE).setMax(200.).setMin(-200.).add()
            .add();
        crac.newBranchCnec()
            .setInstant(Instant.OUTAGE)
            .setContingency(crac.getContingency("contingency-3"))
            .setId("cnec1-outage3")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.AMPERE).setMax(200.).setMin(-200.).add()
            .add();
        crac.newBranchCnec()
            .setInstant(Instant.CURATIVE)
            .setContingency(crac.getContingency("contingency-3"))
            .setId("cnec1-curative3")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.AMPERE).setMax(200.).setMin(-200.).add()
            .add();
    }

    @Test
    public void testCreatePerimetersWithNoRemedialActions() {
        stateTree = new StateTree(crac, network, crac.getPreventiveState());
        assertEquals(1, stateTree.getOptimizedStates().size());
        assertEquals(7, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }

    @Test
    public void testCreatePerimetersWithOneRemedialActionOnOutage() {
        PstRangeAction pstRange = new PstRangeActionImpl("pst-ra", crac.addNetworkElement("pst1"));
        pstRange.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("contingency-1", Instant.OUTAGE)));
        crac.addRangeAction(pstRange);
        stateTree = new StateTree(crac, network, crac.getPreventiveState());
        assertEquals(2, stateTree.getOptimizedStates().size());
        assertEquals(5, stateTree.getPerimeter(crac.getPreventiveState()).size());
        assertEquals(2, stateTree.getPerimeter(crac.getState("contingency-1", Instant.OUTAGE)).size());
    }

    @Test
    public void testCreatePerimetersWithOneRemedialActionOnCurative() {
        PstRangeAction pstRange = new PstRangeActionImpl("pst-ra", crac.addNetworkElement("pst1"));
        pstRange.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("contingency-1", Instant.CURATIVE)));
        crac.addRangeAction(pstRange);
        stateTree = new StateTree(crac, network, crac.getPreventiveState());
        assertEquals(2, stateTree.getOptimizedStates().size());
        assertEquals(6, stateTree.getPerimeter(crac.getPreventiveState()).size());
        assertEquals(1, stateTree.getPerimeter(crac.getState("contingency-1", Instant.CURATIVE)).size());
    }

    @Test
    public void testCreatePerimetersWithTwoRemedialActions() {
        PstRangeAction pstRange1 = new PstRangeActionImpl("pst-ra1", crac.addNetworkElement("pst1"));
        pstRange1.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("contingency-1", Instant.CURATIVE)));
        crac.addRangeAction(pstRange1);
        PstRangeAction pstRange2 = new PstRangeActionImpl("pst-ra2", crac.addNetworkElement("pst1"));
        pstRange2.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("contingency-2", Instant.OUTAGE)));
        crac.addRangeAction(pstRange2);
        stateTree = new StateTree(crac, network, crac.getPreventiveState());
        assertEquals(3, stateTree.getOptimizedStates().size());
        assertEquals(4, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }

    @Test
    public void testCreatePerimetersWithTwoRemedialActionsOnSameContingency() {
        PstRangeAction pstRange1 = new PstRangeActionImpl("pst-ra1", crac.addNetworkElement("pst1"));
        pstRange1.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("contingency-2", Instant.CURATIVE)));
        crac.addRangeAction(pstRange1);
        PstRangeAction pstRange2 = new PstRangeActionImpl("pst-ra2", crac.addNetworkElement("pst1"));
        pstRange2.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("contingency-2", Instant.OUTAGE)));
        crac.addRangeAction(pstRange2);
        stateTree = new StateTree(crac, network, crac.getPreventiveState());
        assertEquals(3, stateTree.getOptimizedStates().size());
        assertEquals(5, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }
}
