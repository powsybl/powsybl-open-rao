/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TestsUtils {
    public static final OffsetDateTime TIMESTAMP_1 = OffsetDateTime.of(2025, 2, 17, 13, 33, 0, 0, ZoneOffset.UTC);
    public static final OffsetDateTime TIMESTAMP_2 = OffsetDateTime.of(2025, 2, 18, 13, 33, 0, 0, ZoneOffset.UTC);
    public static final OffsetDateTime TIMESTAMP_3 = OffsetDateTime.of(2025, 2, 19, 13, 33, 0, 0, ZoneOffset.UTC);

    private TestsUtils() {
    }

    public static State mockState(OffsetDateTime timestamp) {
        State state = Mockito.mock(State.class);
        Mockito.when(state.getTimestamp()).thenReturn(Optional.of(timestamp));
        return state;
    }

    public static FlowCnec mockFlowCnec(State state) {
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnec.getState()).thenReturn(state);
        return flowCnec;
    }

    public static FlowResult createMockedFlowResult(ComputationStatus computationStatus) {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        Mockito.when(flowResult.getComputationStatus()).thenReturn(computationStatus);
        return flowResult;
    }

    public static void mockFlowResult(FlowResult mockedFlowResult, FlowCnec flowCnec, double flow, double margin) {
        Mockito.when(mockedFlowResult.getFlow(flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(flow);
        Mockito.when(mockedFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(margin);
    }

    public static SensitivityResult createMockedSensitivityResult(ComputationStatus computationStatus) {
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        Mockito.when(sensitivityResult.getSensitivityStatus()).thenReturn(computationStatus);
        return sensitivityResult;
    }

    public static void mockSensitivityResult(SensitivityResult mockedSensitivityResult, FlowCnec flowCnec, RangeAction<?> rangeAction, double sensitivityValue) {
        Mockito.when(mockedSensitivityResult.getSensitivityValue(flowCnec, TwoSides.ONE, rangeAction, Unit.MEGAWATT)).thenReturn(sensitivityValue);
    }

    public static RangeActionActivationResult mockRangeActionActivationResult(State state, PstRangeAction pstRangeAction, int tap, double setPoint) {
        RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        Mockito.when(rangeActionActivationResult.getRangeActions()).thenReturn(Set.of(pstRangeAction));
        Mockito.when(rangeActionActivationResult.getActivatedRangeActions(state)).thenReturn(Set.of(pstRangeAction));
        Mockito.when(rangeActionActivationResult.getActivatedRangeActionsPerState()).thenReturn(Map.of(state, Set.of(pstRangeAction)));
        Mockito.when(rangeActionActivationResult.getOptimizedTap(pstRangeAction, state)).thenReturn(tap);
        Mockito.when(rangeActionActivationResult.getTapVariation(pstRangeAction, state)).thenReturn(tap);
        Mockito.when(rangeActionActivationResult.getOptimizedTapsOnState(state)).thenReturn(Map.of(pstRangeAction, tap));
        Mockito.when(rangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, state)).thenReturn(setPoint);
        Mockito.when(rangeActionActivationResult.getSetPointVariation(pstRangeAction, state)).thenReturn(setPoint);
        Mockito.when(rangeActionActivationResult.getOptimizedSetpointsOnState(state)).thenReturn(Map.of(pstRangeAction, setPoint));
        return rangeActionActivationResult;
    }
}
