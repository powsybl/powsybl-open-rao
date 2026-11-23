/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.BestTapFinder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.RangeActionsSynchronizationFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
class TimeCoupledIteratingLinearOptimizerTest {
    private static final OffsetDateTime TIMESTAMP_1 = OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TIMESTAMP_2 = OffsetDateTime.of(2026, 1, 9, 1, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void testCurativeMipCreatesSynchronizationFiller() {
        // 2 timestamps with curative main optimization state -> curative MIP
        List<ProblemFiller> problemFillers = TimeCoupledIteratingLinearOptimizer.getTimeCoupledProblemFillers(createTimeCoupledInput(false), Mockito.mock(IteratingLinearOptimizerParameters.class));
        assertEquals(1, problemFillers.size());
        assertInstanceOf(RangeActionsSynchronizationFiller.class, problemFillers.getFirst());
    }

    @Test
    void testGlobalMipCreatesSynchronizationFiller() {
        // 2 timestamps with preventive main optimization state + curative states -> global MIP
        List<ProblemFiller> problemFillers = TimeCoupledIteratingLinearOptimizer.getTimeCoupledProblemFillers(createTimeCoupledInput(true), Mockito.mock(IteratingLinearOptimizerParameters.class));
        assertEquals(1, problemFillers.size());
        assertInstanceOf(RangeActionsSynchronizationFiller.class, problemFillers.getFirst());
    }

    @Test
    void hybridMipTransitionResetsOnceAndRefillsTimeCoupledConstraintsAfterTimestampModels() {
        LinearProblem linearProblem = Mockito.mock(LinearProblem.class);
        ProblemFiller firstTimestampFiller = Mockito.mock(ProblemFiller.class);
        ProblemFiller secondTimestampFiller = Mockito.mock(ProblemFiller.class);
        ProblemFiller timeCoupledFiller = Mockito.mock(ProblemFiller.class);
        RangeActionActivationResult firstActivationResult = Mockito.mock(RangeActionActivationResult.class);
        RangeActionActivationResult secondActivationResult = Mockito.mock(RangeActionActivationResult.class);
        TemporalData<List<ProblemFiller>> timestampFillers = new TemporalDataImpl<>(Map.of(
            TIMESTAMP_1, List.of(firstTimestampFiller),
            TIMESTAMP_2, List.of(secondTimestampFiller)
        ));
        TemporalData<RangeActionActivationResult> activationResults = new TemporalDataImpl<>(Map.of(
            TIMESTAMP_1, firstActivationResult,
            TIMESTAMP_2, secondActivationResult
        ));

        TimeCoupledIteratingLinearOptimizer.updateLinearProblemBetweenMipIterations(
            linearProblem, timestampFillers, activationResults);

        InOrder inOrder = Mockito.inOrder(linearProblem, firstTimestampFiller, secondTimestampFiller, timeCoupledFiller);
        inOrder.verify(firstTimestampFiller).updateBetweenMipIteration(linearProblem, firstActivationResult);
        inOrder.verify(secondTimestampFiller).updateBetweenMipIteration(linearProblem, secondActivationResult);
        verifyNoMoreInteractions(timeCoupledFiller, firstTimestampFiller, secondTimestampFiller);
    }

    @Test
    void hybridRoundingUsesBestTapFinderFirstIterationOnlyWhenHybridPst() {
        IteratingLinearOptimizerParameters parameters = mockParameters(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.HYBRID);
        when(parameters.getFlowUnit()).thenReturn(Unit.MEGAWATT);
        IteratingLinearOptimizerInput input = Mockito.mock(IteratingLinearOptimizerInput.class);
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of());
        when(input.optimizationPerimeter()).thenReturn(optimizationPerimeter);
        Network network = Mockito.mock(Network.class);
        when(input.network()).thenReturn(network);
        RangeActionSetpointResult prePerimeterSetpoints = new RangeActionSetpointResultImpl(Map.of());
        when(input.prePerimeterSetpoints()).thenReturn(prePerimeterSetpoints);
        RangeActionActivationResult linearProblemResult = Mockito.mock(RangeActionActivationResult.class);
        GlobalLinearOptimizationResult previousResult = Mockito.mock(GlobalLinearOptimizationResult.class);
        RangeActionActivationResultImpl roundedResult = new RangeActionActivationResultImpl(prePerimeterSetpoints);

        try (MockedStatic<BestTapFinder> bestTapFinder = Mockito.mockStatic(BestTapFinder.class)) {
            bestTapFinder.when(() -> BestTapFinder.round(
                linearProblemResult, network, optimizationPerimeter, prePerimeterSetpoints, previousResult, Unit.MEGAWATT
            )).thenReturn(roundedResult);

            assertSame(roundedResult, TimeCoupledIteratingLinearOptimizer.roundResult(linearProblemResult, previousResult, input, parameters, 1));
            assertNotSame(roundedResult, TimeCoupledIteratingLinearOptimizer.roundResult(linearProblemResult, previousResult, input, parameters, 2));

            bestTapFinder.verify(() -> BestTapFinder.round(
                linearProblemResult, network, optimizationPerimeter, prePerimeterSetpoints, previousResult, Unit.MEGAWATT
            ), Mockito.times(1));
        }
    }

