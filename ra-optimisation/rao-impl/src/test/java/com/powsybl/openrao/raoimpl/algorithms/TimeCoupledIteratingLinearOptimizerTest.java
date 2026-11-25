/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RangeActionsSynchronizationFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.raoimpl.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionActivationResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.raoimpl.result.api.SensitivityResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
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
    void fillersOfOneTimestampShareTheSameLinearizationPoint() {
        // the safe CNEC filter caches its result on the identity of its inputs: all the fillers of a timestamp must get the same objects
        ProblemFiller filler11 = Mockito.mock(ProblemFiller.class);
        ProblemFiller filler12 = Mockito.mock(ProblemFiller.class);
        ProblemFiller filler21 = Mockito.mock(ProblemFiller.class);
        ProblemFiller filler22 = Mockito.mock(ProblemFiller.class);
        FlowResult flowResult1 = Mockito.mock(FlowResult.class);
        FlowResult flowResult2 = Mockito.mock(FlowResult.class);
        SensitivityResult sensitivityResult1 = Mockito.mock(SensitivityResult.class);
        SensitivityResult sensitivityResult2 = Mockito.mock(SensitivityResult.class);
        LinearProblem linearProblem = Mockito.mock(LinearProblem.class);

        TimeCoupledIteratingLinearOptimizer.fillLinearProblem(
            linearProblem,
            new TemporalDataImpl<>(Map.of(TIMESTAMP_1, List.of(filler11, filler12), TIMESTAMP_2, List.of(filler21, filler22))),
            List.of(),
            new TemporalDataImpl<>(Map.of(TIMESTAMP_1, flowResult1, TIMESTAMP_2, flowResult2)),
            new TemporalDataImpl<>(Map.of(TIMESTAMP_1, sensitivityResult1, TIMESTAMP_2, sensitivityResult2)),
            new TemporalDataImpl<>(Map.of(TIMESTAMP_1, Mockito.mock(RangeActionSetpointResult.class), TIMESTAMP_2, Mockito.mock(RangeActionSetpointResult.class))));

        RangeActionActivationResult linearizationPoint1 = captureLinearizationPoint(filler11, linearProblem, flowResult1, sensitivityResult1);
        RangeActionActivationResult linearizationPoint2 = captureLinearizationPoint(filler21, linearProblem, flowResult2, sensitivityResult2);
        assertSame(linearizationPoint1, captureLinearizationPoint(filler12, linearProblem, flowResult1, sensitivityResult1));
        assertSame(linearizationPoint2, captureLinearizationPoint(filler22, linearProblem, flowResult2, sensitivityResult2));
        assertNotSame(linearizationPoint1, linearizationPoint2);
    }

    private static RangeActionActivationResult captureLinearizationPoint(ProblemFiller filler, LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        ArgumentCaptor<RangeActionActivationResult> captor = ArgumentCaptor.forClass(RangeActionActivationResult.class);
        verify(filler).fill(same(linearProblem), same(flowResult), same(sensitivityResult), captor.capture());
        return captor.getValue();
    }

    private static TimeCoupledIteratingLinearOptimizerInput createTimeCoupledInput(boolean isGlobalMip) {
        TemporalData<IteratingLinearOptimizerInput> iteratingLinearOptimizerInputs = new TemporalDataImpl<>(Map.of(
                TIMESTAMP_1, mockIteratingLinearOptimizerInput(isGlobalMip),
                TIMESTAMP_2, mockIteratingLinearOptimizerInput(isGlobalMip)
        ));
        return new TimeCoupledIteratingLinearOptimizerInput(iteratingLinearOptimizerInputs, Mockito.mock(ObjectiveFunction.class), Mockito.mock(TimeCoupledConstraints.class), true);
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
