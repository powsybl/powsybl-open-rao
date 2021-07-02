/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowCnecResultTest {

    @Test
    public void defaultValuesTest() {
        FlowCnecResult defaultFlowCnecResult = new FlowCnecResult();
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.INITIAL).getCommercialFlow(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.AFTER_PRA).getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.AFTER_PRA).getRelativeMargin(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.AFTER_CRA).getLoopFlow(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.AFTER_CRA).getPtdfZonalSum(), 1e-3);
    }

    @Test
    public void testGetAndCreateIfAbsent() {

        FlowCnecResult flowCnecResult = new FlowCnecResult();
        assertEquals(Double.NaN, flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Unit.MEGAWATT));

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.INITIAL).setFlow(100., Unit.MEGAWATT);
        assertEquals(100., flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Unit.MEGAWATT), 1e-3);

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.INITIAL).setMargin(150., Unit.MEGAWATT);
        assertEquals(100., flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Unit.MEGAWATT), 1e-3);
    }
}
