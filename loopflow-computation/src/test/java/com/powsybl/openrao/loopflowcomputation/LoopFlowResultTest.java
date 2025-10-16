/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.loopflowcomputation;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.cnec.BranchCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        loopFlowResult.addCnecResult(cnec, TwoSides.TWO, 1., 2., 3.);
        assertEquals(1., loopFlowResult.getLoopFlow(cnec, TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(2., loopFlowResult.getCommercialFlow(cnec, TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(3., loopFlowResult.getReferenceFlow(cnec, TwoSides.TWO), DOUBLE_TOLERANCE);
    }

    @Test
    void loopFlowResultCnecNotFound() {
        LoopFlowResult loopFlowResult = new LoopFlowResult();
        assertThrows(OpenRaoException.class, () -> loopFlowResult.getLoopFlow(cnec, TwoSides.TWO));
    }

    @Test
    void testGetCommercialFlowsMap() {
        FlowCnec cnec1 = Mockito.mock(FlowCnec.class);
        FlowCnec cnec2 = Mockito.mock(FlowCnec.class);

        LoopFlowResult loopFlowResult = new LoopFlowResult();
        loopFlowResult.addCnecResult(cnec, TwoSides.TWO, 1., 2., 3.);
        loopFlowResult.addCnecResult(cnec1, TwoSides.TWO, 1., 20., 3.);
        loopFlowResult.addCnecResult(cnec1, TwoSides.ONE, 1., 22., 3.);
        loopFlowResult.addCnecResult(cnec2, TwoSides.ONE, 1., 30., 3.);

        Map<FlowCnec, Map<TwoSides, Double>> commercialFlowsMap = loopFlowResult.getCommercialFlowsMap();
        assertEquals(2, commercialFlowsMap.size());

        assertEquals(2, commercialFlowsMap.get(cnec1).size());
        assertEquals(20., commercialFlowsMap.get(cnec1).get(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(22., commercialFlowsMap.get(cnec1).get(TwoSides.ONE), DOUBLE_TOLERANCE);

        assertEquals(1, commercialFlowsMap.get(cnec2).size());
        assertEquals(30., commercialFlowsMap.get(cnec2).get(TwoSides.ONE), DOUBLE_TOLERANCE);
    }
}
