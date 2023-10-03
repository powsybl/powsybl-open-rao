/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class FlowCnecResultTest {

    @Test
    void defaultValuesTest() {
        FlowCnecResult defaultFlowCnecResult = new FlowCnecResult();
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(null).getCommercialFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(null).getFlow(Side.RIGHT, Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(Instant.PREVENTIVE).getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(Instant.PREVENTIVE).getRelativeMargin(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(Instant.CURATIVE).getLoopFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(Instant.CURATIVE).getPtdfZonalSum(Side.RIGHT), 1e-3);
    }

    @Test
    void testGetAndCreateIfAbsent() {
        FlowCnecResult flowCnecResult = new FlowCnecResult();
        assertEquals(Double.NaN, flowCnecResult.getResult(null).getFlow(Side.LEFT, Unit.MEGAWATT));
        assertEquals(Double.NaN, flowCnecResult.getResult(null).getFlow(Side.RIGHT, Unit.MEGAWATT));

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(null).setFlow(Side.LEFT, 100., Unit.MEGAWATT);
        assertEquals(100., flowCnecResult.getResult(null).getFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, flowCnecResult.getResult(null).getFlow(Side.RIGHT, Unit.MEGAWATT));

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(null).setMargin(150., Unit.MEGAWATT);
        assertEquals(100., flowCnecResult.getResult(null).getFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, flowCnecResult.getResult(null).getFlow(Side.RIGHT, Unit.MEGAWATT));
        assertEquals(150., flowCnecResult.getResult(null).getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, flowCnecResult.getResult(null).getMargin(Unit.AMPERE), 1e-3);
    }
}
