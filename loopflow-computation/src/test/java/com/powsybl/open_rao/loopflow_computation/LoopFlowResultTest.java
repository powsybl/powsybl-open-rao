/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.loopflow_computation;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.cnec.BranchCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class LoopFlowResultTest {

    private static final double DOUBLE_TOLERANCE = 0.01;
    private BranchCnec<?> cnec;

    @BeforeEach
    public void setUp() {
        cnec = Mockito.mock(BranchCnec.class);
    }

    @Test
    void loopFlowResultTest() {
        LoopFlowResult loopFlowResult = new LoopFlowResult();
        loopFlowResult.addCnecResult(cnec, Side.RIGHT, 1., 2., 3.);
        assertEquals(1., loopFlowResult.getLoopFlow(cnec, Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(2., loopFlowResult.getCommercialFlow(cnec, Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(3., loopFlowResult.getReferenceFlow(cnec, Side.RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    void loopFlowResultCnecNotFound() {
        LoopFlowResult loopFlowResult = new LoopFlowResult();
        assertThrows(OpenRaoException.class, () -> loopFlowResult.getLoopFlow(cnec, Side.RIGHT));
    }
}

