/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
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
    private Cnec cnec;

    @Before
    public void setUp() {
        cnec = Mockito.mock(Cnec.class);
    }

    @Test
    public void loopFlowResultTest() {
        LoopFlowResult loopFlowResult = new LoopFlowResult();
        loopFlowResult.addCnecResult(cnec, 1., 2., 3.);
        assertTrue(loopFlowResult.containValues(cnec));
        assertEquals(1., loopFlowResult.getLoopFlow(cnec), DOUBLE_TOLERANCE);
        assertEquals(2., loopFlowResult.getCommercialFlow(cnec), DOUBLE_TOLERANCE);
        assertEquals(3., loopFlowResult.getReferenceFlow(cnec), DOUBLE_TOLERANCE);

        assertFalse(loopFlowResult.containValues(Mockito.mock(Cnec.class)));
    }

    @Test(expected = FaraoException.class)
    public void loopFlowResultCnecNotFound() {
        LoopFlowResult loopFlowResult = new LoopFlowResult();
        loopFlowResult.getLoopFlow(cnec);
    }
}

