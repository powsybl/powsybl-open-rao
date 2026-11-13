/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.extension;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class TimeCoupledCostResultTest {
    private static final OffsetDateTime TIMESTAMP_1 = OffsetDateTime.parse("2026-01-01T00:00Z");
    private static final OffsetDateTime TIMESTAMP_2 = OffsetDateTime.parse("2026-01-01T01:00Z");
    private static final OffsetDateTime TIMESTAMP_3 = OffsetDateTime.parse("2026-01-01T02:00Z");

    private CostResult costResult1;
    private CostResult costResult2;
    private TimeCoupledCostResult result;

    @BeforeEach
    void setUp() {
        costResult1 = CostResult.builder()
            .addFunctionalCostResult(null, 100.0)
            .addVirtualCostResult(null, "mnec", 10.0)
            .addVirtualCostResult(null, "loopflow", 5.0)
            .build();
        costResult2 = CostResult.builder()
            .addFunctionalCostResult(null, 200.0)
            .addVirtualCostResult(null, "sensi-failure", 1000.0)
            .build();
        result = TimeCoupledCostResult.builder()
            .addCostResult(TIMESTAMP_2, costResult2)
            .addCostResult(TIMESTAMP_1, costResult1)
            .build();
    }

    @Test
    void testName() {
        assertEquals("time-coupled-cost-results", result.getName());
    }

    @Test
    void testTimestamps() {
        assertEquals(List.of(TIMESTAMP_1, TIMESTAMP_2), result.getTimestamps());
    }

    @Test
    void testGetCostResult() {
        assertSame(costResult1, result.getCostResult(TIMESTAMP_1));
        assertSame(costResult2, result.getCostResult(TIMESTAMP_2));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> result.getCostResult(TIMESTAMP_3));
        assertEquals("No cost result found for timestamp 2026-01-01T02:00:00Z", exception.getMessage());
    }

    @Test
    void testCosts() {
        assertEquals(115.0, result.getCost(null, TIMESTAMP_1));
        assertEquals(100.0, result.getFunctionalCost(null, TIMESTAMP_1));
        assertEquals(15.0, result.getVirtualCost(null, TIMESTAMP_1));
        assertEquals(10.0, result.getVirtualCost(null, "mnec", TIMESTAMP_1));
        assertEquals(5.0, result.getVirtualCost(null, "loopflow", TIMESTAMP_1));
        assertTrue(Double.isNaN(result.getVirtualCost(null, "sensi-failure", TIMESTAMP_1)));

        assertEquals(1200.0, result.getCost(null, TIMESTAMP_2));
        assertEquals(200.0, result.getFunctionalCost(null, TIMESTAMP_2));
        assertEquals(1000.0, result.getVirtualCost(null, TIMESTAMP_2));
        assertEquals(1000.0, result.getVirtualCost(null, "sensi-failure", TIMESTAMP_2));
    }

    @Test
    void testVirtualCostNames() {
        assertEquals(Set.of("mnec", "loopflow", "sensi-failure"), result.getVirtualCostNames());
        assertEquals(Set.of("mnec", "loopflow"), result.getVirtualCostNames(TIMESTAMP_1));
        assertEquals(Set.of("sensi-failure"), result.getVirtualCostNames(TIMESTAMP_2));
    }

    @Test
    void testEmpty() {
        TimeCoupledCostResult empty = TimeCoupledCostResult.builder().build();
        assertTrue(empty.getTimestamps().isEmpty());
        assertTrue(empty.getVirtualCostNames().isEmpty());
    }
}
