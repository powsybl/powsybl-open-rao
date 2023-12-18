/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.rao_result_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
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

    private void assertFlowsAreNan(ElementaryFlowCnecResult elementaryFlowCnecResult, Unit unit, Side side) {
        assertEquals(Double.NaN, elementaryFlowCnecResult.getFlow(side, unit), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getCommercialFlow(side, unit), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getLoopFlow(side, unit), 1e-3);
    }

    @Test
    void defaultValuesTest() {
        ElementaryFlowCnecResult elementaryFlowCnecResult = new ElementaryFlowCnecResult();

        assertMarginsAreNan(elementaryFlowCnecResult, Unit.MEGAWATT);
        assertMarginsAreNan(elementaryFlowCnecResult, Unit.AMPERE);

        assertFlowsAreNan(elementaryFlowCnecResult, Unit.MEGAWATT, Side.LEFT);
        assertFlowsAreNan(elementaryFlowCnecResult, Unit.MEGAWATT, Side.RIGHT);
        assertFlowsAreNan(elementaryFlowCnecResult, Unit.AMPERE, Side.LEFT);
        assertFlowsAreNan(elementaryFlowCnecResult, Unit.AMPERE, Side.RIGHT);

        assertEquals(Double.NaN, elementaryFlowCnecResult.getPtdfZonalSum(Side.LEFT), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getPtdfZonalSum(Side.RIGHT), 1e-3);
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
        elementaryFlowCnecResult.setPtdfZonalSum(Side.LEFT, 1);
        elementaryFlowCnecResult.setPtdfZonalSum(Side.RIGHT, 2);
        assertEquals(1, elementaryFlowCnecResult.getPtdfZonalSum(Side.LEFT), 1e-3);
        assertEquals(2, elementaryFlowCnecResult.getPtdfZonalSum(Side.RIGHT), 1e-3);

        // Per unit per side
        elementaryFlowCnecResult.setFlow(Side.LEFT, 100, Unit.MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(Side.LEFT, 103, Unit.MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(Side.LEFT, 104, Unit.MEGAWATT);
        elementaryFlowCnecResult.setFlow(Side.RIGHT, 105, Unit.MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(Side.RIGHT, 106, Unit.MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(Side.RIGHT, 107, Unit.MEGAWATT);
        assertEquals(100, elementaryFlowCnecResult.getFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(103, elementaryFlowCnecResult.getCommercialFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(104, elementaryFlowCnecResult.getLoopFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(105, elementaryFlowCnecResult.getFlow(Side.RIGHT, Unit.MEGAWATT), 1e-3);
        assertEquals(106, elementaryFlowCnecResult.getCommercialFlow(Side.RIGHT, Unit.MEGAWATT), 1e-3);
        assertEquals(107, elementaryFlowCnecResult.getLoopFlow(Side.RIGHT, Unit.MEGAWATT), 1e-3);

        elementaryFlowCnecResult.setFlow(Side.LEFT, 200, Unit.AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(Side.LEFT, 203, Unit.AMPERE);
        elementaryFlowCnecResult.setLoopFlow(Side.LEFT, 204, Unit.AMPERE);
        elementaryFlowCnecResult.setFlow(Side.RIGHT, 205, Unit.AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(Side.RIGHT, 206, Unit.AMPERE);
        elementaryFlowCnecResult.setLoopFlow(Side.RIGHT, 207, Unit.AMPERE);
        assertEquals(200, elementaryFlowCnecResult.getFlow(Side.LEFT, Unit.AMPERE), 1e-3);
        assertEquals(203, elementaryFlowCnecResult.getCommercialFlow(Side.LEFT, Unit.AMPERE), 1e-3);
        assertEquals(204, elementaryFlowCnecResult.getLoopFlow(Side.LEFT, Unit.AMPERE), 1e-3);
        assertEquals(205, elementaryFlowCnecResult.getFlow(Side.RIGHT, Unit.AMPERE), 1e-3);
        assertEquals(206, elementaryFlowCnecResult.getCommercialFlow(Side.RIGHT, Unit.AMPERE), 1e-3);
        assertEquals(207, elementaryFlowCnecResult.getLoopFlow(Side.RIGHT, Unit.AMPERE), 1e-3);
    }

    @Test
    void notAFlowUnitTest() {
        ElementaryFlowCnecResult elementaryFlowCnecResult = new ElementaryFlowCnecResult();
        assertThrows(FaraoException.class, () -> elementaryFlowCnecResult.setLoopFlow(Side.RIGHT, 100, Unit.KILOVOLT));
    }
}
