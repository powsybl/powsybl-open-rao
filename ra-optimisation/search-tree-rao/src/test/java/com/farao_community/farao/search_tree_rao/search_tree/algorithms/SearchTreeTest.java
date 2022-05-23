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
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.GlobalRemedialActionLimitationParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.NetworkActionParameters;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.commons.parameters.TreeParameters;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
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
    private final State optimizedState = Mockito.mock(State.class);
    private OptimizationPerimeter optimizationPerimeter;
    private NetworkAction networkAction;
    private List<NetworkActionCombination> availableNaCombinations = new ArrayList<>();
    private Set<NetworkAction> availableNetworkActions;
    private RangeAction<?> rangeAction1;
    private RangeAction<?> rangeAction2;
    private Set<RangeAction<?>> availableRangeActions;
    private PrePerimeterResult prePerimeterResult;
    private AppliedRemedialActions appliedRemedialActions;
    private ObjectiveFunction objectiveFunction;

    private Leaf rootLeaf;

    private SearchTreeParameters searchTreeParameters;
    private TreeParameters treeParameters;
    private GlobalRemedialActionLimitationParameters raLimitationParameters;
    private NetworkActionParameters networkActionParameters;

    private int maximumSearchDepth;
    private int leavesInParallel;

    @Before
    public void setUp() throws Exception {
        setSearchTreeInput();
        searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        setSearchTreeParameters();
        searchTree = Mockito.spy(new SearchTree(searchTreeInput, searchTreeParameters, true));
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        mockNetworkPool(network);
    }

    private void setSearchTreeParameters() {
        maximumSearchDepth = 1;
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
        networkActionParameters = Mockito.mock(NetworkActionParameters.class);
        when(searchTreeParameters.getNetworkActionParameters()).thenReturn(networkActionParameters);
    }

    private void setSearchTreeInput() {
        searchTreeInput = Mockito.mock(SearchTreeInput.class);
        appliedRemedialActions = Mockito.mock(AppliedRemedialActions.class);
        when(searchTreeInput.getPreOptimizationAppliedNetworkActions()).thenReturn(appliedRemedialActions);
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
        objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(objectiveFunction);
        when(optimizedState.getContingency()).thenReturn(Optional.empty());
        when(optimizedState.getInstant()).thenReturn(Instant.PREVENTIVE);
        rootLeaf = Mockito.mock(Leaf.class);
        when(searchTreeInput.getToolProvider()).thenReturn(Mockito.mock(ToolProvider.class));
    }

    @Test
    public void runOnAFailingRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        OptimizationResult result = searchTree.run().get();
        assertEquals(rootLeaf, result);
    }

    @Test
    public void runWithoutOptimizingRootLeaf() throws Exception {
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
        when(treeParameters.getStopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.getTargetObjectiveValue()).thenReturn(value);
    }

    @Test
    public void runAndOptimizeOnlyRootLeaf() throws Exception {
        raoWithoutLoopFlowLimitation();
        setStopCriterionAtMinObjective();
        when(rootLeaf.getCost()).thenReturn(2.);
        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        OptimizationResult result = searchTree.run().get();
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
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        OptimizationResult result = searchTree.run().get();
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
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        Leaf childLeaf = Mockito.mock(Leaf.class);
        when(childLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        Mockito.doReturn(childLeaf).when(searchTree).createChildLeaf(network, new NetworkActionCombination(networkAction));

        OptimizationResult result = searchTree.run().get();
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

        OptimizationResult result = searchTree.run().get();
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

        OptimizationResult result = searchTree.run().get();
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
        Mockito.doReturn(childLeaf1).when(searchTree).createChildLeaf(any(), eq(availableNaCombinations.get(0)));

        when(childLeaf2.getStatus()).thenReturn(Leaf.Status.EVALUATED, Leaf.Status.OPTIMIZED);
        when(childLeaf2.getCost()).thenReturn(childLeaf2CostAfterOptim);
        Mockito.doReturn(childLeaf2).when(searchTree).createChildLeaf(any(), eq(availableNaCombinations.get(1)));

        OptimizationResult result = searchTree.run().get();
        assertEquals(childLeaf1, result);
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
        when(networkAction.getOperator()).thenReturn("operator");
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
    public void testIsNetworkActionAvailable() {
        Crac crac = CommonCracCreation.create();
        when(optimizedState.getInstant()).thenReturn(Instant.CURATIVE);

        FlowCnec flowCnec = crac.getFlowCnec("cnec1stateCurativeContingency1");
        FlowResult flowResult = mock(FlowResult.class);

        NetworkAction na1 = crac.newNetworkAction().withId("na1")
            .newTopologicalAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        assertTrue(RaoUtil.isRemedialActionAvailable(na1, optimizedState, flowResult, crac.getFlowCnecs(), network));

        NetworkAction na2 = crac.newNetworkAction().withId("na2")
            .newTopologicalAction().withNetworkElement("ne2").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintUsageRule().withInstant(Instant.CURATIVE).withFlowCnec(flowCnec.getId()).add()
            .add();
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) na2.getUsageRules().get(0);

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(10.);
        assertFalse(RaoUtil.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult));
        assertFalse(RaoUtil.isRemedialActionAvailable(na2, optimizedState, flowResult, crac.getFlowCnecs(), network));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(-10.);
        assertTrue(RaoUtil.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult));
        assertTrue(RaoUtil.isRemedialActionAvailable(na2, optimizedState, flowResult, crac.getFlowCnecs(), network));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(0.);
        assertTrue(RaoUtil.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult));
        assertTrue(RaoUtil.isRemedialActionAvailable(na2, optimizedState, flowResult, crac.getFlowCnecs(), network));

        when(optimizedState.getInstant()).thenReturn(Instant.PREVENTIVE);
        assertFalse(RaoUtil.isRemedialActionAvailable(na1, optimizedState, flowResult, crac.getFlowCnecs(), network));
        assertFalse(RaoUtil.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult));
        assertFalse(RaoUtil.isRemedialActionAvailable(na2, optimizedState, flowResult, crac.getFlowCnecs(), network));
    }

    @Test
    public void testPurelyVirtualStopCriterion() {
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
        doThrow(FaraoException.class).when(rootLeaf).optimize(any(), any());

        try {
            searchTree.run();
        } catch (FaraoException e) {
            fail("Should not have optimized rootleaf as it had already reached the stop criterion");
        }
    }

    @Test
    public void testLogsVerbose() {
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        when(rootLeaf.toString()).thenReturn("root leaf description");
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        String expectedLog1 = "[INFO] Evaluating root leaf";
        String expectedLog2 = "[INFO] Could not evaluate leaf: root leaf description";
        String expectedLog3 = "[INFO] Scenario \"preventive\": initial cost = 0.00 (functional: 0.00, virtual: 0.00), no remedial actions activated, cost after PRA = 0.00 (functional: 0.00, virtual: 0.00)";

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
    public void testLogsDontVerbose() {
        searchTree = Mockito.spy(new SearchTree(searchTreeInput, searchTreeParameters, false));
        raoWithoutLoopFlowLimitation();

        when(rootLeaf.getStatus()).thenReturn(Leaf.Status.ERROR);
        when(rootLeaf.toString()).thenReturn("root leaf description");
        Mockito.doReturn(rootLeaf).when(searchTree).makeLeaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        String expectedLog1 = "[INFO] Evaluating root leaf";
        String expectedLog2 = "[INFO] Could not evaluate leaf: root leaf description";
        String expectedLog3 = "[INFO] Scenario \"preventive\": initial cost = 0.00 (functional: 0.00, virtual: 0.00), no remedial actions activated, cost after PRA = 0.00 (functional: 0.00, virtual: 0.00)";

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
}
