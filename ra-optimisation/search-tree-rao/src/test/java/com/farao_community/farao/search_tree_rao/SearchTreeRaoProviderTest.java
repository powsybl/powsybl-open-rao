/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.ToolProvider;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.output.PerimeterResult;
import com.farao_community.farao.search_tree_rao.state_tree.StateTree;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SearchTreeRaoProviderTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Crac crac;
    private Network network;
    private FlowResult initialFlowResult;
    private FlowResult preperimFlowResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec cnec4;
    private State state1;
    private State state2;
    private State state3;
    private RangeAction ra1;
    private RangeAction ra2;
    private RangeAction ra3;
    private RangeAction ra4;
    private RangeAction ra5;
    private RangeAction ra6;
    private NetworkAction na1;
    private PrePerimeterResult prePerimeterResult;

    @Before
    public void setUp() {
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);
        cnec3 = Mockito.mock(FlowCnec.class);
        cnec4 = Mockito.mock(FlowCnec.class);
        crac = Mockito.mock(Crac.class);
        network = Mockito.mock(Network.class);
        initialFlowResult = Mockito.mock(FlowResult.class);
        preperimFlowResult = Mockito.mock(FlowResult.class);
        state1 = Mockito.mock(State.class);
        state2 = Mockito.mock(State.class);
        state3 = Mockito.mock(State.class);

        when(crac.getFlowCnecs()).thenReturn(Set.of(cnec1, cnec2, cnec3, cnec4));
        when(crac.getFlowCnecs(state1)).thenReturn(Set.of(cnec1));
        when(crac.getFlowCnecs(state2)).thenReturn(Set.of(cnec2));
        when(crac.getFlowCnecs(state3)).thenReturn(Set.of(cnec3, cnec4));

        ra1 = Mockito.mock(RangeAction.class);
        when(ra1.getMinAdmissibleSetpoint(anyDouble())).thenReturn(-5.);
        when(ra1.getMaxAdmissibleSetpoint(anyDouble())).thenReturn(5.);
        ra2 = Mockito.mock(RangeAction.class);
        when(ra2.getMinAdmissibleSetpoint(anyDouble())).thenReturn(-3.);
        when(ra2.getMaxAdmissibleSetpoint(anyDouble())).thenReturn(3.);
        prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        when(prePerimeterResult.getOptimizedSetPoint(any())).thenReturn(-4.);

        Mockito.when(cnec1.isOptimized()).thenReturn(true);
        Mockito.when(cnec2.isOptimized()).thenReturn(true);
        Mockito.when(cnec3.isOptimized()).thenReturn(true);
        Mockito.when(cnec4.isOptimized()).thenReturn(false);

        Mockito.when(cnec1.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(1000.));
        Mockito.when(cnec1.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnec2.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnec2.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-1500.));
        Mockito.when(cnec3.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnec3.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnec4.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-16000.));
        Mockito.when(cnec4.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-16000.));
    }

    @Test
    public void testRemoveRangeActionsWithWrongInitialSetpoint() {
        Set<RangeAction> rangeActions = new HashSet<>(Set.of(ra1, ra2));
        SearchTreeRaoProvider.removeRangeActionsWithWrongInitialSetpoint(rangeActions, prePerimeterResult);
        assertEquals(Set.of(ra1), rangeActions);
        when(prePerimeterResult.getOptimizedSetPoint(any())).thenReturn(-3.);
        rangeActions = new HashSet<>(Set.of(ra1, ra2));
        SearchTreeRaoProvider.removeRangeActionsWithWrongInitialSetpoint(rangeActions, prePerimeterResult);
        assertEquals(Set.of(ra1, ra2), rangeActions);
    }

    @Test
    public void testComputePerimeterCnecs() {
        assertEquals(Set.of(cnec1), SearchTreeRaoProvider.computePerimeterCnecs(crac, Set.of(state1)));
        assertEquals(Set.of(cnec2), SearchTreeRaoProvider.computePerimeterCnecs(crac, Set.of(state2)));
        assertEquals(Set.of(cnec1, cnec2), SearchTreeRaoProvider.computePerimeterCnecs(crac, Set.of(state1, state2)));
        assertEquals(Set.of(cnec3, cnec4), SearchTreeRaoProvider.computePerimeterCnecs(crac, Set.of(state3)));
        assertEquals(Set.of(cnec2, cnec3, cnec4), SearchTreeRaoProvider.computePerimeterCnecs(crac, Set.of(state2, state3)));
        assertEquals(Set.of(cnec1, cnec2, cnec3, cnec4), SearchTreeRaoProvider.computePerimeterCnecs(crac, null));
    }

    @Test
    public void testCreateObjectiveFunction() throws Exception {
        RaoParameters raoParameters = new RaoParameters();
        ObjectiveFunction objFun;
        ToolProvider toolProvider = Mockito.mock(ToolProvider.class);
        when(toolProvider.getLoopFlowCnecs(any())).thenReturn(new HashSet<>());

        // no virtual cost (except for sensi fallback)
        raoParameters.setMnecViolationCost(0);
        raoParameters.setRaoWithLoopFlowLimitation(false);
        objFun = SearchTreeRaoProvider.createObjectiveFunction(Set.of(cnec1), initialFlowResult, preperimFlowResult, raoParameters, Mockito.mock(LinearOptimizerParameters.class), toolProvider);
        assertNotNull(objFun);
        assertEquals(Set.of("sensitivity-fallback-cost"), objFun.getVirtualCostNames());

        // mnec virtual cost
        raoParameters.setMnecViolationCost(1);
        raoParameters.setRaoWithLoopFlowLimitation(false);
        raoParameters.setRaoWithMnecLimitation(true);
        raoParameters.setMnecViolationCost(0);
        objFun = SearchTreeRaoProvider.createObjectiveFunction(Set.of(cnec1), initialFlowResult, preperimFlowResult, raoParameters, Mockito.mock(LinearOptimizerParameters.class), toolProvider);
        assertNotNull(objFun);
        assertEquals(Set.of("sensitivity-fallback-cost", "mnec-cost"), objFun.getVirtualCostNames());

        // lf cost
        raoParameters.setRaoWithMnecLimitation(false);
        raoParameters.setRaoWithLoopFlowLimitation(true);
        objFun = SearchTreeRaoProvider.createObjectiveFunction(Set.of(cnec1), initialFlowResult, preperimFlowResult, raoParameters, Mockito.mock(LinearOptimizerParameters.class), toolProvider);
        assertNotNull(objFun);
        assertEquals(Set.of("sensitivity-fallback-cost", "loop-flow-cost"), objFun.getVirtualCostNames());

        // mnec and lf costs
        raoParameters.setRaoWithMnecLimitation(true);
        raoParameters.setRaoWithLoopFlowLimitation(true);
        objFun = SearchTreeRaoProvider.createObjectiveFunction(Set.of(cnec1), initialFlowResult, preperimFlowResult, raoParameters, Mockito.mock(LinearOptimizerParameters.class), toolProvider);
        assertNotNull(objFun);
        assertEquals(Set.of("sensitivity-fallback-cost", "mnec-cost", "loop-flow-cost"), objFun.getVirtualCostNames());
    }

    @Test
    public void testGetLargestCnecThreshold() {
        assertEquals(1000., SearchTreeRaoProvider.getLargestCnecThreshold(Set.of(cnec1)), DOUBLE_TOLERANCE);
        assertEquals(1500., SearchTreeRaoProvider.getLargestCnecThreshold(Set.of(cnec2)), DOUBLE_TOLERANCE);
        assertEquals(1500., SearchTreeRaoProvider.getLargestCnecThreshold(Set.of(cnec1, cnec2)), DOUBLE_TOLERANCE);
        assertEquals(1500., SearchTreeRaoProvider.getLargestCnecThreshold(Set.of(cnec1, cnec2, cnec3)), DOUBLE_TOLERANCE);
        assertEquals(1000., SearchTreeRaoProvider.getLargestCnecThreshold(Set.of(cnec1, cnec3)), DOUBLE_TOLERANCE);
        assertEquals(1500., SearchTreeRaoProvider.getLargestCnecThreshold(Set.of(cnec1, cnec2, cnec4)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testBuildSearchTreeInput() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setRaoWithMnecLimitation(true);

        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        when(treeParameters.getMaxRaPerTso()).thenReturn(Map.of("fr", 5));
        when(treeParameters.getMaxPstPerTso()).thenReturn(Map.of("be", 1, "nl", 2));
        when(treeParameters.getMaxTopoPerTso()).thenReturn(new HashMap<>());
        when(treeParameters.getSkipNetworkActionsFarFromMostLimitingElement()).thenReturn(true);

        ToolProvider toolProvider = Mockito.mock(ToolProvider.class);
        when(toolProvider.getLoopFlowCnecs(any())).thenReturn(Set.of(cnec3, cnec4));

        LinearOptimizerParameters linearOptimizerParameters = Mockito.mock(LinearOptimizerParameters.class);

        PrePerimeterResult initialOutput = Mockito.mock(PrePerimeterResult.class);

        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        when(crac.getNetworkActions(state1, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED)).thenReturn(Set.of(na1));
        when(crac.getRangeActions(state1, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED)).thenReturn(new HashSet<>(Set.of(ra1, ra2)));

        SearchTreeInput searchTreeInput = SearchTreeRaoProvider.buildSearchTreeInput(crac,
                network,
                state1,
                Set.of(state1, state2),
                initialOutput,
                prePerimeterResult,
                treeParameters,
                raoParameters,
                linearOptimizerParameters,
                toolProvider,
                false,
                null);

        assertSame(network, searchTreeInput.getNetwork());
        assertEquals(Set.of(cnec1, cnec2), searchTreeInput.getFlowCnecs());
        assertEquals(Set.of(na1), searchTreeInput.getNetworkActions());
        assertEquals(Set.of(ra1), searchTreeInput.getRangeActions()); // ra2 is not valid
        assertNotNull(searchTreeInput.getObjectiveFunction());
        assertEquals(Set.of("sensitivity-fallback-cost", "mnec-cost", "loop-flow-cost"), searchTreeInput.getObjectiveFunction().getVirtualCostNames());
        assertNotNull(searchTreeInput.getIteratingLinearOptimizer());
        assertNotNull(searchTreeInput.getSearchTreeProblem());
        assertEquals(Set.of(cnec1, cnec2), searchTreeInput.getSearchTreeProblem().flowCnecs);
        assertEquals(Set.of(cnec3, cnec4), searchTreeInput.getSearchTreeProblem().loopFlowCnecs);
        assertSame(initialOutput, searchTreeInput.getSearchTreeProblem().initialFlowResult);
        assertSame(prePerimeterResult, searchTreeInput.getSearchTreeProblem().prePerimeterFlowResult);
        assertSame(prePerimeterResult, searchTreeInput.getSearchTreeProblem().prePerimeterSetPoints);
        assertSame(linearOptimizerParameters, searchTreeInput.getSearchTreeProblem().linearOptimizerParameters);
        assertNotNull(searchTreeInput.getSearchTreeBloomer());
        assertNotNull(searchTreeInput.getSearchTreeComputer());
    }

    @Test
    public void testCreatePreventiveLinearOptimizerParameters() {
        RaoParameters raoParameters = new RaoParameters();

        // absolute ampere, no mnec, no lf
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        raoParameters.setRaoWithLoopFlowLimitation(false);
        raoParameters.setMnecViolationCost(0);
        raoParameters.setPstSensitivityThreshold(0.45);

        LinearOptimizerParameters linearOptimizerParameters = SearchTreeRaoProvider.createPreventiveLinearOptimizerParameters(raoParameters);
        assertNotNull(linearOptimizerParameters);
        assertEquals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, linearOptimizerParameters.getObjectiveFunction());
        assertEquals(Unit.AMPERE, linearOptimizerParameters.getUnit());
        assertNotNull(linearOptimizerParameters.getMaxMinMarginParameters());
        assertNull(linearOptimizerParameters.getMaxMinRelativeMarginParameters());
        assertFalse(linearOptimizerParameters.hasRelativeMargins());
        assertEquals(0.45, linearOptimizerParameters.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertFalse(linearOptimizerParameters.isRaoWithLoopFlowLimitation());
        assertNull(linearOptimizerParameters.getLoopFlowParameters());
        assertFalse(linearOptimizerParameters.isRaoWithMnecLimitation());
        assertNull(linearOptimizerParameters.getMnecParameters());
        assertFalse(linearOptimizerParameters.hasOperatorsNotToOptimize());
        assertNull(linearOptimizerParameters.getUnoptimizedCnecParameters());

        // relative mw, with mnec and lf
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setRaoWithMnecLimitation(true);
        raoParameters.setMnecViolationCost(-10);
        raoParameters.setPstSensitivityThreshold(0.67);

        linearOptimizerParameters = SearchTreeRaoProvider.createPreventiveLinearOptimizerParameters(raoParameters);
        assertNotNull(linearOptimizerParameters);
        assertEquals(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, linearOptimizerParameters.getObjectiveFunction());
        assertEquals(Unit.MEGAWATT, linearOptimizerParameters.getUnit());
        assertNull(linearOptimizerParameters.getMaxMinMarginParameters());
        assertNotNull(linearOptimizerParameters.getMaxMinRelativeMarginParameters());
        assertTrue(linearOptimizerParameters.hasRelativeMargins());
        assertEquals(0.67, linearOptimizerParameters.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertTrue(linearOptimizerParameters.isRaoWithLoopFlowLimitation());
        assertNotNull(linearOptimizerParameters.getLoopFlowParameters());
        assertEquals(raoParameters.getLoopFlowApproximationLevel(), linearOptimizerParameters.getLoopFlowParameters().getLoopFlowApproximationLevel());
        assertEquals(raoParameters.getLoopFlowConstraintAdjustmentCoefficient(), linearOptimizerParameters.getLoopFlowParameters().getLoopFlowConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getLoopFlowViolationCost(), linearOptimizerParameters.getLoopFlowParameters().getLoopFlowViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getLoopFlowAcceptableAugmentation(), linearOptimizerParameters.getLoopFlowParameters().getLoopFlowAcceptableAugmentation(), DOUBLE_TOLERANCE);
        assertTrue(linearOptimizerParameters.isRaoWithMnecLimitation());
        assertNotNull(linearOptimizerParameters.getMnecParameters());
        assertEquals(raoParameters.getMnecViolationCost(), linearOptimizerParameters.getMnecParameters().getMnecViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getMnecAcceptableMarginDiminution(), linearOptimizerParameters.getMnecParameters().getMnecAcceptableMarginDiminution(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getMnecConstraintAdjustmentCoefficient(), linearOptimizerParameters.getMnecParameters().getMnecConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertFalse(linearOptimizerParameters.hasOperatorsNotToOptimize());
        assertNull(linearOptimizerParameters.getUnoptimizedCnecParameters());
    }

    @Test
    public void testCreateCurativeLinearOptimizerParameters() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        StateTree stateTree = Mockito.mock(StateTree.class);
        Mockito.when(stateTree.getOperatorsNotSharingCras()).thenReturn(Set.of("DE", "NL"));

        // absolute ampere, with mnec, no lf
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        raoParameters.setRaoWithLoopFlowLimitation(false);
        raoParameters.setRaoWithMnecLimitation(true);
        raoParameters.setMnecViolationCost(10);
        raoParameters.setPstSensitivityThreshold(0.45);
        raoParameters.getExtension(SearchTreeRaoParameters.class).setCurativeRaoOptimizeOperatorsNotSharingCras(true);

        LinearOptimizerParameters linearOptimizerParameters = SearchTreeRaoProvider.createCurativeLinearOptimizerParameters(raoParameters, stateTree, Set.of(cnec1, cnec2, cnec3, cnec4));
        assertNotNull(linearOptimizerParameters);
        assertEquals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, linearOptimizerParameters.getObjectiveFunction());
        assertEquals(Unit.AMPERE, linearOptimizerParameters.getUnit());
        assertNotNull(linearOptimizerParameters.getMaxMinMarginParameters());
        assertNull(linearOptimizerParameters.getMaxMinRelativeMarginParameters());
        assertFalse(linearOptimizerParameters.hasRelativeMargins());
        assertEquals(0.45, linearOptimizerParameters.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertFalse(linearOptimizerParameters.isRaoWithLoopFlowLimitation());
        assertNull(linearOptimizerParameters.getLoopFlowParameters());
        assertTrue(linearOptimizerParameters.isRaoWithMnecLimitation());
        assertNotNull(linearOptimizerParameters.getMnecParameters());
        assertEquals(raoParameters.getMnecViolationCost(), linearOptimizerParameters.getMnecParameters().getMnecViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getMnecAcceptableMarginDiminution(), linearOptimizerParameters.getMnecParameters().getMnecAcceptableMarginDiminution(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getMnecConstraintAdjustmentCoefficient(), linearOptimizerParameters.getMnecParameters().getMnecConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertFalse(linearOptimizerParameters.hasOperatorsNotToOptimize());
        assertNull(linearOptimizerParameters.getUnoptimizedCnecParameters());

        // relative mw, with lf no mnec
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setRaoWithMnecLimitation(false);
        raoParameters.setMnecViolationCost(100);
        raoParameters.setPstSensitivityThreshold(0.67);
        raoParameters.getExtension(SearchTreeRaoParameters.class).setCurativeRaoOptimizeOperatorsNotSharingCras(false);

        linearOptimizerParameters = SearchTreeRaoProvider.createCurativeLinearOptimizerParameters(raoParameters, stateTree, Set.of(cnec1, cnec3, cnec4));
        assertNotNull(linearOptimizerParameters);
        assertEquals(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, linearOptimizerParameters.getObjectiveFunction());
        assertEquals(Unit.MEGAWATT, linearOptimizerParameters.getUnit());
        assertNull(linearOptimizerParameters.getMaxMinMarginParameters());
        assertNotNull(linearOptimizerParameters.getMaxMinRelativeMarginParameters());
        assertTrue(linearOptimizerParameters.hasRelativeMargins());
        assertEquals(0.67, linearOptimizerParameters.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertTrue(linearOptimizerParameters.isRaoWithLoopFlowLimitation());
        assertNotNull(linearOptimizerParameters.getLoopFlowParameters());
        assertEquals(raoParameters.getLoopFlowApproximationLevel(), linearOptimizerParameters.getLoopFlowParameters().getLoopFlowApproximationLevel());
        assertEquals(raoParameters.getLoopFlowConstraintAdjustmentCoefficient(), linearOptimizerParameters.getLoopFlowParameters().getLoopFlowConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getLoopFlowViolationCost(), linearOptimizerParameters.getLoopFlowParameters().getLoopFlowViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getLoopFlowAcceptableAugmentation(), linearOptimizerParameters.getLoopFlowParameters().getLoopFlowAcceptableAugmentation(), DOUBLE_TOLERANCE);
        assertTrue(linearOptimizerParameters.hasOperatorsNotToOptimize());
        assertFalse(linearOptimizerParameters.isRaoWithMnecLimitation());
        assertNotNull(linearOptimizerParameters.getUnoptimizedCnecParameters());
        assertEquals(Set.of("DE", "NL"), linearOptimizerParameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize());
        assertEquals(1000., linearOptimizerParameters.getUnoptimizedCnecParameters().getHighestThresholdValue(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFilterOutGroup() {
        RangeAction ra1 = Mockito.mock(RangeAction.class);
        Mockito.doReturn(Optional.empty()).when(ra1).getGroupId();
        RangeAction ra2 = Mockito.mock(RangeAction.class);
        Mockito.doReturn(Optional.of("group1")).when(ra2).getGroupId();
        RangeAction ra3 = Mockito.mock(RangeAction.class);
        Mockito.doReturn(Optional.of("group2")).when(ra3).getGroupId();
        RangeAction ra4 = Mockito.mock(RangeAction.class);
        Mockito.doReturn(Optional.of("group2")).when(ra4).getGroupId();
        RangeAction ra5 = Mockito.mock(RangeAction.class);
        Mockito.doReturn(Optional.of("group2")).when(ra5).getGroupId();

        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Mockito.doReturn(1.).when(prePerimeterResult).getOptimizedSetPoint(ra1);
        Mockito.doReturn(10.).when(prePerimeterResult).getOptimizedSetPoint(ra2);
        Mockito.doReturn(2.).when(prePerimeterResult).getOptimizedSetPoint(ra3);
        Mockito.doReturn(2.).when(prePerimeterResult).getOptimizedSetPoint(ra4);
        Mockito.doReturn(6.).when(prePerimeterResult).getOptimizedSetPoint(ra5);

        Set<RangeAction> rangeActions = new HashSet<>(Set.of(ra1, ra2, ra3, ra4, ra5));
        SearchTreeRaoProvider.removeAlignedRangeActionsWithDifferentInitialSetpoints(rangeActions, prePerimeterResult);
        assertEquals(Set.of(ra1, ra2), rangeActions);
    }

    @Test
    public void testShouldRunSecondPreventiveRaoSimple() {
        RaoParameters parameters = new RaoParameters();
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Map<State, OptimizationResult> curativeResults = Map.of(state1, optimizationResult1, state2, optimizationResult2);

        // No SearchTreeRaoParameters extension
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));

        // Deactivated in parameters
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.DISABLED);
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));

        // CurativeRaoStopCriterion.MIN_OBJECTIVE
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));

        // CurativeRaoStopCriterion.SECURE, secure case
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.SECURE);
        Mockito.doReturn(-1.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(-10.).when(optimizationResult2).getFunctionalCost();
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
        // CurativeRaoStopCriterion.SECURE, unsecure case 1
        Mockito.doReturn(0.).when(optimizationResult1).getFunctionalCost();
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
        // CurativeRaoStopCriterion.SECURE, unsecure case 2
        Mockito.doReturn(5.).when(optimizationResult1).getFunctionalCost();
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
    }

    private void setCost(OptimizationResult optimizationResultMock, double cost) {
        when(optimizationResultMock.getFunctionalCost()).thenReturn(cost);
        when(optimizationResultMock.getCost()).thenReturn(cost);
    }

    @Test
    public void testShouldRunSecondPreventiveRaoAdvanced() {
        RaoParameters parameters = new RaoParameters();
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Map<State, OptimizationResult> curativeResults = Map.of(state1, optimizationResult1, state2, optimizationResult2);

        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        searchTreeRaoParameters.setCurativeRaoMinObjImprovement(10.);

        // CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE);
        setCost(preventiveResult, -100.);
        setCost(optimizationResult1, -200.);
        setCost(optimizationResult2, -300.);
        // case 1 : all curatives are better than preventive (cost < preventive cost - minObjImprovement)
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
        // case 2 : one curative has cost = preventive cost - minObjImprovement
        setCost(optimizationResult1, -110.);
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
        // case 3 : one curative has cost > preventive cost - minObjImprovement
        setCost(optimizationResult1, -109.);
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));

        // CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        // case 1 : all curatives are better than preventive (cost <= preventive cost - minObjImprovement), SECURE
        setCost(optimizationResult1, -200.);
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
        setCost(optimizationResult1, -110.);
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
        // case 2 : all curatives are better than preventive (cost < preventive cost - minObjImprovement), UNSECURE
        setCost(preventiveResult, 1000.);
        setCost(optimizationResult1, 0.);
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
        setCost(optimizationResult1, 10.);
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
        // case 3 : one curative has cost > preventive cost - minObjImprovement, SECURE
        setCost(preventiveResult, -100.);
        setCost(optimizationResult1, -109.);
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, null, 0));
    }

    @Test
    public void testShouldRunSecondPreventiveRaoTime() {
        RaoParameters parameters = new RaoParameters();
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Map<State, OptimizationResult> curativeResults = Map.of(state1, optimizationResult1, state2, optimizationResult2);

        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);

        // Enough time
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, java.time.Instant.now().plusSeconds(200), 100));
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, java.time.Instant.now().plusSeconds(200), 199));

        // Not enough time
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, java.time.Instant.now().plusSeconds(200), 201));
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, null, preventiveResult, curativeResults, java.time.Instant.now().plusSeconds(200), 400));
    }

    @Test
    public void testShouldRunSecondPreventiveRaoCostIncrease() {
        RaoParameters parameters = new RaoParameters();
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        ObjectiveFunctionResult initialResult = Mockito.mock(ObjectiveFunctionResult.class);
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Map<State, OptimizationResult> curativeResults = Map.of(state1, optimizationResult1, state2, optimizationResult2);

        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.COST_INCREASE);
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);

        when(initialResult.getCost()).thenReturn(-100.);
        when(preventiveResult.getCost()).thenReturn(-10.);
        when(optimizationResult1.getCost()).thenReturn(-120.);
        when(optimizationResult2.getCost()).thenReturn(-130.);

        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, initialResult, preventiveResult, curativeResults, null, 0));

        when(optimizationResult2.getCost()).thenReturn(-100.);
        assertFalse(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, initialResult, preventiveResult, curativeResults, null, 0));

        when(optimizationResult2.getCost()).thenReturn(-95.);
        assertTrue(SearchTreeRaoProvider.shouldRunSecondPreventiveRao(parameters, initialResult, preventiveResult, curativeResults, null, 0));
    }

    private void setUpCracWithRAs() {
        crac = CracFactory.findDefault().create("test-crac");
        Contingency contingency1 = crac.newContingency()
                .withId("contingency1")
                .withNetworkElement("contingency1-ne")
                .add();
        Contingency contingency2 = crac.newContingency()
                .withId("contingency2")
                .withNetworkElement("contingency2-ne")
                .add();
        crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("cnec-ne")
            .withContingency("contingency1")
            .withInstant(Instant.CURATIVE)
            .withNominalVoltage(220.)
            .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        // ra1 : preventive only
        ra1 = crac.newPstRangeAction()
                .withId("ra1")
                .withNetworkElement("ra1-ne")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnStateUsageRule().withContingency("contingency1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.UNDEFINED).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra2 : curative only
        ra2 = crac.newPstRangeAction()
                .withId("ra2")
                .withNetworkElement("ra2-ne")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.UNAVAILABLE).add()
                .newOnStateUsageRule().withContingency("contingency2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra3 : preventive and curative
        ra3 = crac.newPstRangeAction()
                .withId("ra3")
                .withNetworkElement("ra3-ne")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnStateUsageRule().withContingency("contingency1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra4 : preventive only, but with same NetworkElement as ra5
        ra4 = crac.newPstRangeAction()
                .withId("ra4")
                .withNetworkElement("ra4-ne1")
                .withNetworkElement("ra4-ne2")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra5 : curative only, but with same NetworkElement as ra4
        ra5 = crac.newPstRangeAction()
                .withId("ra5")
                .withNetworkElement("ra4-ne1")
                .withNetworkElement("ra4-ne2")
                .newOnStateUsageRule().withContingency("contingency2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra6 : preventive and curative (onFlowConsrtaint)
        ra6 = crac.newPstRangeAction()
            .withId("ra6")
            .withNetworkElement("ra6-ne")
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnFlowConstraintUsageRule().withFlowCnec("cnec").withInstant(Instant.CURATIVE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
                .withId("na1")
                .newTopologicalAction().withNetworkElement("na1-ne").withActionType(ActionType.OPEN).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnStateUsageRule().withContingency("contingency1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        state1 = crac.getState(contingency1, Instant.CURATIVE);
        state2 = crac.getState(contingency2, Instant.CURATIVE);
    }

    @Test
    public void testIsRangeActionAvailableInState() {
        setUpCracWithRAs();

        // ra1 is available in preventive only
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra1, crac.getPreventiveState(), crac));
        assertFalse(SearchTreeRaoProvider.isRangeActionAvailableInState(ra1, state1, crac));
        assertFalse(SearchTreeRaoProvider.isRangeActionAvailableInState(ra1, state2, crac));

        // ra2 is available in state2 only
        assertFalse(SearchTreeRaoProvider.isRangeActionAvailableInState(ra2, crac.getPreventiveState(), crac));
        assertFalse(SearchTreeRaoProvider.isRangeActionAvailableInState(ra2, state1, crac));
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra2, state2, crac));

        // ra3 is available in preventive and in state1
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra3, crac.getPreventiveState(), crac));
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra3, state1, crac));
        assertFalse(SearchTreeRaoProvider.isRangeActionAvailableInState(ra3, state2, crac));

        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra4, crac.getPreventiveState(), crac));
        assertFalse(SearchTreeRaoProvider.isRangeActionAvailableInState(ra4, state1, crac));
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra4, state2, crac));

        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra5, crac.getPreventiveState(), crac));
        assertFalse(SearchTreeRaoProvider.isRangeActionAvailableInState(ra5, state1, crac));
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra5, state2, crac));

        // ra6 is available in preventive and in state1 and in state2
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra6, crac.getPreventiveState(), crac));
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra6, state1, crac));
        assertTrue(SearchTreeRaoProvider.isRangeActionAvailableInState(ra6, state2, crac));
    }

    @Test
    public void testIsRangeActionPreventive() {
        setUpCracWithRAs();
        // ra1 is available in preventive only
        assertTrue(SearchTreeRaoProvider.isRangeActionPreventive(ra1, crac));
        // ra2 is available in state2 only
        assertFalse(SearchTreeRaoProvider.isRangeActionPreventive(ra2, crac));
        // ra3 is available in preventive and in state1
        assertTrue(SearchTreeRaoProvider.isRangeActionPreventive(ra3, crac));
        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertTrue(SearchTreeRaoProvider.isRangeActionPreventive(ra4, crac));
        assertTrue(SearchTreeRaoProvider.isRangeActionPreventive(ra5, crac));
        // ra6 is preventive and curative
        assertTrue(SearchTreeRaoProvider.isRangeActionPreventive(ra6, crac));
    }

    @Test
    public void testIsRangeActionCurative() {
        setUpCracWithRAs();
        // ra1 is available in preventive only
        assertFalse(SearchTreeRaoProvider.isRangeActionCurative(ra1, crac));
        // ra2 is available in state2 only
        assertTrue(SearchTreeRaoProvider.isRangeActionCurative(ra2, crac));
        // ra3 is available in preventive and in state1
        assertTrue(SearchTreeRaoProvider.isRangeActionCurative(ra3, crac));
        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertTrue(SearchTreeRaoProvider.isRangeActionCurative(ra4, crac));
        assertTrue(SearchTreeRaoProvider.isRangeActionCurative(ra5, crac));
        // ra6 is preventive and curative
        assertTrue(SearchTreeRaoProvider.isRangeActionCurative(ra6, crac));
    }

    @Test
    public void testGetRangeActionsExcludedFromSecondPreventive() {
        setUpCracWithRAs();
        // detect range actions that are preventive and curative
        assertEquals(Set.of(ra3, ra4, ra5, ra6), SearchTreeRaoProvider.getRangeActionsExcludedFromSecondPreventive(crac));
    }

    @Test
    public void testRemoveRangeActionsExcludedFromSecondPreventive() {
        setUpCracWithRAs();
        Set<RangeAction> rangeActions = new HashSet<>(Set.of(ra1, ra2, ra3, ra4, ra5));
        SearchTreeRaoProvider.removeRangeActionsExcludedFromSecondPreventive(rangeActions, crac);
        assertEquals(Set.of(ra1, ra2), rangeActions);
    }

    @Test
    public void testBuildSearchTreeInputForSecondPreventive() {
        setUpCracWithRAs();
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        raoParameters.setMnecViolationCost(10);
        raoParameters.setRaoWithLoopFlowLimitation(true);

        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        when(treeParameters.getMaxRaPerTso()).thenReturn(Map.of("fr", 5));
        when(treeParameters.getMaxPstPerTso()).thenReturn(Map.of("be", 1, "nl", 2));
        when(treeParameters.getMaxTopoPerTso()).thenReturn(new HashMap<>());
        when(treeParameters.getSkipNetworkActionsFarFromMostLimitingElement()).thenReturn(true);

        ToolProvider toolProvider = Mockito.mock(ToolProvider.class);
        when(toolProvider.getLoopFlowCnecs(any())).thenReturn(Set.of(cnec3, cnec4));

        LinearOptimizerParameters linearOptimizerParameters = Mockito.mock(LinearOptimizerParameters.class);

        PrePerimeterResult initialOutput = Mockito.mock(PrePerimeterResult.class);

        State preventiveState = crac.getPreventiveState();
        Set<State> optimizedStates = Set.of(preventiveState, state2);
        assertThrows(NullPointerException.class, () -> SearchTreeRaoProvider.buildSearchTreeInput(crac,
                network,
                preventiveState,
                optimizedStates,
                initialOutput,
                prePerimeterResult,
                treeParameters,
                raoParameters,
                linearOptimizerParameters,
                toolProvider,
                true,
                null)
        );

        SearchTreeInput searchTreeInput = SearchTreeRaoProvider.buildSearchTreeInput(crac,
                network,
                crac.getPreventiveState(),
                Set.of(crac.getPreventiveState(), state2),
                initialOutput,
                prePerimeterResult,
                treeParameters,
                raoParameters,
                linearOptimizerParameters,
                toolProvider,
                true,
                new AppliedRemedialActions());

        assertEquals(Set.of(ra1), searchTreeInput.getRangeActions()); // only ra1 is purely preventive
        assertEquals(Set.of(na1), searchTreeInput.getNetworkActions()); // na1 is preventive + curative but shouldn't be removed
        assertNotNull(searchTreeInput.getSearchTreeComputer());
    }

    private void setUpCracWithRealRAs(boolean curative) {
        network = NetworkImportsUtil.import12NodesNetwork();
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger();
        HashMap<Integer, Double> tapToAngleConversionMap = new HashMap<>();
        phaseTapChanger.getAllSteps().forEach((stepInt, step) -> tapToAngleConversionMap.put(stepInt, step.getAlpha()));
        crac = CracFactory.findDefault().create("test-crac");
        Contingency contingency1 = crac.newContingency()
                .withId("contingency1")
                .withNetworkElement("contingency1-ne")
                .add();
        Contingency contingency2 = crac.newContingency()
                .withId("contingency2")
                .withNetworkElement("contingency2-ne")
                .add();
        // ra1 : preventive only
        PstRangeActionAdder adder = crac.newPstRangeAction()
                .withId("ra1")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(0).withTapToAngleConversionMap(tapToAngleConversionMap)
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add();
        if (curative) {
            adder.newOnStateUsageRule().withContingency("contingency1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add();
        }
        ra1 = adder.add();
        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
                .withId("na1")
                .newTopologicalAction().withNetworkElement("BBE1AA1  BBE2AA1  1").withActionType(ActionType.OPEN).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnStateUsageRule().withContingency("contingency2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        state1 = crac.getState(contingency1, Instant.CURATIVE);
        state2 = crac.getState(contingency2, Instant.CURATIVE);
    }

    @Test
    public void testApplyPreventiveResultsForCurativeRangeActions() {
        PerimeterResult perimeterResult = Mockito.mock(PerimeterResult.class);
        String pstNeId = "BBE2AA1  BBE3AA1  1";

        setUpCracWithRealRAs(false);
        Mockito.doReturn(-1.5583491325378418).when(perimeterResult).getOptimizedSetPoint(ra1);
        Mockito.doReturn(Set.of(ra1)).when(perimeterResult).getActivatedRangeActions();
        SearchTreeRaoProvider.applyPreventiveResultsForCurativeRangeActions(network, perimeterResult, crac);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());

        setUpCracWithRealRAs(true);
        Mockito.doReturn(-1.5583491325378418).when(perimeterResult).getOptimizedSetPoint(ra1);
        Mockito.doReturn(Set.of(ra1)).when(perimeterResult).getActivatedRangeActions();
        SearchTreeRaoProvider.applyPreventiveResultsForCurativeRangeActions(network, perimeterResult, crac);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void testGetAppliedRemedialActionsInCurative() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Mockito.doReturn(0.).when(prePerimeterResult).getOptimizedSetPoint(ra1);

        String pstNeId = "BBE2AA1  BBE3AA1  1";
        String naNeId = "BBE1AA1  BBE2AA1  1";

        setUpCracWithRealRAs(true);

        OptimizationResult optimResult1 = Mockito.mock(OptimizationResult.class);
        Mockito.doReturn(Set.of(ra1)).when(optimResult1).getRangeActions();
        Mockito.doReturn(-1.5583491325378418).when(optimResult1).getOptimizedSetPoint(ra1);
        Mockito.doReturn(Set.of()).when(optimResult1).getActivatedNetworkActions();

        OptimizationResult optimResult2 = Mockito.mock(OptimizationResult.class);
        Mockito.doReturn(Set.of(ra1)).when(optimResult2).getRangeActions();
        Mockito.doReturn(0.).when(optimResult2).getOptimizedSetPoint(ra1);
        Mockito.doReturn(Set.of(na1)).when(optimResult2).getActivatedNetworkActions();

        Map<State, OptimizationResult> curativeResults = Map.of(state1, optimResult1, state2, optimResult2);

        AppliedRemedialActions appliedRemedialActions = SearchTreeRaoProvider.getAppliedRemedialActionsInCurative(curativeResults, prePerimeterResult);

        // apply only range action
        appliedRemedialActions.applyOnNetwork(state1, network);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertTrue(network.getLine(naNeId).getTerminal1().isConnected());

        // reset network
        network = NetworkImportsUtil.import12NodesNetwork();

        // apply only network action
        appliedRemedialActions.applyOnNetwork(state2, network);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine(naNeId).getTerminal1().isConnected());

        // apply also range action
        appliedRemedialActions.applyOnNetwork(state1, network);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine(naNeId).getTerminal1().isConnected());
    }
}
