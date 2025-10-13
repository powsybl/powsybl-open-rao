/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CostResultTest {

    @Test
    void defaultValuesTest() {
        CostResult costResult = new CostResult();

        assertEquals(Double.NaN, costResult.getCost(), 1e-3);
        assertEquals(Double.NaN, costResult.getFunctionalCost(), 1e-3);
        assertEquals(Double.NaN, costResult.getVirtualCost(), 1e-3);
        assertEquals(Double.NaN, costResult.getVirtualCost("unknownCost"), 1e-3);
        assertTrue(costResult.getVirtualCostNames().isEmpty());
    }

    @Test
    void setterAndGettersTest() {
        CostResult costResult = new CostResult();

        costResult.setFunctionalCost(100);
        costResult.setVirtualCost("loopflow-penalisation-cost", 0);
        costResult.setVirtualCost("fallback-penalisation-cost", 35);
        costResult.setVirtualCost("mnec-penalisation-cost", 55);

        assertEquals(190, costResult.getCost(), 1e-3);
        assertEquals(100, costResult.getFunctionalCost(), 1e-3);
        assertEquals(90, costResult.getVirtualCost(), 1e-3);
        assertEquals(Double.NaN, costResult.getVirtualCost("unknownCost"), 1e-3);
        assertEquals(0, costResult.getVirtualCost("loopflow-penalisation-cost"), 1e-3);
        assertEquals(35, costResult.getVirtualCost("fallback-penalisation-cost"), 1e-3);
        assertEquals(55, costResult.getVirtualCost("mnec-penalisation-cost"), 1e-3);

        assertEquals(3, costResult.getVirtualCostNames().size());
    }

    @Test
    void getCostWithOneCostDefinedTest() {
        CostResult costResult = new CostResult();

        costResult.setFunctionalCost(99);
        assertEquals(99, costResult.getCost(), 1e-3);

        costResult = new CostResult();
        costResult.setVirtualCost("anyCost", 101);
        assertEquals(101, costResult.getCost(), 1e-3);
    }
}
