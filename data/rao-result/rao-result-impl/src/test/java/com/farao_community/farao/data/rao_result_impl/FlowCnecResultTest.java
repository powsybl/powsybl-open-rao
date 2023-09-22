/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class FlowCnecResultTest {
    // TODO : fix these tests
/*
    @Test
    void defaultValuesTest() {
        FlowCnecResult defaultFlowCnecResult = new FlowCnecResult();
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.INITIAL).getCommercialFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Side.RIGHT, Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.AFTER_PRA).getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.AFTER_PRA).getRelativeMargin(Unit.AMPERE), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.AFTER_CRA).getLoopFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, defaultFlowCnecResult.getResult(OptimizationState.AFTER_CRA).getPtdfZonalSum(Side.RIGHT), 1e-3);
    }

    @Test
    void testGetAndCreateIfAbsent() {
        FlowCnecResult flowCnecResult = new FlowCnecResult();
        assertEquals(Double.NaN, flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Side.LEFT, Unit.MEGAWATT));
        assertEquals(Double.NaN, flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Side.RIGHT, Unit.MEGAWATT));

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.INITIAL).setFlow(Side.LEFT, 100., Unit.MEGAWATT);
        assertEquals(100., flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Side.RIGHT, Unit.MEGAWATT));

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.INITIAL).setMargin(150., Unit.MEGAWATT);
        assertEquals(100., flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Side.LEFT, Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, flowCnecResult.getResult(OptimizationState.INITIAL).getFlow(Side.RIGHT, Unit.MEGAWATT));
        assertEquals(150., flowCnecResult.getResult(OptimizationState.INITIAL).getMargin(Unit.MEGAWATT), 1e-3);
        assertEquals(Double.NaN, flowCnecResult.getResult(OptimizationState.INITIAL).getMargin(Unit.AMPERE), 1e-3);
    }

 */
}
