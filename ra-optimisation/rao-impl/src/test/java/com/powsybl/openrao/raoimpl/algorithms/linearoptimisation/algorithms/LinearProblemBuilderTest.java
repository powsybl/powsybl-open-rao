/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering.FlowCnecFilter;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering.MarginCriterion;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering.SafeFlowCnecFilter;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.AbstractCoreProblemFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.ContinuousRangeActionGroupFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.DiscretePstGroupFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.DiscretePstTapFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MarginCoreProblemFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MaxLoopFlowFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MaxMinMarginFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MaxMinRelativeMarginFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MnecFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RaUsageLimitsFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.UnoptimizedCnecFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.raoimpl.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class LinearProblemBuilderTest {
    private LinearProblemBuilder linearProblemBuilder;
    private IteratingLinearOptimizerInput inputs;
    private IteratingLinearOptimizerParameters parameters;
    private SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;
    private OptimizationPerimeter optimizationPerimeter;
    private SearchTreeRaoRelativeMarginsParameters relativeMarginParameters;
    private SearchTreeRaoCostlyMinMarginParameters minMarginsParameters;

    @BeforeEach
    public void setup() {
        linearProblemBuilder = new LinearProblemBuilder();
        inputs = Mockito.mock(IteratingLinearOptimizerInput.class);
        parameters = Mockito.mock(IteratingLinearOptimizerParameters.class);

        final SearchTreeRaoRangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters = Mockito.mock(SearchTreeRaoRangeActionsOptimizationParameters.LinearOptimizationSolver.class);
        when(solverParameters.getSolver()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP);
        when(parameters.getSolverParameters()).thenReturn(solverParameters);
        final RangeActionsOptimizationParameters rangeActionParameters = Mockito.mock(RangeActionsOptimizationParameters.class);
        when(parameters.getRangeActionParameters()).thenReturn(rangeActionParameters);
        rangeActionParametersExtension = Mockito.mock(SearchTreeRaoRangeActionsOptimizationParameters.class);
        when(parameters.getRangeActionParametersExtension()).thenReturn(rangeActionParametersExtension);
        relativeMarginParameters = Mockito.mock(SearchTreeRaoRelativeMarginsParameters.class);
        when(parameters.getMaxMinRelativeMarginParameters()).thenReturn(relativeMarginParameters);
        minMarginsParameters = Mockito.mock(SearchTreeRaoCostlyMinMarginParameters.class);
        when(parameters.getMinMarginParameters()).thenReturn(minMarginsParameters);
        MnecParameters mnecParameters = Mockito.mock(MnecParameters.class);
        when(parameters.getMnecParameters()).thenReturn(mnecParameters);
        SearchTreeRaoMnecParameters mnecParametersExtension = Mockito.mock(SearchTreeRaoMnecParameters.class);
        when(parameters.getMnecParametersExtension()).thenReturn(mnecParametersExtension);
        LoopFlowParameters loopFlowParameters = Mockito.mock(LoopFlowParameters.class);
        when(parameters.getLoopFlowParameters()).thenReturn(loopFlowParameters);
        SearchTreeRaoLoopFlowParameters loopFlowParametersExtension = Mockito.mock(SearchTreeRaoLoopFlowParameters.class);
        when(parameters.getLoopFlowParametersExtension()).thenReturn(loopFlowParametersExtension);

        State optimizationState = Mockito.mock(State.class);
        when(optimizationState.getTimestamp()).thenReturn(Optional.empty());
        optimizationPerimeter = Mockito.mock(CurativeOptimizationPerimeter.class);
        when(inputs.optimizationPerimeter()).thenReturn(optimizationPerimeter);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(optimizationState);
    }

    @Test
    void testBuildMaxMarginContinuous() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(parameters.getFlowUnit()).thenReturn(Unit.MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(3, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.getFirst());
        assertInstanceOf(MaxMinMarginFiller.class, fillers.get(1));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(2));
    }

    @Test
    void testBuildMaxMarginDiscrete() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(parameters.getFlowUnit()).thenReturn(Unit.MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(5, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.getFirst());
        assertInstanceOf(MaxMinMarginFiller.class, fillers.get(1));
        assertInstanceOf(DiscretePstTapFiller.class, fillers.get(2));
        assertInstanceOf(DiscretePstGroupFiller.class, fillers.get(3));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(4));
    }

    @Test
    void testBuildMaxRelativeMarginContinuous() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN);
        when(parameters.getFlowUnit()).thenReturn(Unit.MEGAWATT);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(3, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.getFirst());
        assertInstanceOf(MaxMinRelativeMarginFiller.class, fillers.get(1));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(2));
    }

    @Test
    void testBuildMaxMarginContinuousMnecLoopflowUnoptimized() {
        when(rangeActionParametersExtension.getPstModel()).thenReturn(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(parameters.getFlowUnit()).thenReturn(Unit.MEGAWATT);
        when(parameters.getFlowUnit()).thenReturn(Unit.MEGAWATT);

        when(parameters.isRaoWithMnecLimitation()).thenReturn(true);
        when(parameters.isRaoWithLoopFlowLimitation()).thenReturn(true);
        when(parameters.getUnoptimizedCnecParameters()).thenReturn(Mockito.mock(UnoptimizedCnecParameters.class));

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(6, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.getFirst());
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
        when(parameters.getFlowUnit()).thenReturn(Unit.MEGAWATT);
        RangeActionLimitationParameters raLimitationParameters = Mockito.mock(RangeActionLimitationParameters.class);
        when(parameters.getRaLimitationParameters()).thenReturn(raLimitationParameters);
        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(Mockito.mock(State.class)));
        when(raLimitationParameters.areRangeActionLimitedForState(Mockito.any())).thenReturn(true);

        LinearProblem linearProblem = linearProblemBuilder.buildFromInputsAndParameters(inputs, parameters);
        assertNotNull(linearProblem);
        List<ProblemFiller> fillers = linearProblem.getFillers();
        assertEquals(4, fillers.size());
        assertInstanceOf(MarginCoreProblemFiller.class, fillers.getFirst());
        assertInstanceOf(MaxMinMarginFiller.class, fillers.get(1));
        assertInstanceOf(ContinuousRangeActionGroupFiller.class, fillers.get(2));
        assertInstanceOf(RaUsageLimitsFiller.class, fillers.get(3));
    }

    private void setUpCnecFiltering(SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering filtering, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
        // costly optimization requires the APPROXIMATED_INTEGERS PST model
        when(rangeActionParametersExtension.getPstModel()).thenReturn(objectiveFunction.costOptimization()
            ? SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS
            : SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(rangeActionParametersExtension.getLinearProblemCnecFiltering()).thenReturn(filtering);
        when(parameters.getObjectiveFunction()).thenReturn(objectiveFunction);
        when(parameters.getFlowUnit()).thenReturn(Unit.MEGAWATT);
        when(minMarginsParameters.getShiftedViolationThreshold()).thenReturn(5.0);
        when(relativeMarginParameters.getPtdfSumLowerBound()).thenReturn(0.01);
        when(relativeMarginParameters.getPtdfApproximation()).thenReturn(PtdfApproximation.FIXED_PTDF);
        when(inputs.preOptimizationFlowResult()).thenReturn(Mockito.mock(FlowResult.class));
    }

    private static FlowCnecFilter getCoreFilter(List<ProblemFiller> fillers) {
        return ((AbstractCoreProblemFiller) fillers.getFirst()).getFlowCnecFilter();
    }

    private static FlowCnecFilter getMarginFilter(List<ProblemFiller> fillers) {
        return ((MaxMinMarginFiller) fillers.get(1)).getFlowCnecFilter();
    }

    private static FlowCnec mockCnec(String id, String operator) {
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        when(cnec.getId()).thenReturn(id);
        when(cnec.getOperator()).thenReturn(operator);
        return cnec;
    }

    @ParameterizedTest
    @EnumSource(ObjectiveFunctionParameters.ObjectiveFunctionType.class)
    void safeBoundsFilterIsSharedByCoreAndMarginFillers(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
        setUpCnecFiltering(SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering.SAFE_BOUNDS, objectiveFunction);

        List<ProblemFiller> fillers = ProblemFillerHelper.getProblemFillers(inputs, parameters, null);

        FlowCnecFilter coreFilter = getCoreFilter(fillers);
        assertInstanceOf(SafeFlowCnecFilter.class, coreFilter);
        assertSame(coreFilter, getMarginFilter(fillers));
        MarginCriterion expectedCriterion = switch (objectiveFunction) {
            case SECURE_FLOW, MAX_MIN_MARGIN -> new MarginCriterion.Absolute(OptionalDouble.empty());
            case MIN_COST -> new MarginCriterion.Absolute(OptionalDouble.of(5.0));
            case MAX_MIN_RELATIVE_MARGIN -> new MarginCriterion.Relative(inputs.preOptimizationFlowResult(), 0.01);
        };
        assertEquals(expectedCriterion, ((SafeFlowCnecFilter) coreFilter).getCriterion());
    }

    @ParameterizedTest
    @EnumSource(ObjectiveFunctionParameters.ObjectiveFunctionType.class)
    void disabledFilteringGivesNoFilterToCoreAndMarginFillers(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
        setUpCnecFiltering(SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering.DISABLED, objectiveFunction);

        List<ProblemFiller> fillers = ProblemFillerHelper.getProblemFillers(inputs, parameters, null);

        assertSame(FlowCnecFilter.NONE, getCoreFilter(fillers));
        assertSame(FlowCnecFilter.NONE, getMarginFilter(fillers));
    }

    @Test
    void relativeCriterionUsesLinearizationPtdfsWhenTheyAreUpdatedWithPstChanges() {
        setUpCnecFiltering(SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering.SAFE_BOUNDS, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN);
        when(relativeMarginParameters.getPtdfApproximation()).thenReturn(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST);

        SafeFlowCnecFilter filter = (SafeFlowCnecFilter) ProblemFillerHelper.buildFlowCnecFilter(inputs, parameters);

        assertEquals(new MarginCriterion.Relative(null, 0.01), filter.getCriterion());
    }

    @Test
    void eachCallBuildsItsOwnFilter() {
        setUpCnecFiltering(SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering.SAFE_BOUNDS, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);

        List<ProblemFiller> firstFillers = ProblemFillerHelper.getProblemFillers(inputs, parameters, null);
        List<ProblemFiller> secondFillers = ProblemFillerHelper.getProblemFillers(inputs, parameters, null);

        assertNotSame(getCoreFilter(firstFillers), getCoreFilter(secondFillers));
    }

    @Test
    void unoptimizedOperatorCnecsAreProtectedInCurativePerimeter() {
        setUpCnecFiltering(SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering.SAFE_BOUNDS, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        FlowCnec unoptimizedCnec = mockCnec("unoptimized", "FR");
        FlowCnec optimizedCnec = mockCnec("optimized", "BE");
        FlowCnec mnec = mockCnec("mnec", "BE");
        FlowCnec loopFlowCnec = mockCnec("loop-flow", "BE");
        when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(unoptimizedCnec, optimizedCnec, mnec, loopFlowCnec));
        when(optimizationPerimeter.getMonitoredFlowCnecs()).thenReturn(Set.of(mnec));
        when(optimizationPerimeter.getLoopFlowCnecs()).thenReturn(Set.of(loopFlowCnec));
        when(parameters.isRaoWithMnecLimitation()).thenReturn(true);
        when(parameters.isRaoWithLoopFlowLimitation()).thenReturn(true);
        UnoptimizedCnecParameters unoptimizedCnecParameters = Mockito.mock(UnoptimizedCnecParameters.class);
        when(unoptimizedCnecParameters.getOperatorsNotToOptimize()).thenReturn(Set.of("FR"));
        when(parameters.getUnoptimizedCnecParameters()).thenReturn(unoptimizedCnecParameters);

        SafeFlowCnecFilter filter = (SafeFlowCnecFilter) ProblemFillerHelper.buildFlowCnecFilter(inputs, parameters);

        assertEquals(Set.of(unoptimizedCnec, mnec, loopFlowCnec), filter.getProtectedCnecs());
        assertEquals(Set.of(unoptimizedCnec), filter.getNotWitnessCnecs());
    }

    @Test
    void unoptimizedOperatorCnecsAreNotSpecialOutsideCurativePerimeter() {
        setUpCnecFiltering(SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering.SAFE_BOUNDS, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        OptimizationPerimeter preventivePerimeter = Mockito.mock(PreventiveOptimizationPerimeter.class);
        when(inputs.optimizationPerimeter()).thenReturn(preventivePerimeter);
        FlowCnec unoptimizedCnec = mockCnec("unoptimized", "FR");
        when(preventivePerimeter.getFlowCnecs()).thenReturn(Set.of(unoptimizedCnec));
        UnoptimizedCnecParameters unoptimizedCnecParameters = Mockito.mock(UnoptimizedCnecParameters.class);
        when(unoptimizedCnecParameters.getOperatorsNotToOptimize()).thenReturn(Set.of("FR"));
        when(parameters.getUnoptimizedCnecParameters()).thenReturn(unoptimizedCnecParameters);

        SafeFlowCnecFilter filter = (SafeFlowCnecFilter) ProblemFillerHelper.buildFlowCnecFilter(inputs, parameters);

        assertTrue(filter.getProtectedCnecs().isEmpty());
        assertTrue(filter.getNotWitnessCnecs().isEmpty());
    }
}
