/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class EmptyFlowResultTest {
    @Test
    public void testBasicReturns() {
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        EmptyFlowResult branchResult = new EmptyFlowResult();
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getFlow(cnec, Unit.AMPERE)));
        assertTrue(Double.isNaN(branchResult.getCommercialFlow(cnec, Unit.MEGAWATT)));
        assertTrue(Double.isNaN(branchResult.getPtdfZonalSum(cnec)));
        assertTrue(branchResult.getPtdfZonalSums().isEmpty());
    }
}
