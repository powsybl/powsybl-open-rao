/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.loopflowcomputation;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.BranchCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

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
        loopFlowResult.addCnecResult(cnec, TwoSides.TWO, 1., 2., 3., Unit.MEGAWATT);
        assertEquals(1., loopFlowResult.getLoopFlow(cnec, TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(2., loopFlowResult.getCommercialFlow(cnec, TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3., loopFlowResult.getReferenceFlow(cnec, TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        // side not found
        OpenRaoException exSideLoop = assertThrows(OpenRaoException.class, () -> loopFlowResult.getLoopFlow(cnec, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(String.format("No loop-flow value found for cnec %s on side %s in %s", cnec.getId(), TwoSides.ONE, Unit.MEGAWATT), exSideLoop.getMessage());

        OpenRaoException exSideComm = assertThrows(OpenRaoException.class, () -> loopFlowResult.getCommercialFlow(cnec, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(String.format("No commercial flow value found for cnec %s on side %s in %s", cnec.getId(), TwoSides.ONE, Unit.MEGAWATT), exSideComm.getMessage());

        OpenRaoException exSideRef = assertThrows(OpenRaoException.class, () -> loopFlowResult.getReferenceFlow(cnec, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(String.format("No reference flow value found for cnec %s on side %s in %s", cnec.getId(), TwoSides.ONE, Unit.MEGAWATT), exSideRef.getMessage());

        // unit not found
        OpenRaoException exUnitLoop = assertThrows(OpenRaoException.class, () -> loopFlowResult.getLoopFlow(cnec, TwoSides.TWO, Unit.AMPERE));
        assertEquals(String.format("No loop-flow value found for cnec %s on side %s in %s", cnec.getId(), TwoSides.TWO, Unit.AMPERE), exUnitLoop.getMessage());

        OpenRaoException exUnitComm = assertThrows(OpenRaoException.class, () -> loopFlowResult.getCommercialFlow(cnec, TwoSides.TWO, Unit.AMPERE));
        assertEquals(String.format("No commercial flow value found for cnec %s on side %s in %s", cnec.getId(), TwoSides.TWO, Unit.AMPERE), exUnitComm.getMessage());

        OpenRaoException exUnitRef = assertThrows(OpenRaoException.class, () -> loopFlowResult.getReferenceFlow(cnec, TwoSides.TWO, Unit.AMPERE));
        assertEquals(String.format("No reference flow value found for cnec %s on side %s in %s", cnec.getId(), TwoSides.TWO, Unit.AMPERE), exUnitRef.getMessage());
    }

    @Test
    void loopFlowResultCnecNotFound() {
        LoopFlowResult loopFlowResult = new LoopFlowResult();
        assertThrows(OpenRaoException.class, () -> loopFlowResult.getLoopFlow(cnec, TwoSides.TWO, Unit.MEGAWATT));
    }

    @Test
    void testGetCommercialFlowsMap() {
        FlowCnec cnec1 = Mockito.mock(FlowCnec.class);
        FlowCnec cnec2 = Mockito.mock(FlowCnec.class);

        LoopFlowResult loopFlowResult = new LoopFlowResult();
        loopFlowResult.addCnecResult(cnec, TwoSides.TWO, 1., 2., 3., Unit.MEGAWATT);
        loopFlowResult.addCnecResult(cnec1, TwoSides.TWO, 1., 20., 3., Unit.MEGAWATT);
        loopFlowResult.addCnecResult(cnec1, TwoSides.ONE, 1., 22., 3., Unit.MEGAWATT);
        loopFlowResult.addCnecResult(cnec1, TwoSides.ONE, 1., 21., 3., Unit.AMPERE);
        loopFlowResult.addCnecResult(cnec2, TwoSides.ONE, 1., 30., 3., Unit.MEGAWATT);
        loopFlowResult.addCnecResult(cnec2, TwoSides.ONE, 1., 35., 3., Unit.AMPERE);

        // get CommercialFlows only consider FlowCnecs ! BranchCnecs that are not FlowCnecs ares filtered out (ie cnec here)
        Map<FlowCnec, Map<TwoSides, Map<Unit, Double>>> commercialFlowsMap = loopFlowResult.getCommercialFlowsMap();
        assertEquals(2, commercialFlowsMap.size());

        assertEquals(2, commercialFlowsMap.get(cnec1).size());
        assertEquals(1, commercialFlowsMap.get(cnec1).get(TwoSides.TWO).size());
        assertEquals(20., commercialFlowsMap.get(cnec1).get(TwoSides.TWO).get(Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(2, commercialFlowsMap.get(cnec1).get(TwoSides.ONE).size());
        assertEquals(22., commercialFlowsMap.get(cnec1).get(TwoSides.ONE).get(Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(21., commercialFlowsMap.get(cnec1).get(TwoSides.ONE).get(Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1, commercialFlowsMap.get(cnec2).size());
        assertEquals(2, commercialFlowsMap.get(cnec2).get(TwoSides.ONE).size());
        assertEquals(30., commercialFlowsMap.get(cnec2).get(TwoSides.ONE).get(Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(35., commercialFlowsMap.get(cnec2).get(TwoSides.ONE).get(Unit.AMPERE), DOUBLE_TOLERANCE);
    }
}
