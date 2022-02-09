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
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.SearchTreeComputer;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.IteratingLinearOptimizer;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.TreeParameters;
import com.farao_community.farao.util.MultipleNetworkPool;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;

    private SearchTree searchTree;

    private SearchTreeInput searchTreeInput;

    private Network network;
    private NetworkAction networkAction;
    private List<NetworkActionCombination> availableNaCombinations;
    private Set<NetworkAction> availableNetworkActions;
    private RangeAction<?> rangeAction1;
    private RangeAction<?> rangeAction2;
    private Set<RangeAction<?>> availableRangeActions;
    private PrePerimeterResult prePerimeterOutput;
    private SearchTreeComputer searchTreeComputer;
    private SearchTreeProblem searchTreeProblem;
    private SearchTreeBloomer bloomer;
    private ObjectiveFunction objectiveFunction;
    private IteratingLinearOptimizer iteratingLinearOptimizer;

    private Leaf rootLeaf;

    private TreeParameters treeParameters;

    private int maximumSearchDepth;
    private int leavesInParallel;

    private LinearOptimizerParameters linearOptimizerParameters;

    @Before
    public void setUp() throws Exception {
        searchTree = Mockito.spy(new SearchTree());
        setSearchTreeInput();
        treeParameters = Mockito.mock(TreeParameters.class);
        setTreeParameters();
        linearOptimizerParameters = Mockito.mock(LinearOptimizerParameters.class);
        when(linearOptimizerParameters.getObjectiveFunction()).thenReturn(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        mockNetworkPool(network);
    }

    private void setTreeParameters() {
        maximumSearchDepth = 1;
        leavesInParallel = 1;
        when(treeParameters.getMaximumSearchDepth()).thenReturn(maximumSearchDepth);
        when(treeParameters.getLeavesInParallel()).thenReturn(leavesInParallel);
        when(treeParameters.getMaxRa()).thenReturn(Integer.MAX_VALUE);
        when(treeParameters.getMaxTso()).thenReturn(Integer.MAX_VALUE);
        when(treeParameters.getMaxPstPerTso()).thenReturn(new HashMap<>());
    }

    private void setSearchTreeInput() {
        searchTreeInput = Mockito.mock(SearchTreeInput.class);
        network = Mockito.mock(Network.class);
        when(searchTreeInput.getNetwork()).thenReturn(network);
        availableNetworkActions = new HashSet<>();
        availableNaCombinations = new ArrayList<>();
        when(searchTreeInput.getNetworkActions()).thenReturn(availableNetworkActions);
        availableRangeActions = new HashSet<>();
        when(searchTreeInput.getRangeActions()).thenReturn(availableRangeActions);
        prePerimeterOutput = Mockito.mock(PrePerimeterResult.class);
        when(searchTreeInput.getPrePerimeterOutput()).thenReturn(prePerimeterOutput);
        searchTreeComputer = Mockito.mock(SearchTreeComputer.class);
        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        when(searchTreeComputer.getSensitivityComputer(availableRangeActions)).thenReturn(sensitivityComputer);
        when(searchTreeInput.getSearchTreeComputer()).thenReturn(searchTreeComputer);
        searchTreeProblem = Mockito.mock(SearchTreeProblem.class);
        when(searchTreeInput.getSearchTreeProblem()).thenReturn(searchTreeProblem);
        bloomer = Mockito.mock(SearchTreeBloomer.class);
        when(searchTreeInput.getSearchTreeBloomer()).thenReturn(bloomer);
        objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(objectiveFunction);
        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);
        when(searchTreeInput.getIteratingLinearOptimizer()).thenReturn(iteratingLinearOptimizer);
        State optimizedState = Mockito.mock(State.class);
        when(optimizedState.getContingency()).thenReturn(Optional.empty());
        when(optimizedState.getInstant()).thenReturn(Instant.PREVENTIVE);
        when(searchTreeInput.getOptimizedState()).thenReturn(optimizedState);
        rootLeaf = Mockito.mock(Leaf.class);
        when(bloomer.bloom(rootLeaf, availableNetworkActions)).thenReturn(availableNaCombinations);

        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        when(cnec.isOptimized()).thenReturn(true);
        when(searchTreeInput.getFlowCnecs()).thenReturn(Set.of(cnec));
    }

    @Test
    public void runOnAFailingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(rootLeaf, result);
    }

    @Test
    public void runWithoutOptimizingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        setStopCriterionAtTargetObjectiveValue(3.);

        double leafCost = 2.;
        when(rootLeaf.getCost()).thenReturn(leafCost);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(rootLeaf, result);
        assertEquals(leafCost, result.getCost(), DOUBLE_TOLERANCE);
    }

    private void setStopCriterionAtTargetObjectiveValue(double value) {
        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.getTargetObjectiveValue()).thenReturn(value);
    }

    @Test
    public void runAndOptimizeOnlyRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        when(rootLeaf.getCost()).thenReturn(2.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);
        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(rootLeaf, result);
        assertEquals(2., result.getCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rootLeafMeetsTargetObjectiveValue() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(3.);
        searchTreeWithOneChildLeaf();
        when(rootLeaf.getCost()).thenReturn(4., 2.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);
        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(rootLeaf, result);
        assertEquals(2., result.getCost(), DOUBLE_TOLERANCE);
    }

    public class MockedNetworkPool extends MultipleNetworkPool {

        public MockedNetworkPool(Network network, String targetVariant, int parallelism) {
            super(network, targetVariant, parallelism);
        }

        @Override
        protected void initAvailableNetworks(Network network) {
            this.networksQueue.offer(network);
        }

        @Override
        protected void cleanVariants(Network network) {
            // do nothing
        }
    }

    @Test
    public void runAndIterateOnTreeWithChildLeafInError() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        searchTreeWithOneChildLeaf();

        when(rootLeaf.getCost()).thenReturn(4.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);

        Leaf childLeaf = Mockito.mock(Leaf.class);
        when(childLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(network, new NetworkActionCombination(networkAction));

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(rootLeaf, result);
        assertEquals(4., result.getCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void runAndIterateOnTreeWithABetterChildLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        searchTreeWithOneChildLeaf();
        Leaf childLeaf = Mockito.mock(Leaf.class);

        double rootLeafCostAfterOptim = 4.;
        double childLeafCostAfterOptim = 3.;

        mockLeafsCosts(rootLeafCostAfterOptim, childLeafCostAfterOptim, childLeaf);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(childLeaf, result);
    }

    @Test
    public void runAndIterateOnTreeWithAWorseChildLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        searchTreeWithOneChildLeaf();
        Leaf childLeaf = Mockito.mock(Leaf.class);

        double rootLeafCostAfterOptim = 4.;
        double childLeafCostAfterOptim = 5.;

        mockLeafsCosts(rootLeafCostAfterOptim, childLeafCostAfterOptim, childLeaf);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(rootLeaf, result);
    }

    @Test
    public void runAndIterateOnTreeStopCriterionReached() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(0.);

        NetworkAction networkAction1 = Mockito.mock(NetworkAction.class);
        NetworkAction networkAction2 = Mockito.mock(NetworkAction.class);
        when(networkAction1.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(networkAction2.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
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
        Mockito.doReturn(childLeaf1).when(searchTree).createChildLeaf(eq(network), eq(availableNaCombinations.get(0)));

        when(childLeaf2.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf2.getCost()).thenReturn(childLeaf2CostAfterOptim);
        Mockito.doReturn(childLeaf2).when(searchTree).createChildLeaf(eq(network), eq(availableNaCombinations.get(1)));

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(childLeaf1, result);
    }

    @Test
    public void tooManyRangeActions() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();

        String tsoName = "TSO";
        raoWithRangeActionsForTso(tsoName);
        int maxPstOfTso = 1;
        setMaxPstPerTso(tsoName, maxPstOfTso);
        mockRootLeafCost(5.);
        RangeAction<?> rangeAction4 = Mockito.mock(PstRangeAction.class);
        when(rangeAction4.getOperator()).thenReturn("TSO - not in map");
        when(rangeAction4.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        availableRangeActions.add(rangeAction4);

        searchTree.setTreeParameters(treeParameters);
        searchTree.setAvailableRangeActions(availableRangeActions);
        Map<RangeAction<?>, Double> prePerimeterRangeActionSetPoints = new HashMap<>();
        availableRangeActions.forEach(rangeAction -> prePerimeterRangeActionSetPoints.put(rangeAction, 0.));
        searchTree.setPrePerimeterRangeActionSetPoints(prePerimeterRangeActionSetPoints);
        Set<RangeAction<?>> rangeActionsToOptimize = searchTree.applyRangeActionsFilters(rootLeaf, availableRangeActions, false);

        assert rangeActionsToOptimize.contains(rangeAction2);
        assertFalse(rangeActionsToOptimize.contains(rangeAction1));

        assert rangeActionsToOptimize.contains(rangeAction4);
    }

    @Test
    public void tooManyRangeActions2() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();

        String tsoName = "TSO";
        raoWithRangeActionsForTso(tsoName);
        int maxPstOfTso = 1;
        setMaxPstPerTso(tsoName, maxPstOfTso);
        mockRootLeafCost(5.);

        searchTree.setTreeParameters(treeParameters);
        searchTree.setAvailableRangeActions(availableRangeActions);
        Map<RangeAction<?>, Double> prePerimeterRangeActionSetPoints = new HashMap<>();
        availableRangeActions.forEach(rangeAction -> prePerimeterRangeActionSetPoints.put(rangeAction, 0.));
        searchTree.setPrePerimeterRangeActionSetPoints(prePerimeterRangeActionSetPoints);
        Set<RangeAction<?>> rangeActionsToOptimize = searchTree.applyRangeActionsFilters(rootLeaf, availableRangeActions, false);

        assertTrue(rangeActionsToOptimize.contains(rangeAction2));
        assertFalse(rangeActionsToOptimize.contains(rangeAction1));
    }

    @Test
    public void optimizeRootLeafWithRangeActions() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();

        String tsoName = "TSO";
        raoWithRangeActionsForTso(tsoName);
        int maxPstOfTso = 2;
        setMaxPstPerTso(tsoName, maxPstOfTso);

        mockRootLeafCost(5.);
        when(rootLeaf.getOptimizedSetPoint(rangeAction2)).thenReturn(3.);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true).get();
        assertEquals(3., result.getOptimizedSetPoint(rangeAction2), DOUBLE_TOLERANCE);
    }

    @Test
    public void maxCurativeRaLimitNumberOfAvailableRangeActions() {
        raoWithRangeActionsForTso("TSO");
        int maxCurativeRa = 2;
        setMaxRa(maxCurativeRa);
        Leaf childLeaf = Mockito.mock(Leaf.class);
        when(childLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(networkAction));
        FlowCnec mostLimitingElement = Mockito.mock(FlowCnec.class);
        when(childLeaf.getMostLimitingElements(1)).thenReturn(Collections.singletonList(mostLimitingElement));
        searchTree.setTreeParameters(treeParameters);
        searchTree.setAvailableRangeActions(availableRangeActions);
        Map<RangeAction<?>, Double> prePerimeterRangeActionSetPoints = new HashMap<>();
        availableRangeActions.forEach(rangeAction -> prePerimeterRangeActionSetPoints.put(rangeAction, 0.));
        searchTree.setPrePerimeterRangeActionSetPoints(prePerimeterRangeActionSetPoints);
        Set<RangeAction<?>> rangeActionsToOptimize = searchTree.applyRangeActionsFilters(childLeaf, availableRangeActions, false);
        assertEquals(1, rangeActionsToOptimize.size());
    }

    @Test
    public void maxCurativeRaPreventToOptimizeRangeActions() {
        raoWithRangeActionsForTso("TSO");
        int maxCurativeRa = 1;
        setMaxRa(maxCurativeRa);
        Leaf childLeaf = Mockito.mock(Leaf.class);
        when(childLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(networkAction));
        FlowCnec mostLimitingElement = Mockito.mock(FlowCnec.class);
        when(childLeaf.getMostLimitingElements(1)).thenReturn(Collections.singletonList(mostLimitingElement));
        searchTree.setTreeParameters(treeParameters);
        searchTree.setAvailableRangeActions(availableRangeActions);
        Set<RangeAction<?>> rangeActionsToOptimize = searchTree.applyRangeActionsFilters(childLeaf, availableRangeActions, false);
        assertEquals(0, rangeActionsToOptimize.size());
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
        when(rootLeaf.getSensitivityValue(mostLimitingElement, rangeAction1, Unit.MEGAWATT)).thenReturn(1.);
        when(rootLeaf.getSensitivityValue(mostLimitingElement, rangeAction2, Unit.MEGAWATT)).thenReturn(2.);
    }

    private void mockRootLeafCost(double cost) throws Exception {
        when(rootLeaf.getCost()).thenReturn(cost);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);
    }

    private void setMaxPstPerTso(String tsoName, int maxPstOfTso) {
        Map<String, Integer> maxPstPerTso = new HashMap<>();
        maxPstPerTso.put(tsoName, maxPstOfTso);
        when(treeParameters.getMaxPstPerTso()).thenReturn(maxPstPerTso);
    }

    private void mockLeafsCosts(double rootLeafCostAfterOptim, double childLeafCostAfterOptim, Leaf childLeaf) throws Exception {
        mockRootLeafCost(rootLeafCostAfterOptim);
        when(childLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf.getCost()).thenReturn(childLeafCostAfterOptim);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(eq(network), any());
    }

    private void mockNetworkPool(Network network) throws Exception {
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        String workingVariantId = "ID";
        when(variantManager.getWorkingVariantId()).thenReturn(workingVariantId);
        when(network.getVariantManager()).thenReturn(variantManager);
        MockedNetworkPool faraoNetworkPool = new MockedNetworkPool(network, workingVariantId, leavesInParallel);
        Mockito.doReturn(faraoNetworkPool).when(searchTree).makeFaraoNetworkPool(network, leavesInParallel);
    }

    private void searchTreeWithOneChildLeaf() {
        networkAction = Mockito.mock(NetworkAction.class);
        when(networkAction.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        availableNetworkActions.add(networkAction);
        availableNaCombinations.add(new NetworkActionCombination(networkAction));
    }

    private void setStopCriterionAtMinObjective() {
        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
    }

    private void raoWithoutLoopFlowLimitation() {
        when(linearOptimizerParameters.isRaoWithLoopFlowLimitation()).thenReturn(false);
    }

    private void setMaxRa(int maxRa) {
        when(treeParameters.getMaxRa()).thenReturn(maxRa);
    }

    @Test
    public void testIsNetworkActionAvailable() {
        Crac crac = CommonCracCreation.create();
        State optimizedState = mock(State.class);
        when(optimizedState.getInstant()).thenReturn(Instant.CURATIVE);

        FlowCnec flowCnec = crac.getFlowCnec("cnec1stateCurativeContingency1");
        FlowResult flowResult = mock(FlowResult.class);

        NetworkAction na1 = crac.newNetworkAction().withId("na1")
            .newTopologicalAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        assertTrue(SearchTree.isRemedialActionAvailable(na1, optimizedState, flowResult));

        NetworkAction na2 = crac.newNetworkAction().withId("na2")
            .newTopologicalAction().withNetworkElement("ne2").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintUsageRule().withInstant(Instant.CURATIVE).withFlowCnec(flowCnec.getId()).add()
            .add();
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) na2.getUsageRules().get(0);

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(10.);
        assertFalse(SearchTree.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult));
        assertFalse(SearchTree.isRemedialActionAvailable(na2, optimizedState, flowResult));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(-10.);
        assertTrue(SearchTree.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult));
        assertTrue(SearchTree.isRemedialActionAvailable(na2, optimizedState, flowResult));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(0.);
        assertTrue(SearchTree.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult));
        assertTrue(SearchTree.isRemedialActionAvailable(na2, optimizedState, flowResult));

        when(optimizedState.getInstant()).thenReturn(Instant.PREVENTIVE);
        assertFalse(SearchTree.isRemedialActionAvailable(na1, optimizedState, flowResult));
        assertFalse(SearchTree.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult));
        assertFalse(SearchTree.isRemedialActionAvailable(na2, optimizedState, flowResult));
    }

    @Test
    public void testPurelyVirtualStopCriterion() {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(-30.);

        FlowCnec mnec = Mockito.mock(FlowCnec.class);
        when(mnec.isOptimized()).thenReturn(false);
        when(searchTreeInput.getFlowCnecs()).thenReturn(Set.of(mnec));

        RangeAction ra = Mockito.mock(RangeAction.class);
        when(ra.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        when(searchTreeInput.getRangeActions()).thenReturn(Set.of(ra));

        double leafCost = 0.;
        when(rootLeaf.getCost()).thenReturn(leafCost);
        when(rootLeaf.getVirtualCost()).thenReturn(0.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);
        // rootLeaf should not be optimized : its virtual cost is zero so stop criterion is already reached
        doThrow(FaraoException.class).when(rootLeaf).optimize(any(), any(), any());

        try {
            searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true);
        } catch (FaraoException e) {
            fail("Should not have optimized rootleaf as it had already reached the stop criterion");
        }
    }

    @Test
    public void testLogsVerbose() {
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        when(rootLeaf.toString()).thenReturn("root leaf description");
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);

        String expectedLog1 = "[INFO] Evaluating root leaf";
        String expectedLog2 = "[INFO] Could not evaluate leaf: root leaf description";
        String expectedLog3 = "[INFO] Scenario \"preventive\": initial cost = 0.00 (functional: 0.00, virtual: 0.00), no preventive remedial actions activated, cost after PRA = 0.00 (functional: 0.00, virtual: 0.00)";

        ListAppender<ILoggingEvent> technical = getLogs(TechnicalLogs.class);
        ListAppender<ILoggingEvent> business = getLogs(RaoBusinessLogs.class);
        searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, true);
        assertEquals(1, technical.list.size());
        assertEquals(2, business.list.size());
        assertEquals(expectedLog1, technical.list.get(0).toString());
        assertEquals(expectedLog2, business.list.get(0).toString());
        assertEquals(expectedLog3, business.list.get(1).toString());
    }

    @Test
    public void testLogsDontVerbose() {
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        when(rootLeaf.toString()).thenReturn("root leaf description");
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);

        String expectedLog1 = "[INFO] Evaluating root leaf";
        String expectedLog2 = "[INFO] Could not evaluate leaf: root leaf description";
        String expectedLog3 = "[INFO] Scenario \"preventive\": initial cost = 0.00 (functional: 0.00, virtual: 0.00), no preventive remedial actions activated, cost after PRA = 0.00 (functional: 0.00, virtual: 0.00)";

        ListAppender<ILoggingEvent> technical = getLogs(TechnicalLogs.class);
        ListAppender<ILoggingEvent> business = getLogs(RaoBusinessLogs.class);
        searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters, false);
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
}
