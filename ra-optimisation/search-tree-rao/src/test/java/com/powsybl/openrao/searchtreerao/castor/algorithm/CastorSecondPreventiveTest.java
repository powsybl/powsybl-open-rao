/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters.getSecondPreventiveHintFromFirstPreventiveRao;
import static com.powsybl.openrao.searchtreerao.castor.algorithm.CastorSecondPreventive.SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
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
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        if (curative) {
            adder.newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).add();
        }
        ra1 = adder.add();
        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
            .withId("na1")
            .newTerminalsConnectionAction().withNetworkElement("BBE1AA1  BBE2AA1  1").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).add()
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
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);

        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postPreventiveResult = Mockito.mock(PostPerimeterResult.class);
        when(postPreventiveResult.optimizationResult()).thenReturn(preventiveResult);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postOptimizationResult1 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult1.optimizationResult()).thenReturn(optimizationResult1);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postOptimizationResult2 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult2.optimizationResult()).thenReturn(optimizationResult2);

        Collection<PostPerimeterResult> curativeResults = Set.of(postOptimizationResult1, postOptimizationResult2);
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, parameters, network, null, null, null);

        // No SearchTreeRaoParameters extension
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));

        // Deactivated in parameters
        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED);
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));

        // PreventiveStopCriterion.SECURE, secure case
        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        doReturn(-1.).when(optimizationResult1).getFunctionalCost();
        doReturn(-10.).when(optimizationResult2).getFunctionalCost();
        doReturn(0.).when(optimizationResult1).getVirtualCost();
        doReturn(0.).when(optimizationResult2).getVirtualCost();
        assertFalse(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 1
        doReturn(0.).when(optimizationResult1).getFunctionalCost();
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 2
        doReturn(5.).when(optimizationResult1).getFunctionalCost();
        assertTrue(castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, curativeResults, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 3
        doReturn(-10.).when(optimizationResult1).getFunctionalCost();
        doReturn(9.).when(optimizationResult1).getVirtualCost();
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
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        RaoResult postFirstPreventiveRaoResult = Mockito.mock(RaoResult.class);

        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postPreventiveResult = Mockito.mock(PostPerimeterResult.class);
        when(postPreventiveResult.optimizationResult()).thenReturn(preventiveResult);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postOptimizationResult1 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult1.optimizationResult()).thenReturn(optimizationResult1);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postOptimizationResult2 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult2.optimizationResult()).thenReturn(optimizationResult2);

        Collection<PostPerimeterResult> curativeResults = Set.of(postOptimizationResult1, postOptimizationResult2);

        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        searchTreeParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(10.);
        parameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        when(preventiveResult.getCost()).thenReturn(-500.);
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, parameters, network, null, null, null);

        // PreventiveStopCriterion.MIN_OBJECTIVE
        parameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
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
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);

        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postPreventiveResult = Mockito.mock(PostPerimeterResult.class);
        when(postPreventiveResult.optimizationResult()).thenReturn(preventiveResult);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postOptimizationResult1 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult1.optimizationResult()).thenReturn(optimizationResult1);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postOptimizationResult2 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult2.optimizationResult()).thenReturn(optimizationResult2);

        Collection<PostPerimeterResult> curativeResults = Set.of(postOptimizationResult1, postOptimizationResult2);

        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
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
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);

        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postPreventiveResult = Mockito.mock(PostPerimeterResult.class);
        when(postPreventiveResult.optimizationResult()).thenReturn(preventiveResult);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postOptimizationResult1 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult1.optimizationResult()).thenReturn(optimizationResult1);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        PostPerimeterResult postOptimizationResult2 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult2.optimizationResult()).thenReturn(optimizationResult2);

        Collection<PostPerimeterResult> curativeResults = Set.of(postOptimizationResult1, postOptimizationResult2);

        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE);
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
    void testGetAppliedRemedialActionsInCurative() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);

        String pstNeId = "BBE2AA1  BBE3AA1  1";
        String naNeId = "BBE1AA1  BBE2AA1  1";

        setUpCracWithRealRAs(true);
        doReturn(0.).when(prePerimeterResult).getSetpoint(ra1);

        OptimizationResult optimResult1 = Mockito.mock(OptimizationResult.class);
        doReturn(Set.of(ra1)).when(optimResult1).getActivatedRangeActions(any());
        doReturn(-1.5583491325378418).when(optimResult1).getOptimizedSetpoint(eq(ra1), any());
        doReturn(Set.of()).when(optimResult1).getActivatedNetworkActions();
        PostPerimeterResult postOptimizationResult1 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult1.optimizationResult()).thenReturn(optimResult1);

        OptimizationResult optimResult2 = Mockito.mock(OptimizationResult.class);
        doReturn(Set.of(ra1)).when(optimResult1).getActivatedRangeActions(any());
        doReturn(0.).when(optimResult2).getOptimizedSetpoint(eq(ra1), any());
        doReturn(Set.of(na1)).when(optimResult2).getActivatedNetworkActions();
        PostPerimeterResult postOptimizationResult2 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult2.optimizationResult()).thenReturn(optimResult2);

        Map<State, PostPerimeterResult> curativeResults = Map.of(state1, postOptimizationResult1, state2, postOptimizationResult2);
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

        PostPerimeterResult postOptimizationResult11 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult11.optimizationResult()).thenReturn(optimizationResult11);
        PostPerimeterResult postOptimizationResult12 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult12.optimizationResult()).thenReturn(optimizationResult12);
        PostPerimeterResult postOptimizationResult21 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult21.optimizationResult()).thenReturn(optimizationResult21);
        PostPerimeterResult postOptimizationResult22 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult22.optimizationResult()).thenReturn(optimizationResult22);

        Map<State, PostPerimeterResult> postContingencyResults = Map.of(state11, postOptimizationResult11, state12, postOptimizationResult12,
            state21, postOptimizationResult21, state22, postOptimizationResult22);
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

        PostPerimeterResult postOptimizationResult11 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult11.optimizationResult()).thenReturn(optimizationResult11);
        PostPerimeterResult postOptimizationResult12 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult12.optimizationResult()).thenReturn(optimizationResult12);
        PostPerimeterResult postOptimizationResult21 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult21.optimizationResult()).thenReturn(optimizationResult21);
        PostPerimeterResult postOptimizationResult22 = Mockito.mock(PostPerimeterResult.class);
        when(postOptimizationResult22.optimizationResult()).thenReturn(optimizationResult22);

        Map<State, PostPerimeterResult> postContingencyResults = Map.of(state11, postOptimizationResult11, state12, postOptimizationResult12,
            state21, postOptimizationResult21, state22, postOptimizationResult22);
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

    @Test
    void testGetEmptyNetworkActionCombinationHintFrom1stPreventive() {
        // Test that when hintFromFirstPreventiveRao is true
        // network action combination hints are only passed to the 2nd PRAO if the 1st PRAO actually activated network actions.
        // We must not provide an empty NetworkActionCombination, as it would cause the SearchTree
        // to redundantly re-evaluate the optimal network action from the previous depth at each search tree depth.

        // Setup context and data
        setUpCracWithRealRAs(false);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);

        RaoParameters raoParameters = new RaoParameters();
        OpenRaoSearchTreeParameters stExtension = new OpenRaoSearchTreeParameters();
        stExtension.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(true);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, stExtension);

        StateTree stateTree = Mockito.mock(StateTree.class);
        when(stateTree.getOperatorsNotSharingCras()).thenReturn(Set.of());

        PrePerimeterResult initialOutput = Mockito.mock(PrePerimeterResult.class);
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        OptimizationResult firstPreventiveResult = Mockito.mock(OptimizationResult.class);

        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, raoParameters, network, stateTree, null, null);

        // Prepare Mock SearchTree behavior
        OptimizationResult mockResult = Mockito.mock(OptimizationResult.class);
        CompletableFuture<OptimizationResult> futureResult = CompletableFuture.completedFuture(mockResult);
        when(mockResult.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        when(mockResult.getActivatedRangeActions(any())).thenReturn(Collections.emptySet());

        AtomicReference<SearchTreeParameters> capturedParameters = new AtomicReference<>();

        // Use mockConstruction to intercept "new SearchTree(...)"
        try (MockedConstruction<SearchTree> mockedSearchTree = Mockito.mockConstruction(SearchTree.class,
            (mock, context) -> {
                capturedParameters.set((SearchTreeParameters) context.arguments().get(1));
                when(mock.run()).thenReturn(futureResult);
            })) {

            // 1st preventive activated one network action
            when(firstPreventiveResult.getActivatedNetworkActions()).thenReturn(Set.of(na1));
            castorSecondPreventive.optimizeSecondPreventivePerimeter(initialOutput, prePerimeterResult, firstPreventiveResult, new AppliedRemedialActions());
            assertEquals(1,capturedParameters.get().getNetworkActionParameters().getNetworkActionCombinations().size());
            assertEquals(Set.of(na1), capturedParameters.get().getNetworkActionParameters().getNetworkActionCombinations().get(0).getNetworkActionSet());

            // 1st preventive activated no network action
            when(firstPreventiveResult.getActivatedNetworkActions()).thenReturn(Set.of());
            castorSecondPreventive.optimizeSecondPreventivePerimeter(initialOutput, prePerimeterResult, firstPreventiveResult, new AppliedRemedialActions());
            assertTrue(capturedParameters.get().getNetworkActionParameters().getNetworkActionCombinations().isEmpty());
        }
    }
}
