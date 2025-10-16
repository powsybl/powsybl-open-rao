/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.iidm.network.TwoSides;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class ElementaryFlowCnecResultTest {

    private void assertMarginsAreNan(ElementaryFlowCnecResult elementaryFlowCnecResult, Unit unit) {
        assertEquals(Double.NaN, elementaryFlowCnecResult.getMargin(unit), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getRelativeMargin(unit), 1e-3);
    }

    private void assertFlowsAreNan(ElementaryFlowCnecResult elementaryFlowCnecResult, Unit unit, TwoSides side) {
        assertEquals(Double.NaN, elementaryFlowCnecResult.getFlow(side, unit), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getCommercialFlow(side, unit), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getLoopFlow(side, unit), 1e-3);
    }

    @Test
    void defaultValuesTest() {
        ElementaryFlowCnecResult elementaryFlowCnecResult = new ElementaryFlowCnecResult();

        assertMarginsAreNan(elementaryFlowCnecResult, Unit.MEGAWATT);
        assertMarginsAreNan(elementaryFlowCnecResult, Unit.AMPERE);

        assertFlowsAreNan(elementaryFlowCnecResult, Unit.MEGAWATT, TwoSides.ONE);
        assertFlowsAreNan(elementaryFlowCnecResult, Unit.MEGAWATT, TwoSides.TWO);
        assertFlowsAreNan(elementaryFlowCnecResult, Unit.AMPERE, TwoSides.ONE);
        assertFlowsAreNan(elementaryFlowCnecResult, Unit.AMPERE, TwoSides.TWO);

        assertEquals(Double.NaN, elementaryFlowCnecResult.getPtdfZonalSum(TwoSides.ONE), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getPtdfZonalSum(TwoSides.TWO), 1e-3);
    }

    @Test
    void getterAndSetters() {
        ElementaryFlowCnecResult elementaryFlowCnecResult = new ElementaryFlowCnecResult();

        // Per unit
        elementaryFlowCnecResult.setMargin(101, Unit.MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(102, Unit.MEGAWATT);
        elementaryFlowCnecResult.setMargin(201, Unit.AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(202, Unit.AMPERE);
        assertEquals(101, elementaryFlowCnecResult.getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(102, elementaryFlowCnecResult.getRelativeMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(201, elementaryFlowCnecResult.getMargin(Unit.AMPERE), 1e-3);
        assertEquals(202, elementaryFlowCnecResult.getRelativeMargin(Unit.AMPERE), 1e-3);

        // Per side
        elementaryFlowCnecResult.setPtdfZonalSum(TwoSides.ONE, 1);
        elementaryFlowCnecResult.setPtdfZonalSum(TwoSides.TWO, 2);
        assertEquals(1, elementaryFlowCnecResult.getPtdfZonalSum(TwoSides.ONE), 1e-3);
        assertEquals(2, elementaryFlowCnecResult.getPtdfZonalSum(TwoSides.TWO), 1e-3);

        // Per unit per side
        elementaryFlowCnecResult.setFlow(TwoSides.ONE, 100, Unit.MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(TwoSides.ONE, 103, Unit.MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 104, Unit.MEGAWATT);
        elementaryFlowCnecResult.setFlow(TwoSides.TWO, 105, Unit.MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(TwoSides.TWO, 106, Unit.MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(TwoSides.TWO, 107, Unit.MEGAWATT);
        assertEquals(100, elementaryFlowCnecResult.getFlow(TwoSides.ONE, Unit.MEGAWATT), 1e-3);
        assertEquals(103, elementaryFlowCnecResult.getCommercialFlow(TwoSides.ONE, Unit.MEGAWATT), 1e-3);
        assertEquals(104, elementaryFlowCnecResult.getLoopFlow(TwoSides.ONE, Unit.MEGAWATT), 1e-3);
        assertEquals(105, elementaryFlowCnecResult.getFlow(TwoSides.TWO, Unit.MEGAWATT), 1e-3);
        assertEquals(106, elementaryFlowCnecResult.getCommercialFlow(TwoSides.TWO, Unit.MEGAWATT), 1e-3);
        assertEquals(107, elementaryFlowCnecResult.getLoopFlow(TwoSides.TWO, Unit.MEGAWATT), 1e-3);

        elementaryFlowCnecResult.setFlow(TwoSides.ONE, 200, Unit.AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(TwoSides.ONE, 203, Unit.AMPERE);
        elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 204, Unit.AMPERE);
        elementaryFlowCnecResult.setFlow(TwoSides.TWO, 205, Unit.AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(TwoSides.TWO, 206, Unit.AMPERE);
        elementaryFlowCnecResult.setLoopFlow(TwoSides.TWO, 207, Unit.AMPERE);
        assertEquals(200, elementaryFlowCnecResult.getFlow(TwoSides.ONE, Unit.AMPERE), 1e-3);
        assertEquals(203, elementaryFlowCnecResult.getCommercialFlow(TwoSides.ONE, Unit.AMPERE), 1e-3);
        assertEquals(204, elementaryFlowCnecResult.getLoopFlow(TwoSides.ONE, Unit.AMPERE), 1e-3);
        assertEquals(205, elementaryFlowCnecResult.getFlow(TwoSides.TWO, Unit.AMPERE), 1e-3);
        assertEquals(206, elementaryFlowCnecResult.getCommercialFlow(TwoSides.TWO, Unit.AMPERE), 1e-3);
        assertEquals(207, elementaryFlowCnecResult.getLoopFlow(TwoSides.TWO, Unit.AMPERE), 1e-3);
    }

    @Test
    void notAFlowUnitTest() {
        ElementaryFlowCnecResult elementaryFlowCnecResult = new ElementaryFlowCnecResult();
        assertThrows(OpenRaoException.class, () -> elementaryFlowCnecResult.setLoopFlow(TwoSides.TWO, 100, Unit.KILOVOLT));
    }
}
