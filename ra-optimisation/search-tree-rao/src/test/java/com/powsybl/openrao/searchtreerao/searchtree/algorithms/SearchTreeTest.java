/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.HvdcRangeActionImpl;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.searchtreerao.commons.HvdcUtils;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.NetworkActionParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.reports.ReportsTestUtils;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

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
    private final List<NetworkActionCombination> availableNaCombinations = new ArrayList<>();
    private Set<NetworkAction> availableNetworkActions;
    private RangeAction<?> rangeAction1;
    private RangeAction<?> rangeAction2;
    private Set<RangeAction<?>> availableRangeActions;
    private PrePerimeterResult prePerimeterResult;
    private AppliedRemedialActions appliedRemedialActions;

    private Leaf rootLeaf;

    private SearchTreeParameters searchTreeParameters;
    private TreeParameters treeParameters;
    private Map<Instant, RaUsageLimits> raLimitationParameters;

    private int leavesInParallel;

    private NetworkActionCombination predefinedNaCombination;

    private ReportNode reportNode;

    MockedStatic<HvdcUtils> hvdcUtilsMock;

    @BeforeEach
    void setUp() {
        reportNode = ReportsTestUtils.getTestRootNode();
        setSearchTreeInput();
        searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        setSearchTreeParameters();
        searchTree = Mockito.spy(new SearchTree(searchTreeInput, searchTreeParameters, true, reportNode));
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(searchTreeParameters.getObjectiveFunctionUnit()).thenReturn(Unit.MEGAWATT);
        mockNetworkPool(network);

        // Mock call to runLoadFlowAndUpdateHvdcActivePowerSetpoint(...)
        hvdcUtilsMock = mockStatic(HvdcUtils.class);
        hvdcUtilsMock
            .when(() -> HvdcUtils.runLoadFlowAndUpdateHvdcActivePowerSetpoint(any(Network.class), any(State.class), any(String.class), any(LoadFlowParameters.class), any(Set.class)))
            .thenReturn(Map.of());

        hvdcUtilsMock
            .when(() -> HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation(any(), eq(network)))
            .thenCallRealMethod();

    }

    @AfterEach
    void tearDown() {
        if (hvdcUtilsMock != null) {
            hvdcUtilsMock.close();
        }
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
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = Mockito.mock(LoadFlowAndSensitivityParameters.class);
        when(searchTreeParameters.getLoadFlowAndSensitivityParameters()).thenReturn(Optional.ofNullable(loadFlowAndSensitivityParameters));
        SensitivityAnalysisParameters sensitivityAnalysisParameters = Mockito.mock(SensitivityAnalysisParameters.class);
        when(loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters()).thenReturn(sensitivityAnalysisParameters);
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
        when(optimizationPerimeter.copyWithFilteredAvailableHvdcRangeAction(network)).thenReturn(optimizationPerimeter);
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

        OptimizationResult result = searchTree.run().get();
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

        OptimizationResult result = searchTree.run().get();
        assertEquals(rootLeaf, result);
        assertEquals(leafCost, result.getCost(), DOUBLE_TOLERANCE);
    }

    private void setStopCriterionAtTargetObjectiveValue(double value) {
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(value);
    }

    @Test
    void runAndOptimizeOnlyRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        when(rootLeaf.getCost()).thenReturn(2.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        OptimizationResult result = searchTree.run().get();
        assertEquals(rootLeaf, result);
        assertEquals(2., result.getCost(), DOUBLE_TOLERANCE);

        hvdcUtilsMock.verify(() -> HvdcUtils.runLoadFlowAndUpdateHvdcActivePowerSetpoint(any(), any(), any(), any(), any()), times(0));
    }

    @Test
    void runAndOptimizeOnlyRootLeafWithLoadFlow() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        when(rootLeaf.getCost()).thenReturn(2.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        // add an hvdc range action on a HVDC lie in AC emulation
        HvdcRangeAction hvdcRangeAction = Mockito.mock(HvdcRangeActionImpl.class);
        when(hvdcRangeAction.isAngleDroopActivePowerControlEnabled(network)).thenReturn(true);
        when(optimizationPerimeter.getRangeActions()).thenReturn(Set.of(hvdcRangeAction));
        OptimizationResult result = searchTree.run().get();

        hvdcUtilsMock.verify(() -> HvdcUtils.runLoadFlowAndUpdateHvdcActivePowerSetpoint(any(), any(), any(), any(), any()), times(1));

    }

    @Test
    void rootLeafMeetsTargetObjectiveValue() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(3.);
        searchTreeWithOneChildLeaf();
        when(rootLeaf.getCost()).thenReturn(4., 2.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        OptimizationResult result = searchTree.run().get();
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

        OptimizationResult result = searchTree.run().get();
        assertEquals(rootLeaf, result);
        assertEquals(4., result.getCost(), DOUBLE_TOLERANCE);
    }

    private void setLeafStatusToEvaluated(Leaf leaf) {
        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(null);
        Mockito.doNothing().when(sensitivityComputer).compute(network);
        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        when(objectiveFunction.evaluate(any(), any(), any())).thenReturn(null);
        leaf.evaluate(objectiveFunction, sensitivityComputer, reportNode);
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

        OptimizationResult result = searchTree.run().get();
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

        OptimizationResult result = searchTree.run().get();
        assertEquals(rootLeaf, result);
    }

    @Test
    void runAndIterateOnTreeStopCriterionReached() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(0.);

        NetworkAction networkAction1 = Mockito.mock(NetworkAction.class);
        NetworkAction networkAction2 = Mockito.mock(NetworkAction.class);
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
        Mockito.doReturn(childLeaf1).when(searchTree).createChildLeaf(any(), eq(availableNaCombinations.getFirst()), eq(false));

        when(childLeaf2.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf2.getCost()).thenReturn(childLeaf2CostAfterOptim);
        Mockito.doReturn(childLeaf2).when(searchTree).createChildLeaf(any(), eq(availableNaCombinations.get(1)), eq(false));

        OptimizationResult result = searchTree.run().get();
        assertEquals(childLeaf1, result);
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

        OptimizationResult result = searchTree.run().get();
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

        OptimizationResult result = searchTree.run().get();
        assertEquals(3., result.getOptimizedSetpoint(rangeAction2, optimizedState), DOUBLE_TOLERANCE);
    }

    private void raoWithRangeActionsForTso(String tsoName) {
        rangeAction1 = Mockito.mock(PstRangeAction.class);
        rangeAction2 = Mockito.mock(PstRangeAction.class);
        when(rangeAction1.getOperator()).thenReturn(tsoName);
        when(rangeAction1.getName()).thenReturn("PST1");
        when(rangeAction1.getId()).thenReturn("PST1");
        when(rangeAction1.getMaxAdmissibleSetpoint(anyDouble())).thenReturn(5.);
        when(rangeAction1.getMinAdmissibleSetpoint(anyDouble())).thenReturn(-5.);
        when(rangeAction2.getOperator()).thenReturn(tsoName);
        when(rangeAction2.getName()).thenReturn("PST2");
        when(rangeAction2.getId()).thenReturn("PST2");
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
        when(childLeaf.getCost()).thenReturn(childLeafCostAfterOptim);
        when(childLeaf.getVirtualCost()).thenReturn(childLeafCostAfterOptim);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(eq(network), any(), eq(false));
    }

    private void mockNetworkPool(Network network) {
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        String workingVariantId = "ID";
        when(variantManager.getWorkingVariantId()).thenReturn(workingVariantId);
        when(network.getVariantManager()).thenReturn(variantManager);
        AbstractNetworkPool openRaoNetworkPool = AbstractNetworkPool.create(network, workingVariantId, leavesInParallel, true);
        Mockito.doReturn(openRaoNetworkPool).when(searchTree).makeOpenRaoNetworkPool(network, leavesInParallel);
    }

    private void searchTreeWithOneChildLeaf() {
        networkAction = Mockito.mock(NetworkAction.class);
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
        when(optimizationPerimeter.getRangeActions()).thenReturn(Set.of(ra));

        double leafCost = 0.;
        when(rootLeaf.getCost()).thenReturn(leafCost);
        when(rootLeaf.getVirtualCost()).thenReturn(0.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        // rootLeaf should not be optimized : its virtual cost is zero so stop criterion is already reached
        doThrow(OpenRaoException.class).when(rootLeaf).optimize(any(), any(), any());

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
        String expectedLog3 = "[INFO] Scenario \"preventive\": initial cost = 0.0 (functional: 0.0, virtual: 0.0), no remedial actions activated, cost after preventive optimization = 0.0 (functional: 0.0, virtual: 0.0)";

        ListAppender<ILoggingEvent> technical = ReportsTestUtils.getTechnicalLogs();
        ListAppender<ILoggingEvent> business = ReportsTestUtils.getBusinessLogs();
        searchTree.run();
        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(0, traceReports.size());
        assertEquals(2, infoReports.size());
        assertEquals(1, technical.list.size());
        assertEquals(2, business.list.size());
        assertTrue(expectedLog2.endsWith(infoReports.getFirst().getMessage()));
        assertTrue(expectedLog3.endsWith(infoReports.get(1).getMessage()));
        assertEquals(expectedLog1, technical.list.getFirst().toString());
        assertEquals(expectedLog2, business.list.getFirst().toString());
        assertEquals(expectedLog3, business.list.get(1).toString());
    }

    @Test
    void testLogsDontVerbose() {
        searchTree = Mockito.spy(new SearchTree(searchTreeInput, searchTreeParameters, false, reportNode));
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
        String expectedLog3 = "[INFO] Scenario \"preventive\": initial cost = 0.0 (functional: 0.0, virtual: 0.0), no remedial actions activated, cost after preventive optimization = 0.0 (functional: 0.0, virtual: 0.0)";

        ListAppender<ILoggingEvent> technical = ReportsTestUtils.getTechnicalLogs();
        ListAppender<ILoggingEvent> business = ReportsTestUtils.getBusinessLogs();
        searchTree.run();
        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(1, traceReports.size());
        assertEquals(1, infoReports.size());
        assertEquals(2, technical.list.size());
        assertEquals(1, business.list.size());
        assertTrue(expectedLog2.endsWith(traceReports.getFirst().getMessage()));
        assertTrue(expectedLog3.endsWith(infoReports.getFirst().getMessage()));
        assertEquals(expectedLog1, technical.list.getFirst().toString());
        assertEquals(expectedLog2, technical.list.get(1).toString());
        assertEquals(expectedLog3, business.list.getFirst().toString());
    }

    @Test
    void testCostSatisfiesStopCriterion() {
        setSearchTreeParameters();

        // MIN_COST
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
        assertTrue(SearchTree.costSatisfiesStopCriterion(0, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(0.1, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(10, searchTreeParameters));

        // MIN_OBJECTIVE
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
        assertFalse(SearchTree.costSatisfiesStopCriterion(-10, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(-0.1, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(0, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(0.1, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(10, searchTreeParameters));

        // AT_TARGET_OBJECTIVE_VALUE
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        assertTrue(SearchTree.costSatisfiesStopCriterion(-10, searchTreeParameters));
        assertTrue(SearchTree.costSatisfiesStopCriterion(-0.1, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(0, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(0.1, searchTreeParameters));
        assertFalse(SearchTree.costSatisfiesStopCriterion(10, searchTreeParameters));
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
