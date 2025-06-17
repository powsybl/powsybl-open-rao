/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.*;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class LinearProblemBuilderTest {
    private LinearProblemBuilder linearProblemBuilder;
    private IteratingLinearOptimizerInput inputs;
    private IteratingLinearOptimizerParameters parameters;
    private SearchTreeRaoRangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters;
    private com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters rangeActionParameters;
    private SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;
    private OptimizationPerimeter optimizationPerimeter;

    @BeforeEach
    public void setup() {
        linearProblemBuilder = new LinearProblemBuilder();
        inputs = Mockito.mock(IteratingLinearOptimizerInput.class);
        parameters = Mockito.mock(IteratingLinearOptimizerParameters.class);

        solverParameters = Mockito.mock(SearchTreeRaoRangeActionsOptimizationParameters.LinearOptimizationSolver.class);
        when(solverParameters.getSolver()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP);
        when(parameters.getSolverParameters()).thenReturn(solverParameters);
        rangeActionParameters = Mockito.mock(com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters.class);
        when(parameters.getRangeActionParameters()).thenReturn(rangeActionParameters);
        rangeActionParametersExtension = Mockito.mock(SearchTreeRaoRangeActionsOptimizationParameters.class);
        when(parameters.getRangeActionParametersExtension()).thenReturn(rangeActionParametersExtension);
        SearchTreeRaoRelativeMarginsParameters relativeMarginParameters = Mockito.mock(SearchTreeRaoRelativeMarginsParameters.class);
        when(parameters.getMaxMinRelativeMarginParameters()).thenReturn(relativeMarginParameters);
        MnecParameters mnecParameters = Mockito.mock(MnecParameters.class);
        when(parameters.getMnecParameters()).thenReturn(mnecParameters);
        SearchTreeRaoMnecParameters mnecParametersExtension = Mockito.mock(SearchTreeRaoMnecParameters.class);
        when(parameters.getMnecParametersExtension()).thenReturn(mnecParametersExtension);
        LoopFlowParameters loopFlowParameters = Mockito.mock(LoopFlowParameters.class);
        when(parameters.getLoopFlowParameters()).thenReturn(loopFlowParameters);
        SearchTreeRaoLoopFlowParameters loopFlowParametersExtension = Mockito.mock(SearchTreeRaoLoopFlowParameters.class);
        when(parameters.getLoopFlowParametersExtension()).thenReturn(loopFlowParametersExtension);

        optimizationPerimeter = Mockito.mock(CurativeOptimizationPerimeter.class);
        when(inputs.optimizationPerimeter()).thenReturn(optimizationPerimeter);

    }

    @Test
    void testBuildMaxMarginContinuous() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(parameters.getObjectiveFunctionUnit()).thenReturn(Unit.MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(3, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.get(0));
        assertInstanceOf(MaxMinMarginFiller.class, fillers.get(1));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(2));
    }

    @Test
    void testBuildMaxMarginDiscrete() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(parameters.getObjectiveFunctionUnit()).thenReturn(Unit.MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(5, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.get(0));
        assertInstanceOf(MaxMinMarginFiller.class, fillers.get(1));
        assertInstanceOf(DiscretePstTapFiller.class, fillers.get(2));
        assertInstanceOf(DiscretePstGroupFiller.class, fillers.get(3));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(4));
    }

    @Test
    void testBuildMaxRelativeMarginContinuous() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN);
        when(parameters.getObjectiveFunctionUnit()).thenReturn(Unit.MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(3, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.get(0));
        assertInstanceOf(MaxMinRelativeMarginFiller.class, fillers.get(1));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(2));
    }

    @Test
    void testBuildMaxMarginContinuousMnecLoopflowUnoptimized() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(parameters.getObjectiveFunctionUnit()).thenReturn(Unit.MEGAWATT);
        when(parameters.getObjectiveFunctionUnit()).thenReturn(Unit.MEGAWATT);

        when(parameters.isRaoWithMnecLimitation()).thenReturn(true);
        when(parameters.isRaoWithLoopFlowLimitation()).thenReturn(true);
        when(parameters.getUnoptimizedCnecParameters()).thenReturn(Mockito.mock(UnoptimizedCnecParameters.class));

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(6, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.get(0));
        assertInstanceOf(MaxMinMarginFiller.class, fillers.get(1));
        assertInstanceOf(MnecFiller.class, fillers.get(2));
        assertInstanceOf(MaxLoopFlowFiller.class, fillers.get(3));
        assertInstanceOf(UnoptimizedCnecFiller.class, fillers.get(4));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(5));
    }

    @Test
    void testBuildMaxMarginContinuousRaLimitation() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(parameters.getObjectiveFunctionUnit()).thenReturn(Unit.MEGAWATT);
        RangeActionLimitationParameters raLimitationParameters = Mockito.mock(RangeActionLimitationParameters.class);
        when(parameters.getRaLimitationParameters()).thenReturn(raLimitationParameters);
        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(Mockito.mock(State.class)));
        when(raLimitationParameters.areRangeActionLimitedForState(Mockito.any())).thenReturn(true);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(4, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.get(0));
        assertInstanceOf(MaxMinMarginFiller.class, fillers.get(1));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(2));
        assertInstanceOf(RaUsageLimitsFiller.class, fillers.get(3));
    }
}
