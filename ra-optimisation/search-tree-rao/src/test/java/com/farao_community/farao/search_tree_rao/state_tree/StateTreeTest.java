/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.state_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class StateTreeTest {
    private Crac crac;
    private StateTree stateTree;
    private State preventiveState;
    private State autoState;
    private State outageState;
    private State curativeState1;
    private State curativeState2;

    private void setUpCommonCrac(boolean withCra) {
        if (withCra) {
            crac = CommonCracCreation.createWithCurativePstRange();
        } else {
            crac = CommonCracCreation.create();
        }
        preventiveState = crac.getPreventiveState();
        curativeState1 = crac.getState("Contingency FR1 FR2", Instant.CURATIVE);
        curativeState2 = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        stateTree = new StateTree(crac);
    }

    @Test
    public void testNoCraStartFromPreventive() {
        setUpCommonCrac(false);
        assertEquals(0, stateTree.getContingencyScenarios().size());
        assertEquals(crac.getStates(), stateTree.getBasecaseScenario().getAllStates());

        assertEquals(2, stateTree.getOperatorsNotSharingCras().size());
        assertTrue(stateTree.getOperatorsNotSharingCras().contains("operator1"));
        assertTrue(stateTree.getOperatorsNotSharingCras().contains("operator2"));
    }

    @Test
    public void testInitFromPreventive() {
        setUpCommonCrac(true); // PST is operated by operator1, usable after CURATIVE_STATE_2

        assertEquals(Set.of(preventiveState, curativeState1), stateTree.getBasecaseScenario().getAllStates());

        assertEquals(1, stateTree.getContingencyScenarios().size());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("Contingency FR1 FR3"), contingencyScenario.getContingency());
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals(curativeState2, contingencyScenario.getCurativeState());

        assertEquals(1, stateTree.getOperatorsNotSharingCras().size());
        assertEquals("operator2", stateTree.getOperatorsNotSharingCras().iterator().next());
    }

    private void setUpCustomCrac() {
        crac = CracFactory.findDefault().create("crac-id");
        crac.newContingency().withId("contingency-1").add();
        crac.newContingency().withId("contingency-2").add();
        crac.newContingency().withId("contingency-3").add();
        crac.newFlowCnec()
            .withInstant(Instant.PREVENTIVE)
            .withId("cnec1-preventive")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingency-1")
            .withId("cnec1-outage1")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(400.).withMin(-400.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-1")
            .withId("cnec1-curative1")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingency-2")
            .withId("cnec1-outage2")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(500.).withMin(-500.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-2")
            .withId("cnec1-curative2")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingency-3")
            .withId("cnec1-outage3")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-3")
            .withId("cnec1-curative3")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
    }

    @Test
    public void testCreatePerimetersWithNoRemedialActions() {
        setUpCustomCrac();
        stateTree = new StateTree(crac);
        assertTrue(stateTree.getContingencyScenarios().isEmpty());
        BasecaseScenario basecaseScenario = stateTree.getBasecaseScenario();
        assertNotNull(basecaseScenario);
        assertEquals(crac.getPreventiveState(), basecaseScenario.getBasecaseState());
        assertEquals(6, basecaseScenario.getOtherStates().size());
        assertEquals(7, basecaseScenario.getAllStates().size());
    }

    @Test
    public void testCreatePerimetersWithOneRemedialActionOnCurative() {
        setUpCustomCrac();
        crac.newPstRangeAction()
            .withId("pst-ra")
            .withNetworkElement("pst1")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnStateUsageRule().withContingency("contingency-1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        stateTree = new StateTree(crac);
        assertEquals(6, stateTree.getBasecaseScenario().getAllStates().size());
        assertEquals(1, stateTree.getContingencyScenarios().size());

        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency-1"), contingencyScenario.getContingency());
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals(crac.getState("contingency-1", Instant.CURATIVE), contingencyScenario.getCurativeState());
    }

    @Test
    public void testCreatePerimetersWithTwoRemedialActions() {
        setUpCustomCrac();
        crac.newPstRangeAction()
            .withId("pst-ra-1")
            .withNetworkElement("pst1")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnStateUsageRule().withContingency("contingency-1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        crac.newPstRangeAction()
            .withId("pst-ra-2")
            .withNetworkElement("pst2")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnStateUsageRule().withContingency("contingency-2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        stateTree = new StateTree(crac);
        assertEquals(5, stateTree.getBasecaseScenario().getAllStates().size());
        assertEquals(2, stateTree.getContingencyScenarios().size());
    }

    @Test
    public void testCreatePerimetersWithTwoRemedialActionsOnSameContingency() {
        setUpCustomCrac();
        crac.newPstRangeAction()
            .withId("pst-ra-1")
            .withNetworkElement("pst1")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnStateUsageRule().withContingency("contingency-2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        crac.newPstRangeAction()
            .withId("pst-ra-2")
            .withNetworkElement("pst2")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnStateUsageRule().withContingency("contingency-2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        stateTree = new StateTree(crac);
        assertEquals(6, stateTree.getBasecaseScenario().getAllStates().size());
        assertEquals(1, stateTree.getContingencyScenarios().size());
    }

    @Test
    public void testErrorOnOutageRa() {
        setUpCustomCrac();
        Crac mockCrac = Mockito.spy(crac);
        State outageState = crac.getState("contingency-1", Instant.OUTAGE);
        Mockito.when(mockCrac.getNetworkActions(outageState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED))
            .thenReturn(Set.of(Mockito.mock(NetworkAction.class)));
        assertThrows(FaraoException.class, () -> new StateTree(mockCrac));
    }

    private void setUpCustomCracWithAutoInstant(boolean withAutoState, boolean withAutoRa, boolean withCurativeState, boolean withCurativeRa) {
        crac = CracFactory.findDefault().create("crac-id");
        crac.newContingency().withId("contingency").add();
        crac.newFlowCnec()
            .withInstant(Instant.PREVENTIVE)
            .withId("cnec-preventive")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        preventiveState = crac.getPreventiveState();
        crac.newFlowCnec()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingency")
            .withId("cnec-outage")
            .withNetworkElement("ne1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(400.).withMin(-400.).add()
            .withNominalVoltage(400.)
            .add();
        outageState = crac.getState("contingency", Instant.OUTAGE);
        if (withAutoState) {
            crac.newFlowCnec()
                .withInstant(Instant.AUTO)
                .withContingency("contingency")
                .withId("cnec-auto")
                .withNetworkElement("ne1")
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
                .withNominalVoltage(400.)
                .add();
            if (withAutoRa) {
                crac.newPstRangeAction()
                    .withId("pst-ra-auto")
                    .withNetworkElement("pst")
                    .withInitialTap(1)
                    .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                    .newOnStateUsageRule().withContingency("contingency").withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
                    .add();
            }
        }
        autoState = crac.getState("contingency", Instant.AUTO);
        if (withCurativeState) {
            crac.newFlowCnec()
                .withInstant(Instant.CURATIVE)
                .withContingency("contingency")
                .withId("cnec-curative")
                .withNetworkElement("ne1")
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMax(400.).withMin(-400.).add()
                .withNominalVoltage(400.)
                .add();
            if (withCurativeRa) {
                crac.newPstRangeAction()
                    .withId("pst-ra-curative")
                    .withNetworkElement("pst")
                    .withInitialTap(1)
                    .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                    .newOnStateUsageRule().withContingency("contingency").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                    .add();
            }
        }
        curativeState1 = crac.getState("contingency", Instant.CURATIVE);
    }

    @Test
    public void testAutoPerimeters1() {
        // 1. Neither AUTO nor CURATIVE states exist
        setUpCustomCracWithAutoInstant(false, false, false, false);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertTrue(stateTree.getContingencyScenarios().isEmpty());
    }

    @Test
    public void testAutoPerimeters2() {
        // 2. Only AUTO exists

        // 2.1 Only AUTO exists but has no RAs
        setUpCustomCracWithAutoInstant(true, false, false, false);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState, autoState), stateTree.getBasecaseScenario().getAllStates());
        assertTrue(stateTree.getContingencyScenarios().isEmpty());

        // 2.1 Only AUTO exists and has RAs
        setUpCustomCracWithAutoInstant(true, true, false, false);
        assertThrows(FaraoException.class, () -> new StateTree(crac));
    }

    @Test
    public void testAutoPerimeters3() {
        // 3. Only CURATIVE exists

        // 3.1 Only CURATIVE exists but has no RAs
        setUpCustomCracWithAutoInstant(false, false, true, false);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState, curativeState1), stateTree.getBasecaseScenario().getAllStates());
        assertTrue(stateTree.getContingencyScenarios().isEmpty());

        // 3.2 Only CURATIVE exists and has RAs
        setUpCustomCracWithAutoInstant(false, false, true, true);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.empty(), contingencyScenario.getAutomatonState());
        assertEquals(curativeState1, contingencyScenario.getCurativeState());
    }

    @Test
    public void testAutoPerimeters4() {
        // 4. Both AUTO and CURATIVE exist

        // 4.1 Both AUTO and CURATIVE exist but have no RAs
        setUpCustomCracWithAutoInstant(true, false, true, false);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState, autoState, curativeState1), stateTree.getBasecaseScenario().getAllStates());
        assertTrue(stateTree.getContingencyScenarios().isEmpty());

        // 4.2 Both AUTO and CURATIVE exist, only AUTO has RAs
        setUpCustomCracWithAutoInstant(true, true, true, false);
        assertThrows(FaraoException.class, () -> new StateTree(crac));

        // 4.3 Both AUTO and CURATIVE exist, only CURATIVE has RAs
        setUpCustomCracWithAutoInstant(true, false, true, true);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState, autoState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.empty(), contingencyScenario.getAutomatonState());
        assertEquals(curativeState1, contingencyScenario.getCurativeState());

        // 4.4 Both AUTO and CURATIVE exist and have RAs
        setUpCustomCracWithAutoInstant(true, true, true, true);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.of(autoState), contingencyScenario.getAutomatonState());
        assertEquals(curativeState1, contingencyScenario.getCurativeState());
    }
}
