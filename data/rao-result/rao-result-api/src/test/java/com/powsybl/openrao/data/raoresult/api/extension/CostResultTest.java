/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.openrao.data.crac.api.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CostResultTest {

    private CostResult costResult;
    private Instant preventive;
    private Instant curative;

    @BeforeEach
    void setUp() {
        costResult = new CostResult();
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
        assertEquals("cost-results", costResult.getName());
    }

    @Test
    void testEmptyCostResult() {
        // Initial instant (null)
        assertTrue(Double.isNaN(costResult.getFunctionalCost(null)));
        assertTrue(Double.isNaN(costResult.getVirtualCost(null)));
        assertTrue(Double.isNaN(costResult.getCost(null)));
        assertTrue(costResult.getVirtualCostNames().isEmpty());

        // Mocked instants
        assertTrue(Double.isNaN(costResult.getFunctionalCost(preventive)));
        assertTrue(Double.isNaN(costResult.getVirtualCost(curative)));
    }

    @Test
    void testAddAndGetFunctionalCost() {
        costResult.addFunctionalCostResult(null, 100.0);
        costResult.addFunctionalCostResult(preventive, 200.0);
        costResult.addFunctionalCostResult(curative, 300.0);

        assertEquals(100.0, costResult.getFunctionalCost(null));
        assertEquals(200.0, costResult.getFunctionalCost(preventive));
        assertEquals(300.0, costResult.getFunctionalCost(curative));
    }

    @Test
    void testAddAndGetVirtualCost() {
        costResult.addVirtualCostResult(null, "v1", 10.0);
        costResult.addVirtualCostResult(null, "v2", 20.0);
        costResult.addVirtualCostResult(preventive, "v1", 50.0);
        costResult.addVirtualCostResult(curative, "v3", 100.0);

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
        costResult.addFunctionalCostResult(preventive, 200.0);
        costResult.addVirtualCostResult(preventive, "v1", 50.0);
        costResult.addVirtualCostResult(preventive, "v2", 25.0);

        assertEquals(275.0, costResult.getCost(preventive));
    }

    @Test
    void testSerialize() throws IOException {
        costResult.addFunctionalCostResult(null, 10.0);
        costResult.addVirtualCostResult(null, "v1", 1.0);
        costResult.addFunctionalCostResult(preventive, 20.0);
        costResult.addVirtualCostResult(preventive, "v2", 2.0);
        costResult.addFunctionalCostResult(curative, 30.0);
        costResult.addVirtualCostResult(curative, "v3", 3.0);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator gen = factory.createGenerator(writer)) {
            costResult.serialize(gen);
        }

        String expectedJson = "{\"initial\":"
            + "{\"functionalCost\":10.0,\"virtualCost\":{\"v1\":1.0}},"
            + "\"preventive\":"
            + "{\"functionalCost\":20.0,\"virtualCost\":{\"v2\":2.0}},"
            + "\"curative\":"
            + "{\"functionalCost\":30.0,\"virtualCost\":{\"v3\":3.0}}"
            + "}";
        assertEquals(expectedJson, writer.toString());
    }
}
