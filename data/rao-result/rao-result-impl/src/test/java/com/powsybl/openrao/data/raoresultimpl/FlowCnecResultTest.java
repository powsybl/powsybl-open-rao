/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresultimpl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class FlowCnecResultTest {

    @Test
    void defaultValuesTest() {
        Instant preventiveInstant = mock(Instant.class);
        Instant curativeInstant = mock(Instant.class);
        FlowCnecResult defaultFlowCnecResult = new FlowCnecResult();
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(null).getCommercialFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(null).getFlow(Side.RIGHT, Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(preventiveInstant).getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(preventiveInstant).getRelativeMargin(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(curativeInstant).getLoopFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(curativeInstant).getPtdfZonalSum(Side.RIGHT), 1e-3);
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
