/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.CracImplFactory;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
            crac = CommonCracCreation.create(new CracImplFactory(), Set.of(Side.LEFT));
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
        assertEquals(1, contingencyScenario.getCurativePerimeters().size());
        assertEquals(curativeState2, contingencyScenario.getCurativePerimeters().get(0).getRaOptimisationState());
        assertEquals(Set.of(curativeState2), contingencyScenario.getCurativePerimeters().get(0).getAllStates());

        assertEquals(1, stateTree.getOperatorsNotSharingCras().size());
        assertEquals("operator2", stateTree.getOperatorsNotSharingCras().iterator().next());
    }

    private void setUpCustomCrac() {
        crac = new CracImplFactory().create("crac-id")
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
        Perimeter preventivePerimeter = stateTree.getBasecaseScenario();
        assertNotNull(preventivePerimeter);
        assertEquals(crac.getPreventiveState(), preventivePerimeter.getRaOptimisationState());
        assertEquals(7, preventivePerimeter.getAllStates().size());
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
        assertEquals(1, contingencyScenario.getCurativePerimeters().size());
        assertEquals(crac.getState("contingency-1", curativeInstant), contingencyScenario.getCurativePerimeters().get(0).getRaOptimisationState());
        assertEquals(Set.of(crac.getState("contingency-1", curativeInstant)), contingencyScenario.getCurativePerimeters().get(0).getAllStates());
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
        crac = new CracImplFactory().create("crac-id")
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
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        assertEquals(Optional.of(autoState), stateTree.getContingencyScenarios().iterator().next().getAutomatonState());
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
        assertEquals(1, contingencyScenario.getCurativePerimeters().size());
        assertEquals(curativeState1, contingencyScenario.getCurativePerimeters().get(0).getRaOptimisationState());
        assertEquals(Set.of(curativeState1), contingencyScenario.getCurativePerimeters().get(0).getAllStates());
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
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        assertEquals(Optional.of(autoState), stateTree.getContingencyScenarios().iterator().next().getAutomatonState());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(1, contingencyScenario.getCurativePerimeters().size());
        assertEquals(curativeState1, contingencyScenario.getCurativePerimeters().get(0).getRaOptimisationState());
        assertEquals(Set.of(curativeState1), contingencyScenario.getCurativePerimeters().get(0).getAllStates());

        // 4.3 Both AUTO and CURATIVE exist, only CURATIVE has RAs
        setUpCustomCracWithAutoInstant(true, false, true, true);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState, autoState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.empty(), contingencyScenario.getAutomatonState());
        assertEquals(1, contingencyScenario.getCurativePerimeters().size());
        assertEquals(curativeState1, contingencyScenario.getCurativePerimeters().get(0).getRaOptimisationState());
        assertEquals(Set.of(curativeState1), contingencyScenario.getCurativePerimeters().get(0).getAllStates());

        // 4.4 Both AUTO and CURATIVE exist and have RAs
        setUpCustomCracWithAutoInstant(true, true, true, true);
        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.of(autoState), contingencyScenario.getAutomatonState());
        assertEquals(1, contingencyScenario.getCurativePerimeters().size());
        assertEquals(curativeState1, contingencyScenario.getCurativePerimeters().get(0).getRaOptimisationState());
        assertEquals(Set.of(curativeState1), contingencyScenario.getCurativePerimeters().get(0).getAllStates());
    }

    @Test
    void testAutomatonStateWithoutAutoCnecs() {
        setUpCustomCracWithAutoInstant(true, true, true, true);
        Set<String> cnecsToRemove = crac.getFlowCnecs(autoState).stream().map(Identifiable::getId).collect(Collectors.toSet());
        crac.removeFlowCnecs(cnecsToRemove);
        assertTrue(crac.getFlowCnecs(autoState).isEmpty());

        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.of(autoState), contingencyScenario.getAutomatonState());
        assertEquals(1, contingencyScenario.getCurativePerimeters().size());
        assertEquals(curativeState1, contingencyScenario.getCurativePerimeters().get(0).getRaOptimisationState());
        assertEquals(Set.of(curativeState1), contingencyScenario.getCurativePerimeters().get(0).getAllStates());
    }

    @Test
    void testCurativeStateWithoutCurativeCnecs() {
        setUpCustomCracWithAutoInstant(true, true, true, true);
        Set<String> cnecsToRemove = crac.getFlowCnecs(curativeState1).stream().map(Identifiable::getId).collect(Collectors.toSet());
        crac.removeFlowCnecs(cnecsToRemove);
        assertTrue(crac.getFlowCnecs(curativeState1).isEmpty());

        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(1, stateTree.getContingencyScenarios().size());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("contingency"), contingencyScenario.getContingency());
        assertEquals(Optional.of(autoState), contingencyScenario.getAutomatonState());
        assertTrue(contingencyScenario.getCurativePerimeters().isEmpty());
    }

    @Test
    void testStateWithoutCnecs() {
        setUpCustomCracWithAutoInstant(true, true, true, true);
        crac.removeFlowCnecs(crac.getFlowCnecs(autoState).stream().map(Identifiable::getId).collect(Collectors.toSet()));
        crac.removeFlowCnecs(crac.getFlowCnecs(curativeState1).stream().map(Identifiable::getId).collect(Collectors.toSet()));
        assertTrue(crac.getFlowCnecs(autoState).isEmpty());
        assertTrue(crac.getFlowCnecs(curativeState1).isEmpty());

        stateTree = new StateTree(crac);
        assertEquals(Set.of(preventiveState, outageState), stateTree.getBasecaseScenario().getAllStates());
        assertEquals(0, stateTree.getContingencyScenarios().size());
    }

    // Tests with all contingency scenarios possible cases

    @Test
    void multiCurativeContingencyScenarioNoArasNoAutoCnecsCase() {
        Crac multipleCurativeInstantsCrac = createCommonMultipleCurativeInstantsCrac();

        StateTree stateTree = new StateTree(multipleCurativeInstantsCrac);

        List<ContingencyScenario> contingencyScenarios = stateTree.getContingencyScenarios().stream().sorted(Comparator.comparing(contingencyScenario -> contingencyScenario.getContingency().getId())).toList();
        assertEquals(8, contingencyScenarios.size());

        List<Perimeter> curativePerimeters;
        ContingencyScenario contingencyScenario;
        List<State> allStates;

        contingencyScenario = contingencyScenarios.get(0);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-01", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(2, curativePerimeters.size());
        assertEquals("co-01 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-01", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());
        assertEquals("co-01 - curative2", curativePerimeters.get(1).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-01", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(1).getAllStates());

        contingencyScenario = contingencyScenarios.get(1);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-02", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-02 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-02 - curative1", allStates.get(0).getId());
        assertEquals("co-02 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(2);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-03", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-03 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-03", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(3);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-04", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-04 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-04", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(4);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-05", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-05 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-05", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(5);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-09", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-09 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-09", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(6);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-10", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-10 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-10 - curative1", allStates.get(0).getId());
        assertEquals("co-10 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(7);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-13", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-13 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-13", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        assertEquals(7, stateTree.getBasecaseScenario().getAllStates().size());
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-05", "curative1");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-06", "curative1");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-06", "curative2");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-07", "curative1");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-08", "curative1");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-14", "curative2");
    }

    @Test
    void multiCurativeContingencyScenarioNoArasAutoCnecsCase() {
        Crac multipleCurativeInstantsCrac = createCommonMultipleCurativeInstantsCrac();
        addAutoCnecsToCrac(multipleCurativeInstantsCrac);

        StateTree stateTree = new StateTree(multipleCurativeInstantsCrac);

        List<ContingencyScenario> contingencyScenarios = stateTree.getContingencyScenarios().stream().sorted(Comparator.comparing(contingencyScenario -> contingencyScenario.getContingency().getId())).toList();
        assertEquals(8, contingencyScenarios.size());

        List<Perimeter> curativePerimeters;
        ContingencyScenario contingencyScenario;
        List<State> allStates;

        contingencyScenario = contingencyScenarios.get(0);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-01", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(2, curativePerimeters.size());
        assertEquals("co-01 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-01", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());
        assertEquals("co-01 - curative2", curativePerimeters.get(1).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-01", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(1).getAllStates());

        contingencyScenario = contingencyScenarios.get(1);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-02", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-02 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-02 - curative1", allStates.get(0).getId());
        assertEquals("co-02 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(2);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-03", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-03 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-03", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(3);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-04", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-04 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-04", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(4);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-05", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-05 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-05", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(5);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-09", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-09 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-09", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(6);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-10", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-10 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-10 - curative1", allStates.get(0).getId());
        assertEquals("co-10 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(7);
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals("co-13", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-13 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-13", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        assertEquals(23, stateTree.getBasecaseScenario().getAllStates().size());
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-05", "curative1");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-06", "curative1");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-06", "curative2");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-07", "curative1");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-08", "curative1");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-14", "curative2");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-01", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-02", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-03", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-04", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-05", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-06", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-07", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-08", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-09", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-10", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-11", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-12", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-13", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-14", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-15", "auto");
        assertCurativeStateInBaseCase(stateTree, multipleCurativeInstantsCrac, "co-16", "auto");
    }

    @Test
    void multiCurativeContingencyScenarioArasNoAutoCnecsCase() {
        Crac multipleCurativeInstantsCrac = createCommonMultipleCurativeInstantsCrac();
        addArasToCrac(multipleCurativeInstantsCrac);

        StateTree stateTree = new StateTree(multipleCurativeInstantsCrac);

        List<ContingencyScenario> contingencyScenarios = stateTree.getContingencyScenarios().stream().sorted(Comparator.comparing(contingencyScenario -> contingencyScenario.getContingency().getId())).toList();
        assertEquals(12, contingencyScenarios.size());

        List<Perimeter> curativePerimeters;
        ContingencyScenario contingencyScenario;
        List<State> allStates;

        contingencyScenario = contingencyScenarios.get(0);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-01 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-01", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(2, curativePerimeters.size());
        assertEquals("co-01 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-01", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());
        assertEquals("co-01 - curative2", curativePerimeters.get(1).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-01", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(1).getAllStates());

        contingencyScenario = contingencyScenarios.get(1);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-02 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-02", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-02 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-02 - curative1", allStates.get(0).getId());
        assertEquals("co-02 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(2);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-03 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-03", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-03 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-03", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(3);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-04 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-04", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-04 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-04", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(4);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-05 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-05", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(2, curativePerimeters.size());
        assertEquals("co-05 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-05", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());
        assertEquals("co-05 - curative2", curativePerimeters.get(1).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-05", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(1).getAllStates());

        contingencyScenario = contingencyScenarios.get(5);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-06 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-06", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-06 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-06 - curative1", allStates.get(0).getId());
        assertEquals("co-06 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(6);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-07 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-07", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-07 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-07", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(7);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-08 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-08", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-08 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-08", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(8);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-09 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-09", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-09 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-09", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(9);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-10 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-10", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-10 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-10 - curative1", allStates.get(0).getId());
        assertEquals("co-10 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(10);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-13 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-13", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-13 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-13", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        assertEquals(1, stateTree.getBasecaseScenario().getAllStates().size());
    }

    @Test
    void multiCurativeContingencyScenarioArasAutoCnecsCase() {
        Crac multipleCurativeInstantsCrac = createCommonMultipleCurativeInstantsCrac();
        addAutoCnecsToCrac(multipleCurativeInstantsCrac);
        addArasToCrac(multipleCurativeInstantsCrac);

        StateTree stateTree = new StateTree(multipleCurativeInstantsCrac);

        List<ContingencyScenario> contingencyScenarios = stateTree.getContingencyScenarios().stream().sorted(Comparator.comparing(contingencyScenario -> contingencyScenario.getContingency().getId())).toList();
        assertEquals(16, contingencyScenarios.size());

        List<Perimeter> curativePerimeters;
        ContingencyScenario contingencyScenario;
        List<State> allStates;

        contingencyScenario = contingencyScenarios.get(0);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-01 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-01", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(2, curativePerimeters.size());
        assertEquals("co-01 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-01", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());
        assertEquals("co-01 - curative2", curativePerimeters.get(1).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-01", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(1).getAllStates());

        contingencyScenario = contingencyScenarios.get(1);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-02 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-02", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-02 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-02 - curative1", allStates.get(0).getId());
        assertEquals("co-02 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(2);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-03 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-03", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-03 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-03", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(3);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-04 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-04", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-04 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-04", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(4);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-05 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-05", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(2, curativePerimeters.size());
        assertEquals("co-05 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-05", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());
        assertEquals("co-05 - curative2", curativePerimeters.get(1).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-05", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(1).getAllStates());

        contingencyScenario = contingencyScenarios.get(5);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-06 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-06", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-06 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-06 - curative1", allStates.get(0).getId());
        assertEquals("co-06 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(6);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-07 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-07", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-07 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-07", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(7);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-08 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-08", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-08 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-08", multipleCurativeInstantsCrac.getInstant("curative1"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(8);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-09 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-09", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-09 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-09", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(9);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-10 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-10", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-10 - curative1", curativePerimeters.get(0).getRaOptimisationState().getId());
        allStates = curativePerimeters.get(0).getAllStates().stream().sorted(Comparator.comparing(State::getId)).toList();
        assertEquals(2, allStates.size());
        assertEquals("co-10 - curative1", allStates.get(0).getId());
        assertEquals("co-10 - curative2", allStates.get(1).getId());

        contingencyScenario = contingencyScenarios.get(10);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-11 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-11", contingencyScenario.getContingency().getId());
        assertTrue(contingencyScenario.getCurativePerimeters().isEmpty());

        contingencyScenario = contingencyScenarios.get(11);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-12 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-12", contingencyScenario.getContingency().getId());
        assertTrue(contingencyScenario.getCurativePerimeters().isEmpty());

        contingencyScenario = contingencyScenarios.get(12);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-13 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-13", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-13 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-13", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(13);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-14 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-14", contingencyScenario.getContingency().getId());
        curativePerimeters = contingencyScenario
            .getCurativePerimeters()
            .stream()
            .sorted(Comparator.comparing(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
            .toList();
        assertEquals(1, curativePerimeters.size());
        assertEquals("co-14 - curative2", curativePerimeters.get(0).getRaOptimisationState().getId());
        assertEquals(Set.of(multipleCurativeInstantsCrac.getState("co-14", multipleCurativeInstantsCrac.getInstant("curative2"))), curativePerimeters.get(0).getAllStates());

        contingencyScenario = contingencyScenarios.get(14);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-15 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-15", contingencyScenario.getContingency().getId());
        assertTrue(contingencyScenario.getCurativePerimeters().isEmpty());

        contingencyScenario = contingencyScenarios.get(15);
        assertTrue(contingencyScenario.getAutomatonState().isPresent());
        assertEquals("co-16 - auto", contingencyScenario.getAutomatonState().get().getId());
        assertEquals("co-16", contingencyScenario.getContingency().getId());
        assertTrue(contingencyScenario.getCurativePerimeters().isEmpty());

        assertEquals(1, stateTree.getBasecaseScenario().getAllStates().size());
    }

    private static Crac createCommonMultipleCurativeInstantsCrac() {
        Crac multipleCurativeCrac = new CracImplFactory().create("multipleCurativeCrac", "multipleCurativeCrac");

        multipleCurativeCrac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative1", InstantKind.CURATIVE)
            .newInstant("curative2", InstantKind.CURATIVE);

        addContingencyToCrac(multipleCurativeCrac, "co-01");
        addContingencyToCrac(multipleCurativeCrac, "co-02");
        addContingencyToCrac(multipleCurativeCrac, "co-03");
        addContingencyToCrac(multipleCurativeCrac, "co-04");
        addContingencyToCrac(multipleCurativeCrac, "co-05");
        addContingencyToCrac(multipleCurativeCrac, "co-06");
        addContingencyToCrac(multipleCurativeCrac, "co-07");
        addContingencyToCrac(multipleCurativeCrac, "co-08");
        addContingencyToCrac(multipleCurativeCrac, "co-09");
        addContingencyToCrac(multipleCurativeCrac, "co-10");
        addContingencyToCrac(multipleCurativeCrac, "co-11");
        addContingencyToCrac(multipleCurativeCrac, "co-12");
        addContingencyToCrac(multipleCurativeCrac, "co-13");
        addContingencyToCrac(multipleCurativeCrac, "co-14");
        addContingencyToCrac(multipleCurativeCrac, "co-15");
        addContingencyToCrac(multipleCurativeCrac, "co-16");

        addFlowCnecToCrac(multipleCurativeCrac, "preventive", null);

        addFlowCnecToCrac(multipleCurativeCrac, "curative1", "co-01");
        addFlowCnecToCrac(multipleCurativeCrac, "curative1", "co-02");
        addFlowCnecToCrac(multipleCurativeCrac, "curative1", "co-03");
        addFlowCnecToCrac(multipleCurativeCrac, "curative1", "co-04");
        addFlowCnecToCrac(multipleCurativeCrac, "curative1", "co-05");
        addFlowCnecToCrac(multipleCurativeCrac, "curative1", "co-06");
        addFlowCnecToCrac(multipleCurativeCrac, "curative1", "co-07");
        addFlowCnecToCrac(multipleCurativeCrac, "curative1", "co-08");
        addFlowCnecToCrac(multipleCurativeCrac, "curative2", "co-01");
        addFlowCnecToCrac(multipleCurativeCrac, "curative2", "co-02");
        addFlowCnecToCrac(multipleCurativeCrac, "curative2", "co-05");
        addFlowCnecToCrac(multipleCurativeCrac, "curative2", "co-06");
        addFlowCnecToCrac(multipleCurativeCrac, "curative2", "co-09");
        addFlowCnecToCrac(multipleCurativeCrac, "curative2", "co-10");
        addFlowCnecToCrac(multipleCurativeCrac, "curative2", "co-13");
        addFlowCnecToCrac(multipleCurativeCrac, "curative2", "co-14");

        addTopologicalActionToCrac(multipleCurativeCrac, "curative1", "co-01");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative1", "co-02");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative1", "co-03");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative1", "co-04");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative1", "co-09");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative1", "co-10");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative1", "co-11");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative1", "co-12");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative2", "co-01");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative2", "co-03");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative2", "co-05");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative2", "co-07");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative2", "co-09");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative2", "co-11");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative2", "co-13");
        addTopologicalActionToCrac(multipleCurativeCrac, "curative2", "co-15");
        return multipleCurativeCrac;
    }

    private static void addAutoCnecsToCrac(Crac crac) {
        addFlowCnecToCrac(crac, "auto", "co-01");
        addFlowCnecToCrac(crac, "auto", "co-02");
        addFlowCnecToCrac(crac, "auto", "co-03");
        addFlowCnecToCrac(crac, "auto", "co-04");
        addFlowCnecToCrac(crac, "auto", "co-05");
        addFlowCnecToCrac(crac, "auto", "co-06");
        addFlowCnecToCrac(crac, "auto", "co-07");
        addFlowCnecToCrac(crac, "auto", "co-08");
        addFlowCnecToCrac(crac, "auto", "co-09");
        addFlowCnecToCrac(crac, "auto", "co-10");
        addFlowCnecToCrac(crac, "auto", "co-11");
        addFlowCnecToCrac(crac, "auto", "co-12");
        addFlowCnecToCrac(crac, "auto", "co-13");
        addFlowCnecToCrac(crac, "auto", "co-14");
        addFlowCnecToCrac(crac, "auto", "co-15");
        addFlowCnecToCrac(crac, "auto", "co-16");
    }

    private static void addArasToCrac(Crac crac) {
        addTopologicalActionToCrac(crac, "auto", "co-01");
        addTopologicalActionToCrac(crac, "auto", "co-02");
        addTopologicalActionToCrac(crac, "auto", "co-03");
        addTopologicalActionToCrac(crac, "auto", "co-04");
        addTopologicalActionToCrac(crac, "auto", "co-05");
        addTopologicalActionToCrac(crac, "auto", "co-06");
        addTopologicalActionToCrac(crac, "auto", "co-07");
        addTopologicalActionToCrac(crac, "auto", "co-08");
        addTopologicalActionToCrac(crac, "auto", "co-09");
        addTopologicalActionToCrac(crac, "auto", "co-10");
        addTopologicalActionToCrac(crac, "auto", "co-11");
        addTopologicalActionToCrac(crac, "auto", "co-12");
        addTopologicalActionToCrac(crac, "auto", "co-13");
        addTopologicalActionToCrac(crac, "auto", "co-14");
        addTopologicalActionToCrac(crac, "auto", "co-15");
        addTopologicalActionToCrac(crac, "auto", "co-16");
    }

    private static void addContingencyToCrac(Crac crac, String contingencyId) {
        crac.newContingency()
            .withId(contingencyId)
            .withContingencyElement("co-line", ContingencyElementType.LINE)
            .add();
    }

    private static void addFlowCnecToCrac(Crac crac, String instantId, String contingencyId) {
        crac.newFlowCnec()
            .withId(String.format("FlowCNEC - %s - %s", contingencyId, instantId))
            .withInstant(instantId)
            .withContingency(contingencyId)
            .withNetworkElement("line")
            .withNominalVoltage(400)
            .newThreshold()
                .withSide(Side.LEFT)
                .withUnit(Unit.AMPERE)
                .withMax(500d)
                .withMin(-500d)
                .add()
            .add();
    }

    private static void addTopologicalActionToCrac(Crac crac, String instantId, String contingencyId) {
        crac.newNetworkAction()
            .withId(String.format("onContingency topo - %s - %s", contingencyId, instantId))
            .newSwitchAction()
                .withActionType(ActionType.OPEN)
                .withNetworkElement("switch")
                .add()
            .newOnContingencyStateUsageRule()
                .withInstant(instantId)
                .withContingency(contingencyId)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .add();
    }

    private static void assertCurativeStateInBaseCase(StateTree stateTree, Crac crac, String contingencyId, String instantId) {
        assertTrue(stateTree.getBasecaseScenario().getAllStates().contains(crac.getState(contingencyId, crac.getInstant(instantId))));
    }
}
