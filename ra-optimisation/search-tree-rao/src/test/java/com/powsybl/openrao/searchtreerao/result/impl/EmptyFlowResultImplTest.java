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
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class EmptyFlowResultImplTest {
    @Test
    void testBasicReturns() {
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        EmptyFlowResultImpl flowResult = new EmptyFlowResultImpl();
        assertTrue(Double.isNaN(flowResult.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(flowResult.getFlow(cnec, TwoSides.TWO, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(flowResult.getFlow(cnec, TwoSides.ONE, Unit.AMPERE)));
        assertTrue(Double.isNaN(flowResult.getFlow(cnec, TwoSides.TWO, Unit.AMPERE)));
        assertTrue(Double.isNaN(flowResult.getFlow(cnec, TwoSides.TWO, Unit.AMPERE, Mockito.mock(Instant.class))));
        assertTrue(Double.isNaN(flowResult.getCommercialFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(flowResult.getCommercialFlow(cnec, TwoSides.TWO, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(flowResult.getPtdfZonalSum(cnec, TwoSides.ONE)));
        assertTrue(Double.isNaN(flowResult.getPtdfZonalSum(cnec, TwoSides.TWO)));
        assertTrue(flowResult.getPtdfZonalSums().isEmpty());
        assertEquals(ComputationStatus.DEFAULT, flowResult.getComputationStatus());
        assertEquals(ComputationStatus.DEFAULT, flowResult.getComputationStatus(Mockito.mock(State.class)));
    }
}
