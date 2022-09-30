/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowResultTest {

    private final static double DOUBLE_TOLERANCE = 0.01;
    private BranchCnec<?> cnec;

    @Before
    public void setUp() {
        cnec = Mockito.mock(BranchCnec.class);
    }

    @Test
    public void loopFlowResultTest() {
        LoopFlowResult loopFlowResult = new LoopFlowResult();
        loopFlowResult.addCnecResult(cnec, Side.RIGHT, 1., 2., 3.);
        assertEquals(1., loopFlowResult.getLoopFlow(cnec, Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(2., loopFlowResult.getCommercialFlow(cnec, Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(3., loopFlowResult.getReferenceFlow(cnec, Side.RIGHT), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void loopFlowResultCnecNotFound() {
        LoopFlowResult loopFlowResult = new LoopFlowResult();
        loopFlowResult.getLoopFlow(cnec, Side.RIGHT);
    }
}

