/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class EmptyFlowResultImplTest {
    @Test
    void testBasicReturns() {
        Instant instant = mock(Instant.class);
        State state = mock(State.class);
        FlowCnec cnec = mock(FlowCnec.class);
        when(state.getInstant()).thenReturn(instant);
        when(cnec.getState()).thenReturn(state);

        EmptyFlowResultImpl branchResult = new EmptyFlowResultImpl();
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT, cnec.getState().getInstant())));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, TwoSides.TWO, Unit.MEGAWATT, cnec.getState().getInstant())));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, TwoSides.ONE, Unit.AMPERE, cnec.getState().getInstant())));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, TwoSides.TWO, Unit.AMPERE, cnec.getState().getInstant())));
        assertTrue(Double.isNaN(branchResult.getCommercialFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getCommercialFlow(cnec, TwoSides.TWO, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getPtdfZonalSum(cnec, TwoSides.ONE)));
        assertTrue(Double.isNaN(branchResult.getPtdfZonalSum(cnec, TwoSides.TWO)));
        assertTrue(branchResult.getPtdfZonalSums().isEmpty());
    }
}
