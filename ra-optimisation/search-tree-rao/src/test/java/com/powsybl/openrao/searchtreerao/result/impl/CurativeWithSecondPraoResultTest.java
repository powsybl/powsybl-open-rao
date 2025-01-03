/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CurativeWithSecondPraoResultTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;

    private State mockStateWithContingency(String id) {
        Contingency contingency = mock(Contingency.class);
        State state = mock(State.class);
        when(state.getContingency()).thenReturn(Optional.of(contingency));
        when(state.getId()).thenReturn(id);
        return state;
    }

    private FlowCnec mockFlowCnec(State state, String id) {
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getId()).thenReturn(id);
        when(cnec.getState()).thenReturn(state);
        return cnec;
    }

    @Test
    void testGetFlow() {
        State state1 = mockStateWithContingency("state1");
        State state2 = mockStateWithContingency("state2");
        FlowCnec cnec1 = mockFlowCnec(state1, "cnec1");
        FlowCnec cnec2 = mockFlowCnec(state2, "cnec2");
        PrePerimeterResult postCraPrePerimeterResult = mock(PrePerimeterResult.class);
        when(postCraPrePerimeterResult.getFlow(eq(cnec1), any(), any())).thenReturn(135.4);
        when(postCraPrePerimeterResult.getFlow(eq(cnec1), any(), any(), any())).thenReturn(135.4);

        CurativeWithSecondPraoResult result = new CurativeWithSecondPraoResult(state1, null, null, null, postCraPrePerimeterResult, false);

        assertEquals(135.4, result.getFlow(cnec1, TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(135.4, result.getFlow(cnec1, TwoSides.ONE, Unit.AMPERE, mock(Instant.class)), DOUBLE_TOLERANCE);

        Exception e = assertThrows(OpenRaoException.class, () -> result.getFlow(cnec2, TwoSides.TWO, Unit.MEGAWATT));
        assertEquals("Cnec cnec2 has a different contingency than this result's state (state1)", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> result.getFlow(cnec2, TwoSides.ONE, Unit.AMPERE, mock(Instant.class)));
        assertEquals("Cnec cnec2 has a different contingency than this result's state (state1)", e.getMessage());
    }
}
