/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.results.OptimizationResult;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import com.farao_community.farao.rao_commons.SensitivityComputer;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;

    private SearchTree searchTree;

    private SearchTreeInput searchTreeInput;

    private Network network;
    private NetworkAction networkAction;
    private Set<NetworkAction> availableNetworkActions;
    private RangeAction rangeAction1;
    private RangeAction rangeAction2;
    private RangeAction rangeAction3;
    private Set<RangeAction> availableRangeActions;
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
        mockNetworkPool(network);

    }

    private void setTreeParameters() {
        maximumSearchDepth = 1;
        leavesInParallel = 1;
        Mockito.when(treeParameters.getMaximumSearchDepth()).thenReturn(maximumSearchDepth);
        Mockito.when(treeParameters.getLeavesInParallel()).thenReturn(leavesInParallel);
        Mockito.when(treeParameters.getMaxRa()).thenReturn(Integer.MAX_VALUE);
        Mockito.when(treeParameters.getMaxPstPerTso()).thenReturn(new HashMap<>());
    }

    private void setSearchTreeInput() {
        searchTreeInput = Mockito.mock(SearchTreeInput.class);
        network = Mockito.mock(Network.class);
        Mockito.when(searchTreeInput.getNetwork()).thenReturn(network);
        availableNetworkActions = new HashSet<>();
        Mockito.when(searchTreeInput.getNetworkActions()).thenReturn(availableNetworkActions);
        availableRangeActions = new HashSet<>();
        Mockito.when(searchTreeInput.getRangeActions()).thenReturn(availableRangeActions);
        prePerimeterOutput = Mockito.mock(PrePerimeterResult.class);
        Mockito.when(searchTreeInput.getPrePerimeterOutput()).thenReturn(prePerimeterOutput);
        searchTreeComputer = Mockito.mock(SearchTreeComputer.class);
        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        Mockito.when(searchTreeComputer.getSensitivityComputer(availableRangeActions)).thenReturn(sensitivityComputer);
        Mockito.when(searchTreeInput.getSearchTreeComputer()).thenReturn(searchTreeComputer);
        searchTreeProblem = Mockito.mock(SearchTreeProblem.class);
        Mockito.when(searchTreeInput.getSearchTreeProblem()).thenReturn(searchTreeProblem);
        bloomer = Mockito.mock(SearchTreeBloomer.class);
        Mockito.when(searchTreeInput.getSearchTreeBloomer()).thenReturn(bloomer);
        objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        Mockito.when(searchTreeInput.getObjectiveFunction()).thenReturn(objectiveFunction);
        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);
        Mockito.when(searchTreeInput.getIteratingLinearOptimizer()).thenReturn(iteratingLinearOptimizer);
        rootLeaf = Mockito.mock(Leaf.class);
        Mockito.when(bloomer.bloom(rootLeaf, availableNetworkActions)).thenReturn(availableNetworkActions);
    }

    @Test
    public void runOnAFailingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
        assertEquals(rootLeaf, result);
    }

    @Test
    public void runWithoutOptimizingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        setStopCriterionAtTargetObjectiveValue(3.);

        double leafCost = 2.;
        Mockito.when(rootLeaf.getCost()).thenReturn(leafCost);
        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
        assertEquals(rootLeaf, result);
        assertEquals(leafCost, result.getCost(), DOUBLE_TOLERANCE);
    }

    private void setStopCriterionAtTargetObjectiveValue(double value) {
        Mockito.when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        Mockito.when(treeParameters.getTargetObjectiveValue()).thenReturn(value);
    }

    @Test
    public void runAndOptimizeOnlyRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        Mockito.when(rootLeaf.getCost()).thenReturn(2.);
        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);
        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
        assertEquals(rootLeaf, result);
        assertEquals(2., result.getCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rootLeafMeetsTargetObjectiveValue() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtTargetObjectiveValue(3.);
        searchTreeWithOneChildLeaf();
        Mockito.when(rootLeaf.getCost()).thenReturn(4., 2.);
        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);
        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
        assertEquals(rootLeaf, result);
        assertEquals(2., result.getCost(), DOUBLE_TOLERANCE);
    }

    public class MockedFaraoNetworkPool extends FaraoNetworkPool {

        public MockedFaraoNetworkPool(Network network, String targetVariant, int parallelism) {
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

        Mockito.when(rootLeaf.getCost()).thenReturn(4.);
        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);

        Leaf childLeaf = Mockito.mock(Leaf.class);
        Mockito.when(childLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(network, networkAction);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
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

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
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

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
        assertEquals(rootLeaf, result);
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
        RangeAction rangeAction4 = Mockito.mock(PstRangeAction.class);
        Mockito.when(rangeAction4.getOperator()).thenReturn("TSO - not in map");
        availableRangeActions.add(rangeAction4);

        searchTree.setTreeParameters(treeParameters);
        searchTree.setAvailableRangeActions(availableRangeActions);
        Set<RangeAction> rangeActionsToOptimize = searchTree.getRangeActionsToOptimize(rootLeaf);

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
        Set<RangeAction> rangeActionsToOptimize = searchTree.getRangeActionsToOptimize(rootLeaf);

        assertTrue(rangeActionsToOptimize.contains(rangeAction2));
        assertFalse(rangeActionsToOptimize.contains(rangeAction1));
    }

    @Test
    public void testIsRangeActionUsed() {
        rangeAction1 = Mockito.mock(RangeAction.class);
        rangeAction2 = Mockito.mock(RangeAction.class);
        rangeAction3 = Mockito.mock(RangeAction.class);

        Mockito.when(rootLeaf.getRangeActions()).thenReturn(Set.of(rangeAction1, rangeAction2));
        Mockito.doReturn(0.).when(searchTree).getInitialRangeActionSetPoint(rangeAction1);
        Mockito.doReturn(0.).when(searchTree).getInitialRangeActionSetPoint(rangeAction2);
        Mockito.doReturn(0.).when(searchTree).getInitialRangeActionSetPoint(rangeAction3);
        Mockito.when(rootLeaf.getOptimizedSetPoint(rangeAction1)).thenReturn(0.);
        Mockito.when(rootLeaf.getOptimizedSetPoint(rangeAction2)).thenReturn(2.);
        Mockito.when(rootLeaf.getOptimizedSetPoint(rangeAction3)).thenReturn(3.);

        assertFalse(searchTree.isRangeActionUsed(rangeAction1, rootLeaf));
        assertTrue(searchTree.isRangeActionUsed(rangeAction2, rootLeaf));
        assertFalse(searchTree.isRangeActionUsed(rangeAction3, rootLeaf));
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
        Mockito.when(rootLeaf.getOptimizedSetPoint(rangeAction2)).thenReturn(3.);

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
        assertEquals(3., result.getOptimizedSetPoint(rangeAction2), DOUBLE_TOLERANCE);
    }

    @Test
    public void maxCurativeRaLimitNumberOfAvailableRangeActions() {
        raoWithRangeActionsForTso("TSO");
        int maxCurativeRa = 2;
        setMaxRa(maxCurativeRa);
        Leaf childLeaf = Mockito.mock(Leaf.class);
        Mockito.when(childLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(networkAction));
        FlowCnec mostLimitingElement = Mockito.mock(FlowCnec.class);
        Mockito.when(childLeaf.getMostLimitingElements(1)).thenReturn(Collections.singletonList(mostLimitingElement));
        searchTree.setTreeParameters(treeParameters);
        searchTree.setAvailableRangeActions(availableRangeActions);
        Set<RangeAction> rangeActionsToOptimize = searchTree.getRangeActionsToOptimize(childLeaf);
        assertEquals(1, rangeActionsToOptimize.size());
    }

    @Test
    public void maxCurativeRaPreventToOptimizeRangeActions() {
        raoWithRangeActionsForTso("TSO");
        int maxCurativeRa = 1;
        setMaxRa(maxCurativeRa);
        Leaf childLeaf = Mockito.mock(Leaf.class);
        Mockito.when(childLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(networkAction));
        FlowCnec mostLimitingElement = Mockito.mock(FlowCnec.class);
        Mockito.when(childLeaf.getMostLimitingElements(1)).thenReturn(Collections.singletonList(mostLimitingElement));
        searchTree.setTreeParameters(treeParameters);
        searchTree.setAvailableRangeActions(availableRangeActions);
        Set<RangeAction> rangeActionsToOptimize = searchTree.getRangeActionsToOptimize(childLeaf);
        assertEquals(0, rangeActionsToOptimize.size());
    }

    private void raoWithRangeActionsForTso(String tsoName) {
        rangeAction1 = Mockito.mock(PstRangeAction.class);
        rangeAction2 = Mockito.mock(PstRangeAction.class);
        Mockito.when(rangeAction1.getOperator()).thenReturn(tsoName);
        Mockito.when(rangeAction1.getName()).thenReturn("PST1");
        Mockito.when(rangeAction2.getOperator()).thenReturn(tsoName);
        Mockito.when(rangeAction2.getName()).thenReturn("PST2");
        availableRangeActions.add(rangeAction1);
        availableRangeActions.add(rangeAction2);

        FlowCnec mostLimitingElement = Mockito.mock(FlowCnec.class);
        Mockito.when(rootLeaf.getMostLimitingElements(1)).thenReturn(Collections.singletonList(mostLimitingElement));
        Mockito.when(rootLeaf.getSensitivityValue(mostLimitingElement, rangeAction1, Unit.MEGAWATT)).thenReturn(1.);
        Mockito.when(rootLeaf.getSensitivityValue(mostLimitingElement, rangeAction2, Unit.MEGAWATT)).thenReturn(2.);
    }

    private void mockRootLeafCost(double cost) throws Exception {
        Mockito.when(rootLeaf.getCost()).thenReturn(cost);
        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(network, prePerimeterOutput);
    }

    private void setMaxPstPerTso(String tsoName, int maxPstOfTso) {
        Map<String, Integer> maxPstPerTso = new HashMap<>();
        maxPstPerTso.put(tsoName, maxPstOfTso);
        Mockito.when(treeParameters.getMaxPstPerTso()).thenReturn(maxPstPerTso);
    }

    private void mockLeafsCosts(double rootLeafCostAfterOptim, double childLeafCostAfterOptim, Leaf childLeaf) throws Exception {
        mockRootLeafCost(rootLeafCostAfterOptim);
        Mockito.when(childLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.when(childLeaf.getCost()).thenReturn(childLeafCostAfterOptim);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(network, networkAction);
    }

    private void mockNetworkPool(Network network) throws Exception {
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        String workingVariantId = "ID";
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn(workingVariantId);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        MockedFaraoNetworkPool faraoNetworkPool = new MockedFaraoNetworkPool(network, workingVariantId, leavesInParallel);
        Mockito.doReturn(faraoNetworkPool).when(searchTree).makeFaraoNetworkPool(network, leavesInParallel);
    }

    private void searchTreeWithOneChildLeaf() {
        networkAction = Mockito.mock(NetworkAction.class);
        availableNetworkActions.add(networkAction);
    }

    private void setStopCriterionAtMinObjective() {
        Mockito.when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
    }

    private void raoWithoutLoopFlowLimitation() {
        Mockito.when(linearOptimizerParameters.isRaoWithLoopFlowLimitation()).thenReturn(false);
    }

    private void setMaxRa(int maxRa) {
        Mockito.when(treeParameters.getMaxRa()).thenReturn(maxRa);
    }
}
