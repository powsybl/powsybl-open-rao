/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ElementaryFlowCnecResultTest {

    @Test
    public void defaultValuesTest() {
        ElementaryFlowCnecResult elementaryFlowCnecResult = new ElementaryFlowCnecResult();

        assertEquals(Double.NaN, elementaryFlowCnecResult.getFlow(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getRelativeMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getCommercialFlow(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getLoopFlow(Unit.MEGAWATT), 1e-3);

        assertEquals(Double.NaN, elementaryFlowCnecResult.getFlow(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getMargin(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getRelativeMargin(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getCommercialFlow(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, elementaryFlowCnecResult.getLoopFlow(Unit.AMPERE), 1e-3);

        assertEquals(Double.NaN, elementaryFlowCnecResult.getPtdfZonalSum(), 1e-3);
    }

    @Test
    public void getterAndSetters() {
        ElementaryFlowCnecResult elementaryFlowCnecResult = new ElementaryFlowCnecResult();

        elementaryFlowCnecResult.setFlow(100, Unit.MEGAWATT);
        elementaryFlowCnecResult.setMargin(101, Unit.MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(102, Unit.MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(103, Unit.MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(104, Unit.MEGAWATT);

        elementaryFlowCnecResult.setFlow(200, Unit.AMPERE);
        elementaryFlowCnecResult.setMargin(201, Unit.AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(202, Unit.AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(203, Unit.AMPERE);
        elementaryFlowCnecResult.setLoopFlow(204, Unit.AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(1);

        assertEquals(100, elementaryFlowCnecResult.getFlow(Unit.MEGAWATT), 1e-3);
        assertEquals(101, elementaryFlowCnecResult.getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(102, elementaryFlowCnecResult.getRelativeMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(103, elementaryFlowCnecResult.getCommercialFlow(Unit.MEGAWATT), 1e-3);
        assertEquals(104, elementaryFlowCnecResult.getLoopFlow(Unit.MEGAWATT), 1e-3);

        assertEquals(200, elementaryFlowCnecResult.getFlow(Unit.AMPERE), 1e-3);
        assertEquals(201, elementaryFlowCnecResult.getMargin(Unit.AMPERE), 1e-3);
        assertEquals(202, elementaryFlowCnecResult.getRelativeMargin(Unit.AMPERE), 1e-3);
        assertEquals(203, elementaryFlowCnecResult.getCommercialFlow(Unit.AMPERE), 1e-3);
        assertEquals(204, elementaryFlowCnecResult.getLoopFlow(Unit.AMPERE), 1e-3);

        assertEquals(1, elementaryFlowCnecResult.getPtdfZonalSum(), 1e-3);
    }

    @Test (expected = FaraoException.class)
    public void notAFlowUnitTest() {
        ElementaryFlowCnecResult elementaryFlowCnecResult = new ElementaryFlowCnecResult();
        elementaryFlowCnecResult.setLoopFlow(100, Unit.KILOVOLT);
    }
}
