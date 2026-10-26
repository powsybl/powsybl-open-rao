/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.openrao.data.crac.api.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CostResultTest {

    private CostResult costResult;
    private CostResult.CostResultBuilder costResultBuilder;
    private Instant preventive;
    private Instant curative;

    @BeforeEach
    void setUp() {
        costResultBuilder = CostResult.build();
        preventive = Mockito.mock(Instant.class);
        curative = Mockito.mock(Instant.class);

        when(preventive.getId()).thenReturn("preventive");
        when(preventive.getOrder()).thenReturn(1);
        when(preventive.compareTo(curative)).thenReturn(-1);
        when(preventive.compareTo(preventive)).thenReturn(0);

        when(curative.getId()).thenReturn("curative");
        when(curative.getOrder()).thenReturn(2);
        when(curative.compareTo(preventive)).thenReturn(1);
        when(curative.compareTo(curative)).thenReturn(0);
    }

    @Test
    void getName() {
        assertEquals("cost-results", costResultBuilder.build().getName());
    }

    @Test
    void testEmptyCostResult() {
        costResult = costResultBuilder.build();

        // Initial instant (null)
        assertTrue(Double.isNaN(costResult.getFunctionalCost(null)));
        assertEquals(0.0, costResult.getVirtualCost(null));
        assertTrue(Double.isNaN(costResult.getCost(null)));
        assertTrue(costResult.getVirtualCostNames().isEmpty());

        // Mocked instants
        assertTrue(Double.isNaN(costResult.getFunctionalCost(preventive)));
        assertEquals(0.0, costResult.getVirtualCost(curative));
    }

    @Test
    void testAddAndGetFunctionalCost() {
        costResultBuilder.addFunctionalCostResult(null, 100.0)
            .addFunctionalCostResult(preventive, 200.0)
            .addFunctionalCostResult(curative, 300.0);
        costResult = costResultBuilder.build();

        assertEquals(100.0, costResult.getFunctionalCost(null));
        assertEquals(200.0, costResult.getFunctionalCost(preventive));
        assertEquals(300.0, costResult.getFunctionalCost(curative));
    }

    @Test
    void testAddAndGetVirtualCost() {
        costResultBuilder.addVirtualCostResult(null, "v1", 10.0)
            .addVirtualCostResult(null, "v2", 20.0)
            .addVirtualCostResult(preventive, "v1", 50.0)
            .addVirtualCostResult(curative, "v3", 100.0);
        costResult = costResultBuilder.build();

        assertEquals(10.0, costResult.getVirtualCost(null, "v1"));
        assertEquals(20.0, costResult.getVirtualCost(null, "v2"));
        assertEquals(30.0, costResult.getVirtualCost(null)); // 10 + 20

        assertEquals(50.0, costResult.getVirtualCost(preventive, "v1"));
        assertTrue(Double.isNaN(costResult.getVirtualCost(preventive, "unknown")));
        assertEquals(50.0, costResult.getVirtualCost(preventive));

        assertEquals(100.0, costResult.getVirtualCost(curative, "v3"));
        assertEquals(100.0, costResult.getVirtualCost(curative));

        Set<String> names = costResult.getVirtualCostNames();
        assertEquals(3, names.size());
        assertTrue(names.containsAll(Set.of("v1", "v2", "v3")));
    }

    @Test
    void testGetCost() {
        costResultBuilder.addFunctionalCostResult(preventive, 200.0)
            .addVirtualCostResult(preventive, "v1", 50.0)
            .addVirtualCostResult(preventive, "v2", 25.0);
        costResult = costResultBuilder.build();

        assertEquals(275.0, costResult.getCost(preventive));
    }
}
