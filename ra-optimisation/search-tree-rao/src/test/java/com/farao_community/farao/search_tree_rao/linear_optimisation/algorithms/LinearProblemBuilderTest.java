/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.CurativeOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.*;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers.*;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPSolver;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.linear_optimisation.inputs.IteratingLinearOptimizerInput;
import com.farao_community.farao.search_tree_rao.linear_optimisation.parameters.IteratingLinearOptimizerParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class LinearProblemBuilderTest {
    private LinearProblemBuilder linearProblemBuilder;
    private IteratingLinearOptimizerInput inputs;
    private IteratingLinearOptimizerParameters parameters;
    private SolverParameters solverParameters;
    private RangeActionParameters rangeActionParameters;
    private OptimizationPerimeter optimizationPerimeter;

    @BeforeEach
    public void setup() {
        linearProblemBuilder = new LinearProblemBuilder();
        inputs = Mockito.mock(IteratingLinearOptimizerInput.class);
        parameters = Mockito.mock(IteratingLinearOptimizerParameters.class);
        linearProblemBuilder = Mockito.spy(linearProblemBuilder);
        doReturn(new FaraoMPSolver()).when(linearProblemBuilder).buildSolver();

        solverParameters = Mockito.mock(SolverParameters.class);
        when(parameters.getSolverParameters()).thenReturn(solverParameters);
        rangeActionParameters = Mockito.mock(RangeActionParameters.class);
        when(parameters.getRangeActionParameters()).thenReturn(rangeActionParameters);
        MaxMinRelativeMarginParameters relativeMarginParameters = Mockito.mock(MaxMinRelativeMarginParameters.class);
        when(parameters.getMaxMinRelativeMarginParameters()).thenReturn(relativeMarginParameters);
        MnecParameters mnecParameters = Mockito.mock(MnecParameters.class);
        when(parameters.getMnecParameters()).thenReturn(mnecParameters);
        LoopFlowParameters loopFlowParameters = Mockito.mock(LoopFlowParameters.class);
        when(parameters.getLoopFlowParameters()).thenReturn(loopFlowParameters);

        optimizationPerimeter = Mockito.mock(CurativeOptimizationPerimeter.class);
        when(inputs.getOptimizationPerimeter()).thenReturn(optimizationPerimeter);

    }

    @Test
    void testBuildMaxMarginContinuous() {
        when(rangeActionParameters.getPstOptimizationApproximation()).thenReturn(RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(3, fillers.size());
        assertTrue(fillers.get(0) instanceof CoreProblemFiller);
        assertTrue(fillers.get(1) instanceof MaxMinMarginFiller);
        assertTrue(fillers.get(2) instanceof ContinuousRangeActionGroupFiller);
    }

    @Test
    void testBuildMaxMarginDiscrete() {
        when(rangeActionParameters.getPstOptimizationApproximation()).thenReturn(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(5, fillers.size());
        assertTrue(fillers.get(0) instanceof CoreProblemFiller);
        assertTrue(fillers.get(1) instanceof MaxMinMarginFiller);
        assertTrue(fillers.get(2) instanceof DiscretePstTapFiller);
        assertTrue(fillers.get(3) instanceof DiscretePstGroupFiller);
        assertTrue(fillers.get(4) instanceof ContinuousRangeActionGroupFiller);
    }

    @Test
    void testBuildMaxRelativeMarginContinuous() {
        when(rangeActionParameters.getPstOptimizationApproximation()).thenReturn(RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(3, fillers.size());
        assertTrue(fillers.get(0) instanceof CoreProblemFiller);
        assertTrue(fillers.get(1) instanceof MaxMinRelativeMarginFiller);
        assertTrue(fillers.get(2) instanceof ContinuousRangeActionGroupFiller);
    }

    @Test
    void testBuildMaxMarginContinuousMnecLoopflowUnoptimized() {
        when(rangeActionParameters.getPstOptimizationApproximation()).thenReturn(RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT);
        when(parameters.isRaoWithMnecLimitation()).thenReturn(true);
        when(parameters.isRaoWithLoopFlowLimitation()).thenReturn(true);
        when(parameters.getUnoptimizedCnecParameters()).thenReturn(Mockito.mock(UnoptimizedCnecParameters.class));

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(6, fillers.size());
        assertTrue(fillers.get(0) instanceof CoreProblemFiller);
        assertTrue(fillers.get(1) instanceof MaxMinMarginFiller);
        assertTrue(fillers.get(2) instanceof MnecFiller);
        assertTrue(fillers.get(3) instanceof MaxLoopFlowFiller);
        assertTrue(fillers.get(4) instanceof UnoptimizedCnecFiller);
        assertTrue(fillers.get(5) instanceof ContinuousRangeActionGroupFiller);
    }

    @Test
    void testBuildMaxMarginContinuousRaLimitation() {
        when(rangeActionParameters.getPstOptimizationApproximation()).thenReturn(RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT);
        RangeActionLimitationParameters raLimitationParameters = Mockito.mock(RangeActionLimitationParameters.class);
        when(parameters.getRaLimitationParameters()).thenReturn(raLimitationParameters);
        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(Mockito.mock(State.class)));
        when(raLimitationParameters.areRangeActionLimitedForState(Mockito.any())).thenReturn(true);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(4, fillers.size());
        assertTrue(fillers.get(0) instanceof CoreProblemFiller);
        assertTrue(fillers.get(1) instanceof MaxMinMarginFiller);
        assertTrue(fillers.get(2) instanceof ContinuousRangeActionGroupFiller);
        assertTrue(fillers.get(3) instanceof RaUsageLimitsFiller);
    }
}
