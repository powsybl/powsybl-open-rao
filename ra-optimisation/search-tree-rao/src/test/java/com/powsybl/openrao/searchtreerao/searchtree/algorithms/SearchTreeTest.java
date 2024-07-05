/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.commons.logs.TechnicalLogs;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.NetworkActionParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.SearchTreeResult;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.logRangeActions;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class SearchTreeTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;

    private SearchTree searchTree;

    private SearchTreeInput searchTreeInput;

    private Network network;
    private final State optimizedState = Mockito.mock(State.class);
    private OptimizationPerimeter optimizationPerimeter;
    private NetworkAction networkAction;
    private List<NetworkActionCombination> availableNaCombinations = new ArrayList<>();
    private Set<NetworkAction> availableNetworkActions;
    private RangeAction<?> rangeAction1;
    private RangeAction<?> rangeAction2;
    private Set<RangeAction<?>> availableRangeActions;
    private PerimeterResultWithCnecs prePerimeterResult;
    private AppliedRemedialActions appliedRemedialActions;

    private Leaf rootLeaf;

    private SearchTreeParameters searchTreeParameters;
    private TreeParameters treeParameters;
    private Map<Instant, RaUsageLimits> raLimitationParameters;

    private int leavesInParallel;

    private NetworkActionCombination predefinedNaCombination;

    @BeforeEach
    void setUp() throws Exception {
        setSearchTreeInput();
        searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        setSearchTreeParameters();
        searchTree = Mockito.spy(new SearchTree(searchTreeInput, searchTreeParameters, true));
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT);
        mockNetworkPool(network);
    }

    private void setSearchTreeParameters() {
        int maximumSearchDepth = 1;
        leavesInParallel = 1;
        treeParameters = Mockito.mock(TreeParameters.class);
        when(treeParameters.maximumSearchDepth()).thenReturn(maximumSearchDepth);
        when(treeParameters.leavesInParallel()).thenReturn(leavesInParallel);
        when(searchTreeParameters.getTreeParameters()).thenReturn(treeParameters);
        raLimitationParameters = new HashMap<>();
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raLimitationParameters);
        NetworkActionParameters networkActionParameters = Mockito.mock(NetworkActionParameters.class);
        when(searchTreeParameters.getNetworkActionParameters()).thenReturn(networkActionParameters);
        predefinedNaCombination = Mockito.mock(NetworkActionCombination.class);
        when(predefinedNaCombination.getConcatenatedId()).thenReturn("predefinedNa");
        when(networkActionParameters.getNetworkActionCombinations()).thenReturn(List.of(predefinedNaCombination));
    }

    private void setSearchTreeInput() {
        searchTreeInput = Mockito.mock(SearchTreeInput.class);
        appliedRemedialActions = Mockito.mock(AppliedRemedialActions.class);
        when(searchTreeInput.getPreOptimizationAppliedRemedialActions()).thenReturn(appliedRemedialActions);
        network = Mockito.mock(Network.class);
        when(searchTreeInput.getNetwork()).thenReturn(network);
        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        availableNetworkActions = new HashSet<>();
        when(optimizationPerimeter.getNetworkActions()).thenReturn(availableNetworkActions);
        availableRangeActions = new HashSet<>();
        when(optimizationPerimeter.getRangeActions()).thenReturn(availableRangeActions);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(optimizedState);
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        when(cnec.isOptimized()).thenReturn(true);
        when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnec));
        when(searchTreeInput.getOptimizationPerimeter()).thenReturn(optimizationPerimeter);
        prePerimeterResult = Mockito.mock(PerimeterResultWithCnecs.class);
        when(searchTreeInput.getPrePerimeterResult()).thenReturn(prePerimeterResult);
        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(objectiveFunction);
        when(optimizedState.getContingency()).thenReturn(Optional.empty());
        Instant preventiveInstant = Mockito.mock(Instant.class);
        when(preventiveInstant.toString()).thenReturn("preventive");
        when(optimizedState.getInstant()).thenReturn(preventiveInstant);
        rootLeaf = Mockito.mock(Leaf.class);
        when(searchTreeInput.getToolProvider()).thenReturn(Mockito.mock(ToolProvider.class));
        Instant outageInstant = Mockito.mock(Instant.class);
        when(outageInstant.isOutage()).thenReturn(true);
        when(searchTreeInput.getOutageInstant()).thenReturn(outageInstant);
    }

    @Test
    void runOnAFailingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        SearchTreeResult result = searchTree.run().get();
        assertEquals(rootLeaf.getResult(), result);
    }

    @Test
    void runWithoutOptimizingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        setStopCriterionAtTargetObjectiveValue(3.);

        double leafCost = 2.;
        SearchTreeResult searchTreeResult = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs perimeterResultWithCnecs = Mockito.mock(PerimeterResultWithCnecs.class);
        when(perimeterResultWithCnecs.getCost()).thenReturn(leafCost);
        when(searchTreeResult.getPerimeterResultWithCnecs()).thenReturn(perimeterResultWithCnecs);
        when(rootLeaf.getResult()).thenReturn(searchTreeResult);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        SearchTreeResult result = searchTree.run().get();
        assertEquals(searchTreeResult, result);
    }

    private void setStopCriterionAtTargetObjectiveValue(double value) {
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(value);
    }

    @Test
    void runAndOptimizeOnlyRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();

        double leafCost = 2.;
        SearchTreeResult searchTreeResult = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs perimeterResultWithCnecs = Mockito.mock(PerimeterResultWithCnecs.class);
        when(perimeterResultWithCnecs.getCost()).thenReturn(leafCost);
        when(searchTreeResult.getPerimeterResultWithCnecs()).thenReturn(perimeterResultWithCnecs);
        when(rootLeaf.getResult()).thenReturn(searchTreeResult);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        SearchTreeResult result = searchTree.run().get();
        assertEquals(searchTreeResult, result);
    }

    @Test
    void rootLeafMeetsTargetObjectiveValue() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(3.);
        searchTreeWithOneChildLeaf();

        SearchTreeResult evaluatedSearchTreeResult = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs evaluatedPerimeterResultWithCnecs = Mockito.mock(PerimeterResultWithCnecs.class);
        when(evaluatedPerimeterResultWithCnecs.getCost()).thenReturn(4.);
        when(evaluatedSearchTreeResult.getPerimeterResultWithCnecs()).thenReturn(evaluatedPerimeterResultWithCnecs);

        SearchTreeResult optimizedSearchTreeResult = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs optimizedPerimeterResultWithCnecs = Mockito.mock(PerimeterResultWithCnecs.class);
        when(optimizedPerimeterResultWithCnecs.getCost()).thenReturn(2.);
        when(optimizedSearchTreeResult.getPerimeterResultWithCnecs()).thenReturn(optimizedPerimeterResultWithCnecs);

        when(rootLeaf.getResult()).thenReturn(evaluatedSearchTreeResult, optimizedSearchTreeResult);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);

        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        SearchTreeResult result = searchTree.run().get();
        assertEquals(optimizedSearchTreeResult, result);
    }

    @Test
    void runAndIterateOnTreeWithChildLeafInError() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        searchTreeWithOneChildLeaf();

        SearchTreeResult rootLeafSearchTreeResult = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs rootLeafPerimeterResultWithCnecs = Mockito.mock(PerimeterResultWithCnecs.class);
        when(rootLeafPerimeterResultWithCnecs.getCost()).thenReturn(4.);
        when(rootLeafSearchTreeResult.getPerimeterResultWithCnecs()).thenReturn(rootLeafPerimeterResultWithCnecs);
        when(rootLeaf.getResult()).thenReturn(rootLeafSearchTreeResult);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        Leaf childLeaf = Mockito.mock(Leaf.class);
        when(childLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(network, new NetworkActionCombination(networkAction), false);

        SearchTreeResult result = searchTree.run().get();
        assertEquals(rootLeafSearchTreeResult, result);
    }

    private void setLeafStatusToEvaluated(Leaf leaf) {
        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(null);
        Mockito.doNothing().when(sensitivityComputer).compute(network);
        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        when(objectiveFunction.evaluate(any(), any())).thenReturn(null);
        leaf.evaluate(objectiveFunction, sensitivityComputer);
    }

    @Test
    void testCreateChildLeafFiltersOutRangeActionWhenNeeded() {
        searchTreeWithOneChildLeaf();
        when(networkAction.apply(network)).thenReturn(true);
        NetworkActionCombination naCombination = new NetworkActionCombination(networkAction);

        // 1) Mock rootLeaf and previousDepthOptimalLeaf to return Set.of(rangeAction)
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        RangeActionResultImpl rangeActionActivationResult = Mockito.mock(RangeActionResultImpl.class);
        when(rangeActionActivationResult.getRangeActions()).thenReturn(Set.of(rangeAction));
        when(rootLeaf.getRangeActionResult()).thenReturn(rangeActionActivationResult);
        doReturn(rootLeaf).when(searchTree).makeLeaf(any(), any(), any(), any());
        searchTree.initLeaves(searchTreeInput);

        // 2) Create 2 Leaf with different shouldRangeActionBeRemoved value
        Leaf filteredLeaf = searchTree.createChildLeaf(network, naCombination, true);
        Leaf unfilteredLeaf = searchTree.createChildLeaf(network, naCombination, false);

        // 3) Mocks a sensitivity computer to set leaf.status to EVALUATED
        setLeafStatusToEvaluated(filteredLeaf);
        setLeafStatusToEvaluated(unfilteredLeaf);

        // 4) Asserts that unfilteredLeaf keeps in memory activated range actions of parentLeaf
        assertEquals(rangeActionActivationResult, unfilteredLeaf.getRangeActionResult());
        assertEquals(Set.of(rangeAction), unfilteredLeaf.getRangeActionResult().getRangeActions());

        // 5) Asserts that the filteredLeaf reset activated range actions of parentLeaf
        assertEquals(Set.of(), filteredLeaf.getRangeActionResult().getRangeActions());
    }

    @Test
    void runAndIterateOnTreeWithABetterChildLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        searchTreeWithOneChildLeaf();
        Leaf childLeaf = Mockito.mock(Leaf.class);

        double rootLeafCostAfterOptim = 4.;
        double childLeafCostAfterOptim = 3.;

        mockLeafsCosts(rootLeafCostAfterOptim, childLeafCostAfterOptim, childLeaf);

        SearchTreeResult result = searchTree.run().get();
        assertEquals(childLeaf, result);
    }

    @Test
    void runAndIterateOnTreeWithAWorseChildLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        searchTreeWithOneChildLeaf();
        Leaf childLeaf = Mockito.mock(Leaf.class);

        double rootLeafCostAfterOptim = 4.;
        double childLeafCostAfterOptim = 5.;

        mockLeafsCosts(rootLeafCostAfterOptim, childLeafCostAfterOptim, childLeaf);

        SearchTreeResult result = searchTree.run().get();
        assertEquals(rootLeaf, result);
    }

    @Test
    void runAndIterateOnTreeStopCriterionReached() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(0.);

        NetworkAction networkAction1 = Mockito.mock(NetworkAction.class);
        NetworkAction networkAction2 = Mockito.mock(NetworkAction.class);
        when(networkAction1.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(networkAction2.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(networkAction1.getOperator()).thenReturn("operator1");
        when(networkAction2.getOperator()).thenReturn("operator2");
        when(networkAction1.getId()).thenReturn("na1");
        when(networkAction1.getId()).thenReturn("na2");
        availableNetworkActions.add(networkAction1);
        availableNetworkActions.add(networkAction2);
        availableNaCombinations.add(new NetworkActionCombination(networkAction1));
        availableNaCombinations.add(new NetworkActionCombination(networkAction2));

        Leaf childLeaf1 = Mockito.mock(Leaf.class);
        Leaf childLeaf2 = Mockito.mock(Leaf.class);

        double rootLeafCostAfterOptim = 4.;

        SearchTreeResult leaf1ResultAfterOptim = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs leaf1PerimeterResult = Mockito.mock(PerimeterResultWithCnecs.class);
        when(leaf1ResultAfterOptim.getPerimeterResultWithCnecs()).thenReturn(leaf1PerimeterResult);
        double childLeaf1CostAfterOptim = -1.;
        when(leaf1PerimeterResult.getCost()).thenReturn(childLeaf1CostAfterOptim);

        SearchTreeResult leaf2ResultAfterOptim = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs leaf2PerimeterResult = Mockito.mock(PerimeterResultWithCnecs.class);
        when(leaf2ResultAfterOptim.getPerimeterResultWithCnecs()).thenReturn(leaf2PerimeterResult);
        double childLeaf2CostAfterOptim = -2.;
        when(leaf2PerimeterResult.getCost()).thenReturn(childLeaf2CostAfterOptim);

        mockRootLeafCost(rootLeafCostAfterOptim);

        when(childLeaf1.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf1.getResult()).thenReturn(leaf1ResultAfterOptim);
        Mockito.doReturn(childLeaf1).when(searchTree).createChildLeaf(any(), eq(availableNaCombinations.get(0)), eq(false));

        when(childLeaf2.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf2.getResult()).thenReturn(leaf2ResultAfterOptim);
        Mockito.doReturn(childLeaf2).when(searchTree).createChildLeaf(any(), eq(availableNaCombinations.get(1)), eq(false));

        SearchTreeResult result = searchTree.run().get();
        assertEquals(leaf2ResultAfterOptim, result);
    }

    @Test
    void runAndIterateOnTreeWithSlightlyBetterChildLeafAndStopCriterionReached() throws Exception {
        raoWithoutLoopFlowLimitation();
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.0);
        searchTreeWithOneChildLeaf();
        Leaf childLeaf = Mockito.mock(Leaf.class);
        when(searchTreeParameters.getNetworkActionParameters().getAbsoluteNetworkActionMinimumImpactThreshold()).thenReturn(10.);

        double rootLeafCostAfterOptim = 1.;
        double childLeafCostAfterOptim = -1.;

        mockLeafsCosts(rootLeafCostAfterOptim, childLeafCostAfterOptim, childLeaf);

        SearchTreeResult result = searchTree.run().get();
        assertEquals(-1., result.getPerimeterResultWithCnecs().getCost(), DOUBLE_TOLERANCE);
    }

    private void mockRootLeafCost(double cost) {

        SearchTreeResult rootLeafResult = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs rootLeafPerimeterResult = Mockito.mock(PerimeterResultWithCnecs.class);
        when(rootLeafResult.getPerimeterResultWithCnecs()).thenReturn(rootLeafPerimeterResult);
        when(rootLeafPerimeterResult.getCost()).thenReturn(cost);
        when(rootLeafPerimeterResult.getVirtualCost()).thenReturn(cost);
        when(rootLeaf.getResult()).thenReturn(rootLeafResult);

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
    }

    private void setMaxPstPerTso(String tsoName, int maxPstOfTso) {
        Map<String, Integer> maxPstPerTso = new HashMap<>();
        maxPstPerTso.put(tsoName, maxPstOfTso);
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxPstPerTso(maxPstPerTso);
        Instant curativeInstant = Mockito.mock(Instant.class);
        when(curativeInstant.getId()).thenReturn("curative");
        raLimitationParameters = Map.of(curativeInstant, raUsageLimits);
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raLimitationParameters);
    }

    private void mockLeafsCosts(double rootLeafCostAfterOptim, double childLeafCostAfterOptim, Leaf childLeaf) {
        mockRootLeafCost(rootLeafCostAfterOptim);
        when(childLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);

        SearchTreeResult childLeafResult = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs childLeafPerimeterResult = Mockito.mock(PerimeterResultWithCnecs.class);
        when(childLeafResult.getPerimeterResultWithCnecs()).thenReturn(childLeafPerimeterResult);
        when(childLeafPerimeterResult.getCost()).thenReturn(childLeafCostAfterOptim);
        when(childLeafPerimeterResult.getVirtualCost()).thenReturn(childLeafCostAfterOptim);
        when(childLeaf.getResult()).thenReturn(childLeafResult);

        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(eq(network), any(), eq(false));
    }

    private void mockNetworkPool(Network network) throws Exception {
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        String workingVariantId = "ID";
        when(variantManager.getWorkingVariantId()).thenReturn(workingVariantId);
        when(network.getVariantManager()).thenReturn(variantManager);
        AbstractNetworkPool openRaoNetworkPool = AbstractNetworkPool.create(network, workingVariantId, leavesInParallel, true);
        Mockito.doReturn(openRaoNetworkPool).when(searchTree).makeOpenRaoNetworkPool(network, leavesInParallel);
    }

    private void searchTreeWithOneChildLeaf() {
        networkAction = Mockito.mock(NetworkAction.class);
        when(networkAction.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(networkAction.getOperator()).thenReturn("operator");
        when(networkAction.getId()).thenReturn("na1");
        availableNetworkActions.add(networkAction);
        availableNaCombinations.add(new NetworkActionCombination(networkAction));
    }

    private void setStopCriterionAtMinObjective() {
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
    }

    private void raoWithoutLoopFlowLimitation() {
        when(searchTreeParameters.getLoopFlowParameters()).thenReturn(null);
    }

    @Test
    void testPurelyVirtualStopCriterion() {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(-30.);

        FlowCnec mnec = Mockito.mock(FlowCnec.class);
        when(mnec.isOptimized()).thenReturn(false);
        when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(mnec));

        RangeAction<?> ra = Mockito.mock(RangeAction.class);
        when(ra.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(optimizationPerimeter.getRangeActions()).thenReturn(Set.of(ra));

        double leafCost = 0.;
        mockRootLeafCost(leafCost);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        // rootLeaf should not be optimized : its virtual cost is zero so stop criterion is already reached
        doThrow(OpenRaoException.class).when(rootLeaf).optimize(any(), any());

        try {
            searchTree.run();
        } catch (OpenRaoException e) {
            fail("Should not have optimized rootleaf as it had already reached the stop criterion");
        }
    }

    @Test
    void testLogsVerbose() {
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        when(rootLeaf.toString()).thenReturn("root leaf description");
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        ObjectiveFunctionResult initialResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(initialResult.getFunctionalCost()).thenReturn(0.);
        when(initialResult.getVirtualCost()).thenReturn(0.);
        when(rootLeaf.getPreOptimObjectiveFunctionResult()).thenReturn(initialResult);
        String expectedLog1 = "[DEBUG] Evaluating root leaf";
        String expectedLog2 = "[INFO] Could not evaluate leaf: root leaf description";
        String expectedLog3 = "[INFO] Scenario \"preventive\": initial cost = 0.00 (functional: 0.00, virtual: 0.00), no remedial actions activated, cost after preventive optimization = 0.00 (functional: 0.00, virtual: 0.00)";

        ListAppender<ILoggingEvent> technical = getLogs(TechnicalLogs.class);
        ListAppender<ILoggingEvent> business = getLogs(RaoBusinessLogs.class);
        searchTree.run();
        assertEquals(1, technical.list.size());
        assertEquals(2, business.list.size());
        assertEquals(expectedLog1, technical.list.get(0).toString());
        assertEquals(expectedLog2, business.list.get(0).toString());
        assertEquals(expectedLog3, business.list.get(1).toString());
    }

    @Test
    void testLogsDontVerbose() {
        searchTree = Mockito.spy(new SearchTree(searchTreeInput, searchTreeParameters, false));
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        when(rootLeaf.toString()).thenReturn("root leaf description");
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        ObjectiveFunctionResult initialResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(initialResult.getFunctionalCost()).thenReturn(0.);
        when(initialResult.getVirtualCost()).thenReturn(0.);
        when(rootLeaf.getPreOptimObjectiveFunctionResult()).thenReturn(initialResult);
        String expectedLog1 = "[DEBUG] Evaluating root leaf";
        String expectedLog2 = "[INFO] Could not evaluate leaf: root leaf description";
        String expectedLog3 = "[INFO] Scenario \"preventive\": initial cost = 0.00 (functional: 0.00, virtual: 0.00), no remedial actions activated, cost after preventive optimization = 0.00 (functional: 0.00, virtual: 0.00)";

        ListAppender<ILoggingEvent> technical = getLogs(TechnicalLogs.class);
        ListAppender<ILoggingEvent> business = getLogs(RaoBusinessLogs.class);
        searchTree.run();
        assertEquals(2, technical.list.size());
        assertEquals(1, business.list.size());
        assertEquals(expectedLog1, technical.list.get(0).toString());
        assertEquals(expectedLog2, technical.list.get(1).toString());
        assertEquals(expectedLog3, business.list.get(0).toString());
    }

    private ListAppender<ILoggingEvent> getLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    void testCostSatisfiesStopCriterion() {
        setSearchTreeParameters();

        // MIN_OBJECTIVE
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
        assertFalse(searchTree.costSatisfiesStopCriterion(-10));
        assertFalse(searchTree.costSatisfiesStopCriterion(-0.1));
        assertFalse(searchTree.costSatisfiesStopCriterion(0));
        assertFalse(searchTree.costSatisfiesStopCriterion(0.1));
        assertFalse(searchTree.costSatisfiesStopCriterion(10));

        // AT_TARGET_OBJECTIVE_VALUE
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        assertTrue(searchTree.costSatisfiesStopCriterion(-10));
        assertTrue(searchTree.costSatisfiesStopCriterion(-0.1));
        assertFalse(searchTree.costSatisfiesStopCriterion(0));
        assertFalse(searchTree.costSatisfiesStopCriterion(0.1));
        assertFalse(searchTree.costSatisfiesStopCriterion(10));
    }

    private void setUpForVirtualLogs() {
        setSearchTreeParameters();
        setSearchTreeInput();
        searchTree = Mockito.spy(new SearchTree(searchTreeInput, searchTreeParameters, false));

        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        State state = Mockito.mock(State.class);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        when(cnec.getState()).thenReturn(state);
        when(cnec.getNetworkElement()).thenReturn(networkElement);
        when(cnec.getId()).thenReturn("cnec-id");
        when(cnec.getName()).thenReturn("cnec-name");
        when(cnec.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(1000.));
        when(state.getId()).thenReturn("state-id");
        when(networkElement.getId()).thenReturn("ne-id");

        SearchTreeResult rootLeafResult = Mockito.mock(SearchTreeResult.class);
        PerimeterResultWithCnecs rootLeafPerimeterResult = Mockito.mock(PerimeterResultWithCnecs.class);
        when(rootLeafResult.getPerimeterResultWithCnecs()).thenReturn(rootLeafPerimeterResult);
        when(rootLeaf.getResult()).thenReturn(rootLeafResult);

        when(rootLeafPerimeterResult.getCostlyElements(eq("loop-flow-cost"), anyInt())).thenReturn(List.of(cnec));
        when(rootLeaf.getIdentifier()).thenReturn("leaf-id");
        when(rootLeafPerimeterResult.getMargin(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(-135.);
        when(rootLeafPerimeterResult.getMargin(cnec, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-134.);
        when(rootLeafPerimeterResult.getFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(1135.);
    }

    @Test
    void testGetCostlyElementsLogs() {
        setUpForVirtualLogs();

        List<String> logs = searchTree.getVirtualCostlyElementsLogs(rootLeaf, "loop-flow-cost", "Optimized ");
        assertEquals(1, logs.size());
        assertEquals("Optimized leaf-id, limiting \"loop-flow-cost\" constraint #01: flow = 1135.00 MW, threshold = 1000.00 MW, margin = -135.00 MW, element ne-id at state state-id, CNEC ID = \"cnec-id\", CNEC name = \"cnec-name\"", logs.get(0));
    }

    @Test
    void testLogVirtualCostDetails() {
        setUpForVirtualLogs();

        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        // functional cost = -100 (secure)
        // virtual cost = 200
        // overall cost = 100 (unsecure)
        PerimeterResultWithCnecs resultMock = rootLeaf.getResult().getPerimeterResultWithCnecs();
        when(rootLeaf.isRoot()).thenReturn(true);
        when(resultMock.getCost()).thenReturn(100.);
        when(resultMock.getVirtualCost("loop-flow-cost")).thenReturn(200.);

        // Functional cost does not satisfy stop criterion
        ListAppender<ILoggingEvent> business = getLogs(RaoBusinessLogs.class);
        searchTree.logVirtualCostDetails(rootLeaf, "loop-flow-cost", "Optimized ");
        assertEquals(2, business.list.size());
        assertEquals("[INFO] Optimized leaf-id, stop criterion could have been reached without \"loop-flow-cost\" virtual cost", business.list.get(0).toString());
        assertEquals("[INFO] Optimized leaf-id, limiting \"loop-flow-cost\" constraint #01: flow = 1135.00 MW, threshold = 1000.00 MW, margin = -135.00 MW, element ne-id at state state-id, CNEC ID = \"cnec-id\", CNEC name = \"cnec-name\"", business.list.get(1).toString());
    }

    @Test
    void testLogRangeActions() {
        setUpForVirtualLogs();
        List<ILoggingEvent> logsList = getLogs(TechnicalLogs.class).list;
        logRangeActions(TECHNICAL_LOGS, rootLeaf.getResult(), searchTreeInput.getOptimizationPerimeter(), "");
        assertEquals("[INFO] No range actions activated", logsList.get(logsList.size() - 1).toString());

        // apply 2 range actions
        rangeAction1 = Mockito.mock(PstRangeAction.class);
        rangeAction2 = Mockito.mock(PstRangeAction.class);
        when(rangeAction1.getName()).thenReturn("PST1");
        when(rangeAction2.getName()).thenReturn("PST2");
        when(searchTreeInput.getOptimizationPerimeter().getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState));
        PerimeterResultWithCnecs resultMock = rootLeaf.getResult().getPerimeterResultWithCnecs();
        when(resultMock.getActivatedRangeActions()).thenReturn(Set.of(rangeAction1, rangeAction2));

        logRangeActions(TECHNICAL_LOGS, rootLeaf.getResult(), searchTreeInput.getOptimizationPerimeter(), "");
        // PST can be logged in any order
        assert logsList.get(logsList.size() - 1).toString().contains("[INFO] range action(s):");
        assert logsList.get(logsList.size() - 1).toString().contains("PST1: 0");
        assert logsList.get(logsList.size() - 1).toString().contains("PST2: 0");
    }

    @Test
    void testSortNaCombinations() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        when(na1.getId()).thenReturn("na1");
        when(na2.getId()).thenReturn("na2");

        // 1. First priority given to combinations detected during RAO
        assertEquals(-1, searchTree.deterministicNetworkActionCombinationComparison(
                new NetworkActionCombination(Set.of(na1), true),
                new NetworkActionCombination(Set.of(na2), false)
        ));
        assertEquals(1, searchTree.deterministicNetworkActionCombinationComparison(
                predefinedNaCombination,
                new NetworkActionCombination(Set.of(na2), true)
        ));
        // 2. Second priority given to pre-defined combinations
        assertEquals(-1, searchTree.deterministicNetworkActionCombinationComparison(
                predefinedNaCombination,
                new NetworkActionCombination(Set.of(na2), false)
        ));
        assertEquals(1, searchTree.deterministicNetworkActionCombinationComparison(
                new NetworkActionCombination(Set.of(na2), false),
                predefinedNaCombination
        ));
        // 3. Third priority given to large combinations
        assertEquals(-1, searchTree.deterministicNetworkActionCombinationComparison(
                new NetworkActionCombination(Set.of(na1, na2), false),
                new NetworkActionCombination(Set.of(na2), false)
        ));
        assertEquals(1, searchTree.deterministicNetworkActionCombinationComparison(
                new NetworkActionCombination(Set.of(na1), true),
                new NetworkActionCombination(Set.of(na2, na1), true)
        ));
        // 4. Last priority is random but deterministic
        assertEquals(-1, searchTree.deterministicNetworkActionCombinationComparison(
                new NetworkActionCombination(Set.of(na1), true),
                new NetworkActionCombination(Set.of(na2), true)
        ));
        assertEquals(1, searchTree.deterministicNetworkActionCombinationComparison(
                new NetworkActionCombination(Set.of(na2), false),
                new NetworkActionCombination(Set.of(na1), false)
        ));
    }
}
