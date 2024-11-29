/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CastorSecondPreventiveTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private Network network;
    private State state1;
    private State state2;
    private RangeAction<?> ra1;
    private RangeAction<?> ra2;
    private RangeAction<?> ra3;
    private RangeAction<?> ra4;
    private RangeAction<?> ra5;
    private RangeAction<?> ra6;
    private RangeAction<?> ra7;
    private RangeAction<?> ra8;
    private RangeAction<?> ra9;
    private NetworkAction na1;
    private Instant preventiveInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    private State mockState(Instant instant) {
        State state = Mockito.mock(State.class);
        when(state.getInstant()).thenReturn(instant);
        return state;
    }

    private OptimizationResult mockOptimizationResult(Set<NetworkAction> activatedNetworkActions) {
        OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getActivatedNetworkActions()).thenReturn(activatedNetworkActions);
        return optimizationResult;
    }

    private OptimizationResult mockOptimizationResult(Set<RangeAction<?>> activatedRangeActions, State state) {
        OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getActivatedRangeActions(state)).thenReturn(activatedRangeActions);
        return optimizationResult;
    }

    private void setUpCracWithRAs() {
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withContingencyElement("contingency1-ne", ContingencyElementType.LINE)
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withContingencyElement("contingency2-ne", ContingencyElementType.LINE)
            .add();
        crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("cnec-ne")
            .withContingency("contingency1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withNominalVoltage(220.)
            .newThreshold().withSide(TwoSides.TWO).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        // ra1 : preventive only
        ra1 = crac.newPstRangeAction()
            .withId("ra1")
            .withNetworkElement("ra1-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNDEFINED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra2 : preventive and curative
        ra2 = crac.newPstRangeAction()
            .withId("ra2")
            .withNetworkElement("ra2-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra3 : preventive and curative
        ra3 = crac.newPstRangeAction()
            .withId("ra3")
            .withNetworkElement("ra3-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTapRange().withMaxTap(100).withMinTap(-100).withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra4 : preventive only, but with same NetworkElement as ra5
        ra4 = crac.newPstRangeAction()
            .withId("ra4")
            .withNetworkElement("ra4-ne1")
            .withNetworkElement("ra4-ne2")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra5 : curative only, but with same NetworkElement as ra4
        ra5 = crac.newPstRangeAction()
            .withId("ra5")
            .withNetworkElement("ra4-ne1")
            .withNetworkElement("ra4-ne2")
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra6 : preventive and curative (onFlowConstraint)
        ra6 = crac.newPstRangeAction()
            .withId("ra6")
            .withNetworkElement("ra6-ne")
            .withOperator("FR")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnConstraintUsageRule().withCnec("cnec").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra7 : auto only
        ra7 = crac.newPstRangeAction()
            .withId("ra7")
            .withNetworkElement("ra7-ne")
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .withSpeed(1)
            .add();
        // ra8 : preventive and auto
        ra8 = crac.newPstRangeAction()
            .withId("ra8")
            .withNetworkElement("ra8-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .withSpeed(2)
            .add();
        // ra9 : preventive only, but with same NetworkElement as ra8
        ra9 = crac.newPstRangeAction()
            .withId("ra9")
            .withNetworkElement("ra8-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra10 : preventive only, counter trade
        crac.newCounterTradeRangeAction()
            .withId("ra10")
            .withExportingCountry(Country.FR)
            .withImportingCountry(Country.DE)
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNDEFINED).add()
            .newRange().withMin(-1000).withMax(1000).add()
            .add();

        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
            .withId("na1")
            .newSwitchAction().withNetworkElement("na1-ne").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        state1 = crac.getState(contingency1, curativeInstant);
        state2 = crac.getState(contingency2, curativeInstant);
    }

    private void setUpCracWithRealRAs(boolean curative) {
        network = NetworkImportsUtil.import12NodesNetwork();
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger();
        HashMap<Integer, Double> tapToAngleConversionMap = new HashMap<>();
        phaseTapChanger.getAllSteps().forEach((stepInt, step) -> tapToAngleConversionMap.put(stepInt, step.getAlpha()));
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withContingencyElement("contingency1-ne", ContingencyElementType.LINE)
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withContingencyElement("contingency2-ne", ContingencyElementType.LINE)
            .add();
        // ra1 : preventive only
        PstRangeActionAdder adder = crac.newPstRangeAction()
            .withId("ra1")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(0).withTapToAngleConversionMap(tapToAngleConversionMap)
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        if (curative) {
            adder.newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        }
        ra1 = adder.add();
        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
            .withId("na1")
            .newTerminalsConnectionAction().withNetworkElement("BBE1AA1  BBE2AA1  1").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        state1 = crac.getState(contingency1, curativeInstant);
        state2 = crac.getState(contingency2, curativeInstant);
    }

    @BeforeEach
    void setUp() {
        crac = Mockito.mock(Crac.class);
        curativeInstant = Mockito.mock(Instant.class);
        when(crac.getLastInstant()).thenReturn(curativeInstant);
        preventiveInstant = Mockito.mock(Instant.class);
        autoInstant = Mockito.mock(Instant.class);
    }

    @Test
    void testShouldRunSecondPreventiveRaoSimple() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, parameters, network, null, null, null);

        // No SearchTreeRaoParameters extension
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));

        // Deactivated in parameters
        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED);
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));

        // PreventiveStopCriterion.SECURE, secure case
        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        Mockito.doReturn(-1.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(-10.).when(optimizationResult2).getFunctionalCost();
        Mockito.doReturn(0.).when(optimizationResult1).getVirtualCost();
        Mockito.doReturn(0.).when(optimizationResult2).getVirtualCost();
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 1
        Mockito.doReturn(0.).when(optimizationResult1).getFunctionalCost();
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 2
        Mockito.doReturn(5.).when(optimizationResult1).getFunctionalCost();
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 3
        Mockito.doReturn(-10.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(9.).when(optimizationResult1).getVirtualCost();
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));
    }

    private void setCost(OptimizationResult optimizationResultMock, double cost) {
        when(optimizationResultMock.getFunctionalCost()).thenReturn(cost);
        when(optimizationResultMock.getVirtualCost()).thenReturn(0.);
        when(optimizationResultMock.getCost()).thenReturn(cost);
    }

    @Test
    void testShouldRunSecondPreventiveRaoAdvanced() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        RaoResult postFirstPreventiveRaoResult = Mockito.mock(RaoResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(10.);
        parameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        when(preventiveResult.getCost()).thenReturn(-500.);
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, parameters, network, null, null, null);

        // PreventiveStopCriterion.MIN_OBJECTIVE
        parameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE);
        setCost(preventiveResult, -100.);
        // case 1 : final cost is better than preventive (cost < preventive cost - minObjImprovement)
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-200.);
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, postFirstPreventiveRaoResult, 0));
        // case 2 : final cost = preventive cost - minObjImprovement
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-110.);
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, postFirstPreventiveRaoResult, 0));
        // case 3 : final cost > preventive cost - minObjImprovement
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-109.);
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, postFirstPreventiveRaoResult, 0));
    }

    @Test
    void testShouldRunSecondPreventiveRaoTime() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        // Default objective function parameters are enough for SecondPreventiveRaoParameters to be true if there is enough time
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, parameters, network, null, null, java.time.Instant.now().plusSeconds(200));

        // Enough time
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 100));
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 199));

        // Not enough time
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 201));
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 400));
    }

    @Test
    void testShouldRunSecondPreventiveRaoCostIncrease() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE);
        // Default objective function parameters are enough for SecondPreventiveRaoParameters to be true if cost at curative allows it

        RaoResult postFirstRaoResult = Mockito.mock(RaoResult.class);
        when(postFirstRaoResult.getCost(null)).thenReturn(-100.);
        when(postFirstRaoResult.getCost(preventiveInstant)).thenReturn(-10.);
        when(postFirstRaoResult.getCost(curativeInstant)).thenReturn(-120.);

        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, parameters, network, null, null, null);

        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, postFirstRaoResult, 0));

        when(postFirstRaoResult.getCost(curativeInstant)).thenReturn(-100.);
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, postFirstRaoResult, 0));

        when(postFirstRaoResult.getCost(curativeInstant)).thenReturn(-95.);
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, postFirstRaoResult, 0));
    }

    @Test
    void testGetRangeActionsExcludedFromSecondPreventive() {
        setUpCracWithRAs();
        OptimizationResult firstPreventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
        State preventiveState = crac.getPreventiveState();
        // ra9 has different taps than ra8.
        when(firstPreventiveResult.getOptimizedSetpoint(ra9, preventiveState)).thenReturn(2.);
        crac.newRaUsageLimits(autoInstant.getId()).withMaxRa(0).add();
        crac.newRaUsageLimits(curativeInstant.getId()).withMaxRaPerTso(new HashMap<>(Map.of("FR", 0))).add();
        Map<State, OptimizationResult> contingencyResult = new HashMap<>();
        crac.getStates().forEach(state -> {
            if (!state.isPreventive()) {
                contingencyResult.put(state, optimizationResult);
            }
        });
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, null, network, null, null, null);

        Set<RangeAction<?>> rangeActionsExcludedFrom2P = castorSecondPreventive.getRangeActionsExcludedFromSecondPreventive(firstPreventiveResult, contingencyResult);

        assertEquals(6, rangeActionsExcludedFrom2P.size());
        assertFalse(rangeActionsExcludedFrom2P.contains(ra1)); // Should not be excluded as it's preventive only.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra2)); // Should be excluded as it's UNAVAILABLE for preventive.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra5)); // Should be excluded as it's not preventive.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra7)); // Should be excluded as it's not preventive.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra3));  // Should be excluded as it has a range limitation RELATIVE_TO_PREVIOUS_INSTANT.

        assertFalse(rangeActionsExcludedFrom2P.contains(ra9)); // It shares the same network elements as ra8 but their tap are different. It should not be excluded.

        assertTrue(rangeActionsExcludedFrom2P.contains(ra6));  // It has the same taps in preventive and in curative. The RA belongs to french TSO and there are ra usage limuts on this TSO : It should be excluded.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra8));  // It has the same taps in preventive and auto. As there are RaUsageLimits for this instant, it should be excluded.
        assertFalse(rangeActionsExcludedFrom2P.contains(ra4)); // It has the same network elements as ra5 and their taps are the same. As it doesn't belong to frenchTSO : it should not be excluded.

    }

    @Test
    void testGetAppliedRemedialActionsInCurative() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);

        String pstNeId = "BBE2AA1  BBE3AA1  1";
        String naNeId = "BBE1AA1  BBE2AA1  1";

        setUpCracWithRealRAs(true);
        Mockito.doReturn(0.).when(prePerimeterResult).getSetpoint(ra1);

        OptimizationResult optimResult1 = Mockito.mock(OptimizationResult.class);
        Mockito.doReturn(Set.of(ra1)).when(optimResult1).getActivatedRangeActions(Mockito.any());
        Mockito.doReturn(-1.5583491325378418).when(optimResult1).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of()).when(optimResult1).getActivatedNetworkActions();

        OptimizationResult optimResult2 = Mockito.mock(OptimizationResult.class);
        Mockito.doReturn(Set.of(ra1)).when(optimResult1).getActivatedRangeActions(Mockito.any());
        Mockito.doReturn(0.).when(optimResult2).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(na1)).when(optimResult2).getActivatedNetworkActions();

        Map<State, OptimizationResult> curativeResults = Map.of(state1, optimResult1, state2, optimResult2);
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, null, network, null, null, null);

        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        castorSecondPreventive.addAppliedNetworkActionsPostContingency(Set.of(autoInstant), appliedRemedialActions, curativeResults);
        castorSecondPreventive.addAppliedNetworkActionsPostContingency(Set.of(curativeInstant), appliedRemedialActions, curativeResults);

        // do not apply network action
        // do not apply range action as it was not yet added to applied RAs
        appliedRemedialActions.applyOnNetwork(state1, network);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertTrue(network.getLine(naNeId).getTerminal1().isConnected());

        // reset network
        network = NetworkImportsUtil.import12NodesNetwork();

        // apply only network action
        appliedRemedialActions.applyOnNetwork(state2, network);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine(naNeId).getTerminal1().isConnected());

        // add range action
        castorSecondPreventive.addAppliedRangeActionsPostContingency(Set.of(autoInstant), appliedRemedialActions, curativeResults);
        castorSecondPreventive.addAppliedRangeActionsPostContingency(Set.of(curativeInstant), appliedRemedialActions, curativeResults);

        // apply also range action
        appliedRemedialActions.applyOnNetwork(state1, network);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine(naNeId).getTerminal1().isConnected());
    }

    @Test
    void testApplyPreventiveResultsForCurativeRangeActions() {
        OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
        String pstNeId = "BBE2AA1  BBE3AA1  1";

        setUpCracWithRealRAs(false);
        Mockito.doReturn(-1.5583491325378418).when(optimizationResult).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(ra1)).when(optimizationResult).getActivatedRangeActions(Mockito.any());
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, null, network, null, null, null);
        castorSecondPreventive.applyPreventiveResultsForAutoOrCurativeRangeActions(optimizationResult);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());

        setUpCracWithRealRAs(true);
        Mockito.doReturn(-1.5583491325378418).when(optimizationResult).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(ra1)).when(optimizationResult).getActivatedRangeActions(Mockito.any());
        castorSecondPreventive = new CastorSecondPreventive(crac, null, network, null, null, null);
        castorSecondPreventive.applyPreventiveResultsForAutoOrCurativeRangeActions(optimizationResult);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    void testAddAppliedNetworkActionsPostContingency() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        Instant instant1 = Mockito.mock(Instant.class);
        when(instant1.isAuto()).thenReturn(true);
        Instant instant2 = Mockito.mock(Instant.class);
        when(instant2.isCurative()).thenReturn(true);

        State state11 = mockState(instant1);
        State state12 = mockState(instant1);
        State state21 = mockState(instant2);
        State state22 = mockState(instant2);

        NetworkAction na111 = Mockito.mock(NetworkAction.class);
        NetworkAction na112 = Mockito.mock(NetworkAction.class);
        NetworkAction na121 = Mockito.mock(NetworkAction.class);
        NetworkAction na211 = Mockito.mock(NetworkAction.class);
        NetworkAction na221 = Mockito.mock(NetworkAction.class);
        NetworkAction na222 = Mockito.mock(NetworkAction.class);

        OptimizationResult optimizationResult11 = mockOptimizationResult(Set.of(na111, na112));
        OptimizationResult optimizationResult12 = mockOptimizationResult(Set.of(na121));
        OptimizationResult optimizationResult21 = mockOptimizationResult(Set.of(na211));
        OptimizationResult optimizationResult22 = mockOptimizationResult(Set.of(na221, na222));

        Map<State, OptimizationResult> postContingencyResults = Map.of(state11, optimizationResult11, state12, optimizationResult12,
            state21, optimizationResult21, state22, optimizationResult22);
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, null, network, null, null, null);

        castorSecondPreventive.addAppliedNetworkActionsPostContingency(Set.of(), appliedRemedialActions, postContingencyResults);

        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state11).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state12).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state21).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state22).isEmpty());

        castorSecondPreventive.addAppliedNetworkActionsPostContingency(Set.of(instant1), appliedRemedialActions, postContingencyResults);
        assertEquals(Set.of(na111, na112), appliedRemedialActions.getAppliedNetworkActions(state11));
        assertEquals(Set.of(na121), appliedRemedialActions.getAppliedNetworkActions(state12));
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state21).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state22).isEmpty());

        castorSecondPreventive.addAppliedNetworkActionsPostContingency(Set.of(instant2), appliedRemedialActions, postContingencyResults);
        assertEquals(Set.of(na111, na112), appliedRemedialActions.getAppliedNetworkActions(state11));
        assertEquals(Set.of(na121), appliedRemedialActions.getAppliedNetworkActions(state12));
        assertEquals(Set.of(na211), appliedRemedialActions.getAppliedNetworkActions(state21));
        assertEquals(Set.of(na221, na222), appliedRemedialActions.getAppliedNetworkActions(state22));
    }

    @Test
    void testAddAppliedRangeActionsPostContingency() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        Instant instant1 = Mockito.mock(Instant.class);
        when(instant1.isAuto()).thenReturn(true);
        Instant instant2 = Mockito.mock(Instant.class);
        when(instant2.isCurative()).thenReturn(true);

        State state11 = mockState(instant1);
        State state12 = mockState(instant1);
        State state21 = mockState(instant2);
        State state22 = mockState(instant2);

        RangeAction<?> ra111 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra112 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra121 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra211 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra221 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra222 = Mockito.mock(RangeAction.class);

        OptimizationResult optimizationResult11 = mockOptimizationResult(Set.of(ra111, ra112), state11);
        OptimizationResult optimizationResult12 = mockOptimizationResult(Set.of(ra121), state12);
        OptimizationResult optimizationResult21 = mockOptimizationResult(Set.of(ra211), state21);
        OptimizationResult optimizationResult22 = mockOptimizationResult(Set.of(ra221, ra222), state22);

        Map<State, OptimizationResult> postContingencyResults = Map.of(state11, optimizationResult11, state12, optimizationResult12,
            state21, optimizationResult21, state22, optimizationResult22);
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, null, network, null, null, null);

        castorSecondPreventive.addAppliedRangeActionsPostContingency(Set.of(), appliedRemedialActions, postContingencyResults);

        assertTrue(appliedRemedialActions.getAppliedRangeActions(state11).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state12).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state21).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state22).isEmpty());

        castorSecondPreventive.addAppliedRangeActionsPostContingency(Set.of(instant1), appliedRemedialActions, postContingencyResults);
        assertEquals(Map.of(ra111, 0., ra112, 0.), appliedRemedialActions.getAppliedRangeActions(state11));
        assertEquals(Map.of(ra121, 0.), appliedRemedialActions.getAppliedRangeActions(state12));
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state21).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state22).isEmpty());

        castorSecondPreventive.addAppliedRangeActionsPostContingency(Set.of(instant2), appliedRemedialActions, postContingencyResults);
        assertEquals(Map.of(ra111, 0., ra112, 0.), appliedRemedialActions.getAppliedRangeActions(state11));
        assertEquals(Map.of(ra121, 0.), appliedRemedialActions.getAppliedRangeActions(state12));
        assertEquals(Map.of(ra211, 0.), appliedRemedialActions.getAppliedRangeActions(state21));
        assertEquals(Map.of(ra221, 0., ra222, 0.), appliedRemedialActions.getAppliedRangeActions(state22));
    }
}
