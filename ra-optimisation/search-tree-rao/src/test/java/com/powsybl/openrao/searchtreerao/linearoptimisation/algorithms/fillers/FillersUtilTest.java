/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class FillersUtilTest {
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private Set<FlowCnec> cnecs;
    State state1;
    State state2;

    @BeforeEach
    void setUp() {
        state1 = Mockito.mock(State.class);
        state2 = Mockito.mock(State.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        Mockito.when(cnec1.getId()).thenReturn("cnec1");
        Mockito.when(cnec1.getState()).thenReturn(state1);
        Mockito.when(cnec1.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE, TwoSides.TWO));
        cnec2 = Mockito.mock(FlowCnec.class);
        Mockito.when(cnec2.getId()).thenReturn("cnec2");
        Mockito.when(cnec2.getState()).thenReturn(state2);
        Mockito.when(cnec2.getMonitoredSides()).thenReturn(Set.of(TwoSides.TWO));
        cnecs = Set.of(cnec1, cnec2);
    }

    @Test
    void testGetValidFlowCnecsSensi() {
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);

        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(Set.of(cnec1, cnec2), FillersUtil.getFlowCnecsComputationStatusOk(cnecs, sensitivityResult));

        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.FAILURE);
        assertEquals(Set.of(cnec1), FillersUtil.getFlowCnecsComputationStatusOk(cnecs, sensitivityResult));

        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.FAILURE);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(Set.of(cnec2), FillersUtil.getFlowCnecsComputationStatusOk(cnecs, sensitivityResult));

        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.FAILURE);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.FAILURE);
        assertEquals(Set.of(), FillersUtil.getFlowCnecsComputationStatusOk(cnecs, sensitivityResult));
    }

    @Test
    void testGetValidFlowCnecsFlow() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);

        Mockito.when(flowResult.getFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(1.0);
        Mockito.when(flowResult.getFlow(cnec1, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(flowResult.getFlow(cnec2, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(flowResult.getFlow(cnec2, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Double.NaN);
        assertEquals(Set.of(), FillersUtil.getFlowCnecsNotNaNFlow(cnecs, flowResult));

        Mockito.when(flowResult.getFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(1.0);
        Mockito.when(flowResult.getFlow(cnec1, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(1.0);
        Mockito.when(flowResult.getFlow(cnec2, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(1.0);
        Mockito.when(flowResult.getFlow(cnec2, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Double.NaN);
        assertEquals(Set.of(cnec1), FillersUtil.getFlowCnecsNotNaNFlow(cnecs, flowResult));

        Mockito.when(flowResult.getFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(1.0);
        Mockito.when(flowResult.getFlow(cnec1, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(flowResult.getFlow(cnec1, TwoSides.TWO, Unit.AMPERE)).thenReturn(4.0);
        Mockito.when(flowResult.getFlow(cnec2, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(flowResult.getFlow(cnec2, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(3.0);
        assertEquals(Set.of(cnec2), FillersUtil.getFlowCnecsNotNaNFlow(cnecs, flowResult));
    }
}
