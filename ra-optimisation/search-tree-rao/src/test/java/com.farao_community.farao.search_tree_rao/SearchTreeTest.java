/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.results.OptimizationResult;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityComputer;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class, SearchTreeRaoLogger.class, SystematicSensitivityInterface.class, Leaf.class, SearchTree.class, RaoUtil.class, IteratingLinearOptimizer.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
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
        searchTree = new SearchTree();
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
    }

    private void setSearchTreeInput() throws Exception {
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

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Test
    public void runOnAFailingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        PowerMockito.whenNew(Leaf.class).withArguments(network, prePerimeterOutput).thenReturn(rootLeaf);

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
        PowerMockito.whenNew(Leaf.class).withAnyArguments().thenReturn(rootLeaf);

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
        PowerMockito.whenNew(Leaf.class).withAnyArguments().thenReturn(rootLeaf);
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
        PowerMockito.whenNew(Leaf.class).withAnyArguments().thenReturn(rootLeaf);
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
        PowerMockito.whenNew(Leaf.class).withArguments(network, prePerimeterOutput).thenReturn(rootLeaf);

        Leaf childLeaf = Mockito.mock(Leaf.class);
        Mockito.when(childLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        PowerMockito.whenNew(Leaf.class).withArguments(network, rootLeaf.getNetworkActions(), networkAction, rootLeaf).thenReturn(childLeaf);

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
    public void optimizeRootLeafWithTooManyRangeActions() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();

        String tsoName = "TSO";
        raoWithRangeActionsForTso(tsoName);
        int maxPstOfTso = 1;
        setMaxPstPerTso(tsoName, maxPstOfTso);

        Mockito.when(rootLeaf.getCost()).thenReturn(5.);
        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        PowerMockito.whenNew(Leaf.class).withAnyArguments().thenReturn(rootLeaf);
        Mockito.when(rootLeaf.getOptimizedSetPoint(rangeAction2)).thenReturn(3.);

        /*SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        Set<RangeAction> rangeActionSet = Collections.singleton(rangeAction2);
        Mockito.when(searchTreeComputer.getSensitivityComputer(rangeActionSet)).thenReturn(sensitivityComputer);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        Mockito.when(searchTreeProblem.getLeafProblem(rangeActionSet)).thenReturn(leafProblem);
        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(linearOptimizationResult.getRangeActions()).thenReturn(rangeActionSet);*/

        OptimizationResult result = searchTree.run(searchTreeInput, treeParameters, linearOptimizerParameters).get();
        assertEquals(3., result.getOptimizedSetPoint(rangeAction2), DOUBLE_TOLERANCE);
        assertEquals(rootLeaf, result);
        // assert result.getRangeActions().contains(rangeAction2); too difficult to mock
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

    private void raoWithRangeActionsForTso(String tsoName) {
        rangeAction1 = Mockito.mock(PstRangeAction.class);
        rangeAction2 = Mockito.mock(PstRangeAction.class);
        Mockito.when(rangeAction1.getOperator()).thenReturn(tsoName);
        Mockito.when(rangeAction2.getOperator()).thenReturn(tsoName);
        availableRangeActions.add(rangeAction1);
        availableRangeActions.add(rangeAction2);

        BranchCnec mostLimitingElement = Mockito.mock(BranchCnec.class);
        Mockito.when(rootLeaf.getMostLimitingElements(1)).thenReturn(Collections.singletonList(mostLimitingElement));
        Mockito.when(rootLeaf.getSensitivityValue(mostLimitingElement, rangeAction1, Unit.MEGAWATT)).thenReturn(1.);
        Mockito.when(rootLeaf.getSensitivityValue(mostLimitingElement, rangeAction2, Unit.MEGAWATT)).thenReturn(2.);
    }

    private void mockRootLeafCost(double cost) throws Exception {
        Mockito.when(rootLeaf.getCost()).thenReturn(cost);
        Mockito.when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        PowerMockito.whenNew(Leaf.class).withArguments(network, prePerimeterOutput).thenReturn(rootLeaf);
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
        PowerMockito.whenNew(Leaf.class).withArguments(network, rootLeaf.getNetworkActions(), networkAction, rootLeaf).thenReturn(childLeaf);
    }

    private void mockNetworkPool(Network network) throws Exception {
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        String workingVariantId = "ID";
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn(workingVariantId);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        MockedFaraoNetworkPool faraoNetworkPool = new MockedFaraoNetworkPool(network, workingVariantId, leavesInParallel);
        PowerMockito.whenNew(FaraoNetworkPool.class).withArguments(network, workingVariantId, 1).thenReturn(faraoNetworkPool);
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
}