    @Test
    void failedHybridRefinementKeepsFirstRoundedResults() {
        IteratingLinearOptimizerParameters parameters = mockParameters(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.HYBRID);
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        State state = Mockito.mock(State.class);
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(state, Set.of(pstRangeAction)));
        IteratingLinearOptimizerInput iteratingInput = mockIteratingInput(optimizationPerimeter);
        when(iteratingInput.prePerimeterSetpoints()).thenReturn(new RangeActionSetpointResultImpl(Map.of()));
        TimeCoupledIteratingLinearOptimizerInput input = createSingleTimestampInput(iteratingInput);
        RangeActionActivationResult firstRoundedResult = Mockito.mock(RangeActionActivationResult.class);
        TemporalData<RangeActionActivationResult> firstRoundedResults = new TemporalDataImpl<>(Map.of(TIMESTAMP_1, firstRoundedResult));
        TemporalData<List<ProblemFiller>> timestampFillers = new TemporalDataImpl<>(Map.of(TIMESTAMP_1, List.of()));
        LinearProblem linearProblem = Mockito.mock(LinearProblem.class);
        when(linearProblem.solve()).thenReturn(LinearProblemStatus.INFEASIBLE);

        TemporalData<RangeActionActivationResult> result = TimeCoupledIteratingLinearOptimizer.reSolveIfApproximatedIntegerOrHybridPstModel(
            Mockito.mock(GlobalLinearOptimizationResult.class), linearProblem, 1, firstRoundedResults, input, parameters, timestampFillers, 1
        );

