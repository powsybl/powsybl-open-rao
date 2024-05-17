/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class FillersUtilTest {
    @Test
    public void testGetValidFlowCnecs() {
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        FlowCnec cnec1 = Mockito.mock(FlowCnec.class);
        Mockito.when(cnec1.getState()).thenReturn(state1);
        FlowCnec cnec2 = Mockito.mock(FlowCnec.class);
        Mockito.when(cnec2.getState()).thenReturn(state2);
        Set<FlowCnec> cnecs = Set.of(cnec1, cnec2);

        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);

        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(Set.of(cnec1, cnec2), FillersUtil.getValidFlowCnecs(cnecs, sensitivityResult));

        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.FAILURE);
        assertEquals(Set.of(cnec1), FillersUtil.getValidFlowCnecs(cnecs, sensitivityResult));

        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.FAILURE);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(Set.of(cnec2), FillersUtil.getValidFlowCnecs(cnecs, sensitivityResult));

        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.FAILURE);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.FAILURE);
        assertEquals(Set.of(), FillersUtil.getValidFlowCnecs(cnecs, sensitivityResult));
    }
}
