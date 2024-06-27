/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
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
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, TwoSides.TWO, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, TwoSides.ONE, Unit.AMPERE)));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, TwoSides.TWO, Unit.AMPERE)));
        assertTrue(Double.isNaN(branchResult.getCommercialFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getCommercialFlow(cnec, TwoSides.TWO, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getPtdfZonalSum(cnec, TwoSides.ONE)));
        assertTrue(Double.isNaN(branchResult.getPtdfZonalSum(cnec, TwoSides.TWO)));
        assertTrue(branchResult.getPtdfZonalSums().isEmpty());
    }
}
