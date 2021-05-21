/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import com.farao_community.farao.rao_commons.ToolProvider;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SearchTreeRaoProviderTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Crac crac;
    private Network network;
    private BranchResult initialBranchResult;
    private BranchResult preperimBranchResult;
    private BranchCnec cnec1;
    private BranchCnec cnec2;
    private BranchCnec cnec3;
    private BranchCnec cnec4;
    private State state1;
    private State state2;
    private State state3;
    private RangeAction ra1;
    private RangeAction ra2;
    private PrePerimeterResult prePerimeterResult;

    @Before
    public void setUp() {
        cnec1 = Mockito.mock(BranchCnec.class);
        cnec2 = Mockito.mock(BranchCnec.class);
        cnec3 = Mockito.mock(BranchCnec.class);
        cnec4 = Mockito.mock(BranchCnec.class);
        crac = Mockito.mock(Crac.class);
        network = Mockito.mock(Network.class);
        initialBranchResult = Mockito.mock(BranchResult.class);
        preperimBranchResult = Mockito.mock(BranchResult.class);
        state1 = Mockito.mock(State.class);
        state2 = Mockito.mock(State.class);
        state3 = Mockito.mock(State.class);

        when(crac.getBranchCnecs()).thenReturn(Set.of(cnec1, cnec2, cnec3, cnec4));
        when(crac.getBranchCnecs(state1)).thenReturn(Set.of(cnec1));
        when(crac.getBranchCnecs(state2)).thenReturn(Set.of(cnec2));
        when(crac.getBranchCnecs(state3)).thenReturn(Set.of(cnec3, cnec4));

        ra1 = Mockito.mock(RangeAction.class);
        when(ra1.getMinValue(eq(network), anyDouble())).thenReturn(-5.);
        when(ra1.getMaxValue(eq(network), anyDouble())).thenReturn(5.);
        ra2 = Mockito.mock(RangeAction.class);
        when(ra2.getMinValue(eq(network), anyDouble())).thenReturn(-3.);
        when(ra2.getMaxValue(eq(network), anyDouble())).thenReturn(3.);
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
        SearchTreeRaoProvider.removeRangeActionsWithWrongInitialSetpoint(rangeActions, prePerimeterResult, network);
        assertEquals(Set.of(ra1), rangeActions);
        when(prePerimeterResult.getOptimizedSetPoint(any())).thenReturn(-3.);
        rangeActions = new HashSet<>(Set.of(ra1, ra2));
        SearchTreeRaoProvider.removeRangeActionsWithWrongInitialSetpoint(rangeActions, prePerimeterResult, network);
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
        objFun = SearchTreeRaoProvider.createObjectiveFunction(Set.of(cnec1), initialBranchResult, preperimBranchResult, raoParameters, Mockito.mock(LinearOptimizerParameters.class), toolProvider);
        assertNotNull(objFun);
        assertEquals(Set.of("sensitivity-fallback-cost"), objFun.getVirtualCostNames());

        // mnec virtual cost
        raoParameters.setMnecViolationCost(1);
        raoParameters.setRaoWithLoopFlowLimitation(false);
        objFun = SearchTreeRaoProvider.createObjectiveFunction(Set.of(cnec1), initialBranchResult, preperimBranchResult, raoParameters, Mockito.mock(LinearOptimizerParameters.class), toolProvider);
        assertNotNull(objFun);
        assertEquals(Set.of("sensitivity-fallback-cost", "mnec-cost"), objFun.getVirtualCostNames());

        // lf cost
        raoParameters.setMnecViolationCost(0);
        raoParameters.setRaoWithLoopFlowLimitation(true);
        objFun = SearchTreeRaoProvider.createObjectiveFunction(Set.of(cnec1), initialBranchResult, preperimBranchResult, raoParameters, Mockito.mock(LinearOptimizerParameters.class), toolProvider);
        assertNotNull(objFun);
        assertEquals(Set.of("sensitivity-fallback-cost", "loop-flow-cost"), objFun.getVirtualCostNames());

        // mnec and lf costs
        raoParameters.setMnecViolationCost(-1);
        raoParameters.setRaoWithLoopFlowLimitation(true);
        objFun = SearchTreeRaoProvider.createObjectiveFunction(Set.of(cnec1), initialBranchResult, preperimBranchResult, raoParameters, Mockito.mock(LinearOptimizerParameters.class), toolProvider);
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

        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        when(crac.getNetworkActions(network, state1, UsageMethod.AVAILABLE)).thenReturn(Set.of(na1));
        when(crac.getRangeActions(network, state1, UsageMethod.AVAILABLE)).thenReturn(new HashSet<>(Set.of(ra1, ra2)));

        SearchTreeInput searchTreeInput = SearchTreeRaoProvider.buildSearchTreeInput(crac,
                network,
                state1,
                Set.of(state1, state2),
                initialOutput,
                prePerimeterResult,
                treeParameters,
                raoParameters,
                linearOptimizerParameters,
                toolProvider);

        assertSame(network, searchTreeInput.getNetwork());
        assertEquals(Set.of(cnec1, cnec2), searchTreeInput.getCnecs());
        assertEquals(Set.of(na1), searchTreeInput.getNetworkActions());
        assertEquals(Set.of(ra1), searchTreeInput.getRangeActions()); // ra2 is not valid
        assertNotNull(searchTreeInput.getObjectiveFunction());
        assertEquals(Set.of("sensitivity-fallback-cost", "mnec-cost", "loop-flow-cost"), searchTreeInput.getObjectiveFunction().getVirtualCostNames());
        assertNotNull(searchTreeInput.getIteratingLinearOptimizer());
        assertNotNull(searchTreeInput.getSearchTreeProblem());
        assertEquals(Set.of(cnec1, cnec2), searchTreeInput.getSearchTreeProblem().cnecs);
        assertEquals(Set.of(cnec3, cnec4), searchTreeInput.getSearchTreeProblem().loopFlowCnecs);
        assertSame(initialOutput, searchTreeInput.getSearchTreeProblem().initialBranchResult);
        assertSame(prePerimeterResult, searchTreeInput.getSearchTreeProblem().prePerimeterBranchResult);
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
        assertFalse(linearOptimizerParameters.hasMonitoredElements());
        assertNull(linearOptimizerParameters.getMnecParameters());
        assertFalse(linearOptimizerParameters.hasOperatorsNotToOptimize());
        assertNull(linearOptimizerParameters.getUnoptimizedCnecParameters());

        // relative mw, with mnec and lf
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoParameters.setRaoWithLoopFlowLimitation(true);
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
        assertTrue(linearOptimizerParameters.hasMonitoredElements());
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
        assertTrue(linearOptimizerParameters.hasMonitoredElements());
        assertNotNull(linearOptimizerParameters.getMnecParameters());
        assertEquals(raoParameters.getMnecViolationCost(), linearOptimizerParameters.getMnecParameters().getMnecViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getMnecAcceptableMarginDiminution(), linearOptimizerParameters.getMnecParameters().getMnecAcceptableMarginDiminution(), DOUBLE_TOLERANCE);
        assertEquals(raoParameters.getMnecConstraintAdjustmentCoefficient(), linearOptimizerParameters.getMnecParameters().getMnecConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertFalse(linearOptimizerParameters.hasOperatorsNotToOptimize());
        assertNull(linearOptimizerParameters.getUnoptimizedCnecParameters());

        // relative mw, with lf no mnec
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setMnecViolationCost(0);
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
        assertNotNull(linearOptimizerParameters.getUnoptimizedCnecParameters());
        assertEquals(Set.of("DE", "NL"), linearOptimizerParameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize());
        assertEquals(1000., linearOptimizerParameters.getUnoptimizedCnecParameters().getHighestThresholdValue(), DOUBLE_TOLERANCE);
    }
}
