/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.CurativeRangeActionsSynchronizationFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
        List<ProblemFiller> problemFillers = TimeCoupledIteratingLinearOptimizer.getTimeCoupledProblemFillers(createTimeCoupledInput(false));
        assertEquals(1, problemFillers.size());
        assertInstanceOf(CurativeRangeActionsSynchronizationFiller.class, problemFillers.getFirst());
    }

    @Test
    void testGlobalMipCreatesSynchronizationFiller() {
        // 2 timestamps with preventive main optimization state + curative states -> global MIP
        List<ProblemFiller> problemFillers = TimeCoupledIteratingLinearOptimizer.getTimeCoupledProblemFillers(createTimeCoupledInput(true));
        assertEquals(1, problemFillers.size());
        assertInstanceOf(CurativeRangeActionsSynchronizationFiller.class, problemFillers.getFirst());
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
