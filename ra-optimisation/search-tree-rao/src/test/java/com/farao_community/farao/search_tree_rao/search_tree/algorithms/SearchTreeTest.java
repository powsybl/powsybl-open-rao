/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.RaoBusinessLogs;
import com.farao_community.farao.commons.logs.TechnicalLogs;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.GlobalRemedialActionLimitationParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.NetworkActionParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.TreeParameters;
import com.farao_community.farao.search_tree_rao.result.api.ObjectiveFunctionResult;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;
import static com.farao_community.farao.search_tree_rao.commons.RaoLogger.logRangeActions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class SearchTreeTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private final State optimizedState = Mockito.mock(State.class);
    private final List<NetworkActionCombination> availableNaCombinations = new ArrayList<>();
    private SearchTree searchTree;
    private SearchTreeInput searchTreeInput;
    private Network network;
    private OptimizationPerimeter optimizationPerimeter;
    private NetworkAction networkAction;
    private Set<NetworkAction> availableNetworkActions;
    private RangeAction<?> rangeAction1;
    private RangeAction<?> rangeAction2;
    private Set<RangeAction<?>> availableRangeActions;
    private PrePerimeterResult prePerimeterResult;
    private AppliedRemedialActions appliedRemedialActions;

    private Leaf rootLeaf;

    private SearchTreeParameters searchTreeParameters;
    private TreeParameters treeParameters;
    private GlobalRemedialActionLimitationParameters raLimitationParameters;

    private int leavesInParallel;

    private NetworkActionCombination predefinedNaCombination;
    private Instant outageInstant;

    @BeforeEach
    public void setUp() throws Exception {
        setSearchTreeInput();
        searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        setSearchTreeParameters();
        searchTree = Mockito.spy(new SearchTree(searchTreeInput, searchTreeParameters, true));
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT);
        mockNetworkPool(network);
        outageInstant = Mockito.mock(Instant.class);
        Mockito.when(outageInstant.getOrder()).thenReturn(1);
    }

    private void setSearchTreeParameters() {
        int maximumSearchDepth = 1;
        leavesInParallel = 1;
        treeParameters = Mockito.mock(TreeParameters.class);
        when(treeParameters.getMaximumSearchDepth()).thenReturn(maximumSearchDepth);
        when(treeParameters.getLeavesInParallel()).thenReturn(leavesInParallel);
        when(searchTreeParameters.getTreeParameters()).thenReturn(treeParameters);
        raLimitationParameters = Mockito.mock(GlobalRemedialActionLimitationParameters.class);
        when(raLimitationParameters.getMaxCurativeRa()).thenReturn(Integer.MAX_VALUE);
        when(raLimitationParameters.getMaxCurativeTso()).thenReturn(Integer.MAX_VALUE);
        when(raLimitationParameters.getMaxCurativePstPerTso()).thenReturn(new HashMap<>());
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
        prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        when(searchTreeInput.getPrePerimeterResult()).thenReturn(prePerimeterResult);
        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(objectiveFunction);
        when(optimizedState.getContingency()).thenReturn(Optional.empty());
        Instant preventiveInstant = Mockito.mock(Instant.class);
        when(preventiveInstant.getInstantKind()).thenReturn(InstantKind.PREVENTIVE);
        when(preventiveInstant.toString()).thenReturn(PREVENTIVE_INSTANT_ID);
        when(optimizedState.getInstant()).thenReturn(preventiveInstant);
        rootLeaf = Mockito.mock(Leaf.class);
        when(searchTreeInput.getToolProvider()).thenReturn(Mockito.mock(ToolProvider.class));
    }

    @Test
    void runOnAFailingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        OptimizationResult result = searchTree.run(outageInstant).get();
        assertEquals(rootLeaf, result);
    }

    @Test
    void runWithoutOptimizingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        setStopCriterionAtTargetObjectiveValue(3.);

        double leafCost = 2.;
        when(rootLeaf.getCost()).thenReturn(leafCost);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        OptimizationResult result = searchTree.run(outageInstant).get();
        assertEquals(rootLeaf, result);
        assertEquals(leafCost, result.getCost(), DOUBLE_TOLERANCE);
    }

    private void setStopCriterionAtTargetObjectiveValue(double value) {
        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.getTargetObjectiveValue()).thenReturn(value);
    }

    @Test
    void runAndOptimizeOnlyRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        when(rootLeaf.getCost()).thenReturn(2.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        OptimizationResult result = searchTree.run(outageInstant).get();
        assertEquals(rootLeaf, result);
        assertEquals(2., result.getCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void rootLeafMeetsTargetObjectiveValue() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(3.);
        searchTreeWithOneChildLeaf();
        when(rootLeaf.getCost()).thenReturn(4., 2.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        OptimizationResult result = searchTree.run(outageInstant).get();
        assertEquals(rootLeaf, result);
        assertEquals(2., result.getCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void runAndIterateOnTreeWithChildLeafInError() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        searchTreeWithOneChildLeaf();

        when(rootLeaf.getCost()).thenReturn(4.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        Leaf childLeaf = Mockito.mock(Leaf.class);
        when(childLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(network, new NetworkActionCombination(networkAction), false);

        OptimizationResult result = searchTree.run(outageInstant).get();
        assertEquals(rootLeaf, result);
        assertEquals(4., result.getCost(), DOUBLE_TOLERANCE);
    }

    private void setLeafStatusToEvaluated(Leaf leaf) {
        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(null);
        Mockito.doNothing().when(sensitivityComputer).compute(network, outageInstant);
        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        when(objectiveFunction.evaluate(any(), any(), any(), any())).thenReturn(null);
        leaf.evaluate(objectiveFunction, sensitivityComputer, outageInstant);
    }

    @Test
    void testCreateChildLeafFiltersOutRangeActionWhenNeeded() {
        searchTreeWithOneChildLeaf();
        when(networkAction.apply(network)).thenReturn(true);
        NetworkActionCombination naCombination = new NetworkActionCombination(networkAction);

        // 1) Mock rootLeaf and previousDepthOptimalLeaf to return Set.of(rangeAction)
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        RangeActionActivationResultImpl rangeActionActivationResult = Mockito.mock(RangeActionActivationResultImpl.class);
        when(rangeActionActivationResult.getRangeActions()).thenReturn(Set.of(rangeAction));
        when(rootLeaf.getRangeActionActivationResult()).thenReturn(rangeActionActivationResult);
        doReturn(rootLeaf).when(searchTree).makeLeaf(any(), any(), any(), any());
        searchTree.initLeaves(searchTreeInput);

        // 2) Create 2 Leaf with different shouldRangeActionBeRemoved value
        Leaf filteredLeaf = searchTree.createChildLeaf(network, naCombination, true);
        Leaf unfilteredLeaf = searchTree.createChildLeaf(network, naCombination, false);

        // 3) Mocks a sensitivity computer to set leaf.status to EVALUATED
        setLeafStatusToEvaluated(filteredLeaf);
        setLeafStatusToEvaluated(unfilteredLeaf);

        // 4) Asserts that unfilteredLeaf keeps in memory activated range actions of parentLeaf
        assertEquals(rangeActionActivationResult, unfilteredLeaf.getRangeActionActivationResult());
        assertEquals(Set.of(rangeAction), unfilteredLeaf.getRangeActionActivationResult().getRangeActions());

        // 5) Asserts that the filteredLeaf reset activated range actions of parentLeaf
        assertEquals(Set.of(), filteredLeaf.getRangeActionActivationResult().getRangeActions());
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

        OptimizationResult result = searchTree.run(outageInstant).get();
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

        OptimizationResult result = searchTree.run(outageInstant).get();
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
        double childLeaf1CostAfterOptim = -1.;
        double childLeaf2CostAfterOptim = -2.;

        mockRootLeafCost(rootLeafCostAfterOptim);

        when(childLeaf1.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf1.getCost()).thenReturn(childLeaf1CostAfterOptim);
        Mockito.doReturn(childLeaf1).when(searchTree).createChildLeaf(any(), eq(availableNaCombinations.get(0)), eq(false));

        when(childLeaf2.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf2.getCost()).thenReturn(childLeaf2CostAfterOptim);
        Mockito.doReturn(childLeaf2).when(searchTree).createChildLeaf(any(), eq(availableNaCombinations.get(1)), eq(false));

        OptimizationResult result = searchTree.run(outageInstant).get();
        assertEquals(childLeaf1, result);
    }

    @Test
    void runAndIterateOnTreeWithSlightlyBetterChildLeafAndStopCriterionReached() throws Exception {
        raoWithoutLoopFlowLimitation();
        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.getTargetObjectiveValue()).thenReturn(0.0);
        searchTreeWithOneChildLeaf();
        Leaf childLeaf = Mockito.mock(Leaf.class);
        when(searchTreeParameters.getNetworkActionParameters().getAbsoluteNetworkActionMinimumImpactThreshold()).thenReturn(10.);

        double rootLeafCostAfterOptim = 1.;
        double childLeafCostAfterOptim = -1.;

        mockLeafsCosts(rootLeafCostAfterOptim, childLeafCostAfterOptim, childLeaf);

        OptimizationResult result = searchTree.run(outageInstant).get();
        assertEquals(childLeaf, result);
    }

    @Test
    void optimizeRootLeafWithRangeActions() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();

        String tsoName = "TSO";
        raoWithRangeActionsForTso(tsoName);
        int maxPstOfTso = 2;
        setMaxPstPerTso(tsoName, maxPstOfTso);

        mockRootLeafCost(5.);
        when(rootLeaf.getOptimizedSetpoint(rangeAction2, optimizedState)).thenReturn(3.);

        OptimizationResult result = searchTree.run(outageInstant).get();
        assertEquals(3., result.getOptimizedSetpoint(rangeAction2, optimizedState), DOUBLE_TOLERANCE);
    }

    private void raoWithRangeActionsForTso(String tsoName) {
        rangeAction1 = Mockito.mock(PstRangeAction.class);
        rangeAction2 = Mockito.mock(PstRangeAction.class);
        when(rangeAction1.getOperator()).thenReturn(tsoName);
        when(rangeAction1.getName()).thenReturn("PST1");
        when(rangeAction1.getId()).thenReturn("PST1");
        when(rangeAction1.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(rangeAction1.getMaxAdmissibleSetpoint(anyDouble())).thenReturn(5.);
        when(rangeAction1.getMinAdmissibleSetpoint(anyDouble())).thenReturn(-5.);
        when(rangeAction2.getOperator()).thenReturn(tsoName);
        when(rangeAction2.getName()).thenReturn("PST2");
        when(rangeAction2.getId()).thenReturn("PST2");
        when(rangeAction2.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(rangeAction2.getMaxAdmissibleSetpoint(anyDouble())).thenReturn(5.);
        when(rangeAction2.getMinAdmissibleSetpoint(anyDouble())).thenReturn(-5.);
        availableRangeActions.add(rangeAction1);
        availableRangeActions.add(rangeAction2);

        FlowCnec mostLimitingElement = Mockito.mock(FlowCnec.class);
        when(rootLeaf.getMostLimitingElements(1)).thenReturn(Collections.singletonList(mostLimitingElement));
    }

    private void mockRootLeafCost(double cost) {
        when(rootLeaf.getCost()).thenReturn(cost);
        when(rootLeaf.getVirtualCost()).thenReturn(cost);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
    }

    private void setMaxPstPerTso(String tsoName, int maxPstOfTso) {
        Map<String, Integer> maxPstPerTso = new HashMap<>();
        maxPstPerTso.put(tsoName, maxPstOfTso);
        GlobalRemedialActionLimitationParameters raLimitationParameters = Mockito.mock(GlobalRemedialActionLimitationParameters.class);
        when(raLimitationParameters.getMaxCurativeRa()).thenReturn(Integer.MAX_VALUE);
        when(raLimitationParameters.getMaxCurativeTso()).thenReturn(Integer.MAX_VALUE);
        when(raLimitationParameters.getMaxCurativePstPerTso()).thenReturn(maxPstPerTso);
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raLimitationParameters);
    }

    private void mockLeafsCosts(double rootLeafCostAfterOptim, double childLeafCostAfterOptim, Leaf childLeaf) throws Exception {
        mockRootLeafCost(rootLeafCostAfterOptim);
        when(childLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf.getCost()).thenReturn(childLeafCostAfterOptim);
        when(childLeaf.getVirtualCost()).thenReturn(childLeafCostAfterOptim);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(eq(network), any(), eq(false));
    }

    private void mockNetworkPool(Network network) {
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        String workingVariantId = "ID";
        when(variantManager.getWorkingVariantId()).thenReturn(workingVariantId);
        when(network.getVariantManager()).thenReturn(variantManager);
        AbstractNetworkPool faraoNetworkPool = AbstractNetworkPool.create(network, workingVariantId, leavesInParallel, true);
        Mockito.doReturn(faraoNetworkPool).when(searchTree).makeFaraoNetworkPool(network, leavesInParallel);
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
        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
    }

    private void raoWithoutLoopFlowLimitation() {
        when(searchTreeParameters.getLoopFlowParameters()).thenReturn(null);
    }

    private void setMaxRa(int maxRa) {
        when(raLimitationParameters.getMaxCurativeRa()).thenReturn(maxRa);
    }

    @Test
    void testPurelyVirtualStopCriterion() {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(-30.);

        FlowCnec mnec = Mockito.mock(FlowCnec.class);
        when(mnec.isOptimized()).thenReturn(false);
        when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(mnec));

        RangeAction ra = Mockito.mock(RangeAction.class);
        when(ra.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(optimizationPerimeter.getRangeActions()).thenReturn(Set.of(ra));

        double leafCost = 0.;
        when(rootLeaf.getCost()).thenReturn(leafCost);
        when(rootLeaf.getVirtualCost()).thenReturn(0.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        // rootLeaf should not be optimized : its virtual cost is zero so stop criterion is already reached
        doThrow(FaraoException.class).when(rootLeaf).optimize(any(), any(), any());

        try {
            searchTree.run(outageInstant);
        } catch (FaraoException e) {
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
        searchTree.run(outageInstant);
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
        searchTree.run(outageInstant);
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
        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
        assertFalse(searchTree.costSatisfiesStopCriterion(-10));
        assertFalse(searchTree.costSatisfiesStopCriterion(-0.1));
        assertFalse(searchTree.costSatisfiesStopCriterion(0));
        assertFalse(searchTree.costSatisfiesStopCriterion(0.1));
        assertFalse(searchTree.costSatisfiesStopCriterion(10));

        // AT_TARGET_OBJECTIVE_VALUE
        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.getTargetObjectiveValue()).thenReturn(0.);
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

        when(rootLeaf.getCostlyElements(eq("loop-flow-cost"), anyInt())).thenReturn(List.of(cnec));
        when(rootLeaf.getIdentifier()).thenReturn("leaf-id");
        when(rootLeaf.getMargin(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(-135.);
        when(rootLeaf.getMargin(cnec, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-134.);
        when(rootLeaf.getFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(1135.);
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

        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.getTargetObjectiveValue()).thenReturn(0.);
        // functional cost = -100 (secure)
        // virtual cost = 200
        // overall cost = 100 (unsecure)
        when(rootLeaf.isRoot()).thenReturn(true);
        when(rootLeaf.getCost()).thenReturn(100.);
        when(rootLeaf.getVirtualCost("loop-flow-cost")).thenReturn(200.);

        // Functional cost does not satisfy stop criterion
        ListAppender<ILoggingEvent> business = getLogs(RaoBusinessLogs.class);
        searchTree.logVirtualCostDetails(rootLeaf, "loop-flow-cost", "Optimized ");
        assertEquals(2, business.list.size());
        assertEquals("[INFO] Optimized leaf-id, stop criterion could have been reached without \"loop-flow-cost\" virtual cost", business.list.get(0).toString());
        assertEquals("[INFO] Optimized leaf-id, limiting \"loop-flow-cost\" constraint #01: flow = 1135.00 MW, threshold = 1000.00 MW, margin = -135.00 MW, element ne-id at state state-id, CNEC ID = \"cnec-id\", CNEC name = \"cnec-name\"", business.list.get(1).toString());
    }

    @Test
    public void testLogRangeActions() {
        setUpForVirtualLogs();
        List<ILoggingEvent> logsList = getLogs(TechnicalLogs.class).list;
        logRangeActions(TECHNICAL_LOGS, rootLeaf, searchTreeInput.getOptimizationPerimeter(), "");
        assertEquals("[INFO] No range actions activated", logsList.get(logsList.size() - 1).toString());

        // apply 2 range actions
        rangeAction1 = Mockito.mock(PstRangeAction.class);
        rangeAction2 = Mockito.mock(PstRangeAction.class);
        when(rangeAction1.getName()).thenReturn("PST1");
        when(rangeAction2.getName()).thenReturn("PST2");
        when(searchTreeInput.getOptimizationPerimeter().getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState));
        when(rootLeaf.getActivatedRangeActions(optimizedState)).thenReturn(Set.of(rangeAction1, rangeAction2));

        logRangeActions(TECHNICAL_LOGS, rootLeaf, searchTreeInput.getOptimizationPerimeter(), "");
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
