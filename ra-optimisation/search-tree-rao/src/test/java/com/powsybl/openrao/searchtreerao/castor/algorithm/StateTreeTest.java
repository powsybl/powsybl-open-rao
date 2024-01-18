/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class StateTreeTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private StateTree stateTree;
    private State preventiveState;
    private State autoState;
    private State outageState;
    private State curativeState1;
    private State curativeState2;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    private void setUpCommonCrac(boolean withCra) {
        if (withCra) {
            crac = CommonCracCreation.createWithCurativePstRange();
        } else {
            crac = CommonCracCreation.create();
        }
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        preventiveState = crac.getPreventiveState();
        curativeState1 = crac.getState("Contingency FR1 FR2", curativeInstant);
        curativeState2 = crac.getState("Contingency FR1 FR3", curativeInstant);
        stateTree = new StateTree(crac);
    }

    @Test
    void testNoCraStartFromPreventive() {
        setUpCommonCrac(false);
        assertEquals(0, stateTree.getContingencyScenarios().size());
        assertEquals(crac.getStates(), stateTree.getBasecaseScenario().getAllStates());

        assertEquals(2, stateTree.getOperatorsNotSharingCras().size());
        assertTrue(stateTree.getOperatorsNotSharingCras().contains("operator1"));
        assertTrue(stateTree.getOperatorsNotSharingCras().contains("operator2"));
    }

    @Test
    void testInitFromPreventive() {
        setUpCommonCrac(true); // PST is operated by operator1, usable after CURATIVE_STATE_2

        assertEquals(Set.of(preventiveState, curativeState1), stateTree.getBasecaseScenario().getAllStates());

        assertEquals(1, stateTree.getContingencyScenarios().size());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("Contingency FR1 FR3"), contingencyScenario.getContingency());
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals(List.of(curativeState2), contingencyScenario.getCurativeStates());

        assertEquals(1, stateTree.getOperatorsNotSharingCras().size());
        assertEquals("operator2", stateTree.getOperatorsNotSharingCras().iterator().next());
    }

    private void setUpCustomCrac() {
        crac = CracFactory.findDefault().create("crac-id")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newContingency().withId("contingency-1").add();
        crac.newContingency().withId("contingency-2").add();
        crac.newContingency().withId("contingency-3").add();
        crac.newFlowCnec()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withId("cnec1-preventive")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("contingency-1")
            .withId("cnec1-outage1")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(400.).withMin(-400.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("contingency-1")
            .withId("cnec1-curative1")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("contingency-2")
            .withId("cnec1-outage2")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(500.).withMin(-500.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("contingency-2")
            .withId("cnec1-curative2")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("contingency-3")
            .withId("cnec1-outage3")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        crac.newFlowCnec()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("contingency-3")
            .withId("cnec1-curative3")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
    }

    @Test
    void testCreatePerimetersWithNoRemedialActions() {
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
    void testCreatePerimetersWithOneRemedialActionOnCurative() {
        setUpCustomCrac();
        crac.newPstRangeAction()
            .withId("pst-ra")
            .withNetworkElement("pst1")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnContingencyStateUsageRule().withContingency("contingency-1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        stateTree = new StateTree(crac);
        assertEquals(6, stateTree.getBasecaseScenario().getAllStates().size());
        assertEquals(1, stateTree.getContingencyScenarios().size());

        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency-1"), contingencyScenario.getContingency());
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals(List.of(crac.getState("contingency-1", curativeInstant)), contingencyScenario.getCurativeStates());
    }

    @Test
    void testCreatePerimetersWithTwoRemedialActions() {
        setUpCustomCrac();
        crac.newPstRangeAction()
            .withId("pst-ra-1")
            .withNetworkElement("pst1")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnContingencyStateUsageRule().withContingency("contingency-1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        crac.newPstRangeAction()
            .withId("pst-ra-2")
            .withNetworkElement("pst2")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnContingencyStateUsageRule().withContingency("contingency-2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        stateTree = new StateTree(crac);
        assertEquals(5, stateTree.getBasecaseScenario().getAllStates().size());
        assertEquals(2, stateTree.getContingencyScenarios().size());
    }

    @Test
    void testCreatePerimetersWithTwoRemedialActionsOnSameContingency() {
        setUpCustomCrac();
        crac.newPstRangeAction()
            .withId("pst-ra-1")
            .withNetworkElement("pst1")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnContingencyStateUsageRule().withContingency("contingency-2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        crac.newPstRangeAction()
            .withId("pst-ra-2")
            .withNetworkElement("pst2")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
            .newOnContingencyStateUsageRule().withContingency("contingency-2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        stateTree = new StateTree(crac);
        assertEquals(6, stateTree.getBasecaseScenario().getAllStates().size());
        assertEquals(1, stateTree.getContingencyScenarios().size());
    }

    @Test
    void testErrorOnOutageRa() {
        setUpCustomCrac();
        Crac mockCrac = Mockito.spy(crac);
        State outageState = crac.getState("contingency-1", outageInstant);
        Mockito.when(mockCrac.getPotentiallyAvailableNetworkActions(outageState))
            .thenReturn(Set.of(Mockito.mock(NetworkAction.class)));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new StateTree(mockCrac));
        assertEquals("Outage state contingency-1 - outage has available RAs. This is not supported.", exception.getMessage());
    }

    private void setUpCustomCracWithAutoInstant(boolean withAutoState, boolean withAutoRa, boolean withCurativeState, boolean withCurativeRa) {
        crac = CracFactory.findDefault().create("crac-id")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newContingency().withId("contingency").add();
        crac.newFlowCnec()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withId("cnec-preventive")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
            .withNominalVoltage(400.)
            .add();
        preventiveState = crac.getPreventiveState();
        crac.newFlowCnec()
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("contingency")
            .withId("cnec-outage")
            .withNetworkElement("ne1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(400.).withMin(-400.).add()
            .withNominalVoltage(400.)
            .add();
        outageState = crac.getState("contingency", outageInstant);
        if (withAutoState) {
            crac.newFlowCnec()
                .withInstant(AUTO_INSTANT_ID)
                .withContingency("contingency")
                .withId("cnec-auto")
                .withNetworkElement("ne1")
                .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(200.).withMin(-200.).add()
                .withNominalVoltage(400.)
                .add();
            if (withAutoRa) {
                crac.newPstRangeAction()
                    .withId("pst-ra-auto")
                    .withNetworkElement("pst")
                    .withSpeed(1)
                    .withInitialTap(1)
                    .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                    .newOnContingencyStateUsageRule().withContingency("contingency").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
                    .add();
            }
        }
        autoState = crac.getState("contingency", autoInstant);
        if (withCurativeState) {
            crac.newFlowCnec()
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("contingency")
                .withId("cnec-curative")
                .withNetworkElement("ne1")
                .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMax(400.).withMin(-400.).add()
                .withNominalVoltage(400.)
                .add();
            if (withCurativeRa) {
                crac.newPstRangeAction()
                    .withId("pst-ra-curative")
                    .withNetworkElement("pst")
                    .withInitialTap(1)
                    .withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                    .newOnContingencyStateUsageRule().withContingency("contingency").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                    .add();
            }
        }
        curativeState1 = crac.getState("contingency", curativeInstant);
    }

    @Test
    void testAutoPerimeters1() {
        // 1. Neither AUTO nor CURATIVE states exist
        setUpCustomCracWithAutoInstant(false, false, false, false);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertTrue(stateTree.getContingencyScenarios().isEmpty());
    }

    @Test
    void testAutoPerimeters2() {
        // 2. Only AUTO exists

        // 2.1 Only AUTO exists but has no RAs
        setUpCustomCracWithAutoInstant(true, false, false, false);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState, autoState), stateTree.getBasecaseScenario().getAllStates());
        assertTrue(stateTree.getContingencyScenarios().isEmpty());

        // 2.1 Only AUTO exists and has RAs
        setUpCustomCracWithAutoInstant(true, true, false, false);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new StateTree(crac));
        assertEquals("Automaton state contingency - auto has RAs, but none of the curative states for contingency 'contingency' do. This is not supported.", exception.getMessage());
    }

    @Test
    void testAutoPerimeters3() {
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
        assertEquals(List.of(curativeState1), contingencyScenario.getCurativeStates());
    }

    @Test
    void testAutoPerimeters4() {
        // 4. Both AUTO and CURATIVE exist

        // 4.1 Both AUTO and CURATIVE exist but have no RAs
        setUpCustomCracWithAutoInstant(true, false, true, false);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState, autoState, curativeState1), stateTree.getBasecaseScenario().getAllStates());
        assertTrue(stateTree.getContingencyScenarios().isEmpty());

        // 4.2 Both AUTO and CURATIVE exist, only AUTO has RAs
        setUpCustomCracWithAutoInstant(true, true, true, false);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new StateTree(crac));
        assertEquals("Automaton state contingency - auto has RAs, but none of the curative states for contingency 'contingency' do. This is not supported.", exception.getMessage());

        // 4.3 Both AUTO and CURATIVE exist, only CURATIVE has RAs
        setUpCustomCracWithAutoInstant(true, false, true, true);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState, autoState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.empty(), contingencyScenario.getAutomatonState());
        assertEquals(List.of(curativeState1), contingencyScenario.getCurativeStates());

        // 4.4 Both AUTO and CURATIVE exist and have RAs
        setUpCustomCracWithAutoInstant(true, true, true, true);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.of(autoState), contingencyScenario.getAutomatonState());
        assertEquals(List.of(curativeState1), contingencyScenario.getCurativeStates());
    }
}
