/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class EmptyFlowResultImplTest {
    @Test
    void testBasicReturns() {
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        EmptyFlowResultImpl branchResult = new EmptyFlowResultImpl();
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, Side.LEFT, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, Side.RIGHT, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, Side.LEFT, Unit.AMPERE)));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, Side.RIGHT, Unit.AMPERE)));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, Side.RIGHT, Unit.AMPERE, Mockito.mock(Instant.class))));
        assertTrue(Double.isNaN(branchResult.getCommercialFlow(cnec, Side.LEFT, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getCommercialFlow(cnec, Side.RIGHT, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getPtdfZonalSum(cnec, Side.LEFT)));
        assertTrue(Double.isNaN(branchResult.getPtdfZonalSum(cnec, Side.RIGHT)));
        assertTrue(branchResult.getPtdfZonalSums().isEmpty());
    }
}
