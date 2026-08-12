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
class CastorCostResultTest {

    private CastorCostResult castorCostResult;
    private Instant preventive;
    private Instant curative;

    @BeforeEach
    void setUp() {
        castorCostResult = new CastorCostResult();
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
        assertEquals("castor-cost-results", castorCostResult.getName());
    }

    @Test
    void testEmptyCostResult() {
        // Initial instant (null)
        assertTrue(Double.isNaN(castorCostResult.getFunctionalCost(null)));
        assertTrue(Double.isNaN(castorCostResult.getVirtualCost(null)));
        assertTrue(Double.isNaN(castorCostResult.getCost(null)));
        assertTrue(castorCostResult.getVirtualCostNames().isEmpty());

        // Mocked instants
        assertTrue(Double.isNaN(castorCostResult.getFunctionalCost(preventive)));
        assertTrue(Double.isNaN(castorCostResult.getVirtualCost(curative)));
    }

    @Test
    void testAddAndGetFunctionalCost() {
        castorCostResult.addFunctionalCostResult(null, 100.0);
        castorCostResult.addFunctionalCostResult(preventive, 200.0);
        castorCostResult.addFunctionalCostResult(curative, 300.0);

        assertEquals(100.0, castorCostResult.getFunctionalCost(null));
        assertEquals(200.0, castorCostResult.getFunctionalCost(preventive));
        assertEquals(300.0, castorCostResult.getFunctionalCost(curative));
    }

    @Test
    void testAddAndGetVirtualCost() {
        castorCostResult.addVirtualCostResult(null, "v1", 10.0);
        castorCostResult.addVirtualCostResult(null, "v2", 20.0);
        castorCostResult.addVirtualCostResult(preventive, "v1", 50.0);
        castorCostResult.addVirtualCostResult(curative, "v3", 100.0);

        assertEquals(10.0, castorCostResult.getVirtualCost(null, "v1"));
        assertEquals(20.0, castorCostResult.getVirtualCost(null, "v2"));
        assertEquals(30.0, castorCostResult.getVirtualCost(null)); // 10 + 20

        assertEquals(50.0, castorCostResult.getVirtualCost(preventive, "v1"));
        assertTrue(Double.isNaN(castorCostResult.getVirtualCost(preventive, "unknown")));
        assertEquals(50.0, castorCostResult.getVirtualCost(preventive));

        assertEquals(100.0, castorCostResult.getVirtualCost(curative, "v3"));
        assertEquals(100.0, castorCostResult.getVirtualCost(curative));

        Set<String> names = castorCostResult.getVirtualCostNames();
        assertEquals(3, names.size());
        assertTrue(names.containsAll(Set.of("v1", "v2", "v3")));
    }

    @Test
    void testGetCost() {
        castorCostResult.addFunctionalCostResult(preventive, 200.0);
        castorCostResult.addVirtualCostResult(preventive, "v1", 50.0);
        castorCostResult.addVirtualCostResult(preventive, "v2", 25.0);

        assertEquals(275.0, castorCostResult.getCost(preventive));
    }

    @Test
    void testSerialize() throws IOException {
        castorCostResult.addFunctionalCostResult(null, 10.0);
        castorCostResult.addVirtualCostResult(null, "v1", 1.0);
        castorCostResult.addFunctionalCostResult(preventive, 20.0);
        castorCostResult.addVirtualCostResult(preventive, "v2", 2.0);
        castorCostResult.addFunctionalCostResult(curative, 30.0);
        castorCostResult.addVirtualCostResult(curative, "v3", 3.0);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator gen = factory.createGenerator(writer)) {
            castorCostResult.serialize(gen);
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