        assertSame(firstRoundedResult, result.getData(TIMESTAMP_1).orElseThrow());
        Mockito.verify(linearProblem).solve();
    }

    @Test
    void noPstInAnyOptimizationPerimeter() {
        IteratingLinearOptimizerParameters parameters = mockParameters(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.HYBRID);

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        // No PST to optimize
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of());
        IteratingLinearOptimizerInput iteratingInput = mockIteratingInput(optimizationPerimeter);
        when(iteratingInput.prePerimeterSetpoints()).thenReturn(new RangeActionSetpointResultImpl(Map.of()));
        TimeCoupledIteratingLinearOptimizerInput input = createSingleTimestampInput(iteratingInput);
        RangeActionActivationResult firstRoundedResult = Mockito.mock(RangeActionActivationResult.class);
        TemporalData<RangeActionActivationResult> firstRoundedResults = new TemporalDataImpl<>(Map.of(TIMESTAMP_1, firstRoundedResult));
        TemporalData<List<ProblemFiller>> timestampFillers = new TemporalDataImpl<>(Map.of(TIMESTAMP_1, List.of()));
        LinearProblem linearProblem = Mockito.mock(LinearProblem.class);

        TemporalData<RangeActionActivationResult> result = TimeCoupledIteratingLinearOptimizer.reSolveIfApproximatedIntegerOrHybridPstModel(
            Mockito.mock(GlobalLinearOptimizationResult.class), linearProblem, 1, firstRoundedResults, input, parameters, timestampFillers, 1
        );

        assertSame(firstRoundedResult, result.getData(TIMESTAMP_1).orElseThrow());
        Mockito.verifyNoInteractions(linearProblem);
    }

    private static TimeCoupledIteratingLinearOptimizerInput createTimeCoupledInput(boolean isGlobalMip) {
        TemporalData<IteratingLinearOptimizerInput> iteratingLinearOptimizerInputs = new TemporalDataImpl<>(Map.of(
                TIMESTAMP_1, mockIteratingLinearOptimizerInput(isGlobalMip),
                TIMESTAMP_2, mockIteratingLinearOptimizerInput(isGlobalMip)
        ));
        return new TimeCoupledIteratingLinearOptimizerInput(iteratingLinearOptimizerInputs, Mockito.mock(ObjectiveFunction.class), Mockito.mock(TimeCoupledConstraints.class), true);
    }

    private static TimeCoupledIteratingLinearOptimizerInput createSingleTimestampInput(IteratingLinearOptimizerInput iteratingInput) {
        return new TimeCoupledIteratingLinearOptimizerInput(
            new TemporalDataImpl<>(Map.of(TIMESTAMP_1, iteratingInput)),
            Mockito.mock(ObjectiveFunction.class),
            Mockito.mock(TimeCoupledConstraints.class),
            false
        );
    }

    private static IteratingLinearOptimizerInput mockIteratingInput(OptimizationPerimeter optimizationPerimeter) {
        IteratingLinearOptimizerInput iteratingInput = Mockito.mock(IteratingLinearOptimizerInput.class);
        when(iteratingInput.optimizationPerimeter()).thenReturn(optimizationPerimeter);
        return iteratingInput;
    }

    private static IteratingLinearOptimizerParameters mockParameters(SearchTreeRaoRangeActionsOptimizationParameters.PstModel pstModel) {
        SearchTreeRaoRangeActionsOptimizationParameters rangeActionParameters = Mockito.mock(SearchTreeRaoRangeActionsOptimizationParameters.class);
        when(rangeActionParameters.getPstModel()).thenReturn(pstModel);
        IteratingLinearOptimizerParameters parameters = Mockito.mock(IteratingLinearOptimizerParameters.class);
        when(parameters.getRangeActionParametersExtension()).thenReturn(rangeActionParameters);
        return parameters;
    }

    private static IteratingLinearOptimizerInput mockIteratingLinearOptimizerInput(boolean isGlobalMip) {
        State mainOptimizationState = mockState(!isGlobalMip); // global mip : true, curative mip : false
        Map<State, Set<RangeAction<?>>> stateRangeActionsMap = isGlobalMip
                ? Map.of(mainOptimizationState, Set.of(), mockState(true), Set.of())
                : Map.of(mainOptimizationState, Set.of());
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(mainOptimizationState);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(stateRangeActionsMap);
        IteratingLinearOptimizerInput iteratingLinearOptimizerInput = Mockito.mock(IteratingLinearOptimizerInput.class);
        when(iteratingLinearOptimizerInput.optimizationPerimeter()).thenReturn(optimizationPerimeter);
        return iteratingLinearOptimizerInput;
    }

    private static State mockState(boolean isCurative) {
        Instant instant = Mockito.mock(Instant.class);
        when(instant.isCurative()).thenReturn(isCurative);
        when(instant.isPreventive()).thenReturn(!isCurative);
        State state = Mockito.mock(State.class);
        when(state.getInstant()).thenReturn(instant);
        return state;
    }
}
