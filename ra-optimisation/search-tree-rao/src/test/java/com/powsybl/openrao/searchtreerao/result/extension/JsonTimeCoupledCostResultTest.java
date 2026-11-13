/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.extension;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonTimeCoupledCostResultTest {
    private static final OffsetDateTime TIMESTAMP_1 = OffsetDateTime.parse("2026-01-01T00:00Z");
    private static final OffsetDateTime TIMESTAMP_2 = OffsetDateTime.parse("2026-01-01T01:00Z");
    private static final String JSON = "{"
        + "\"2026-01-01T00:00:00Z\":{\"initial\":{\"functionalCost\":100.0,\"virtualCost\":{\"loopflow\":5.0,\"mnec\":10.0}}},"
        + "\"2026-01-01T01:00:00Z\":{\"initial\":{\"functionalCost\":200.0,\"virtualCost\":{\"sensi-failure\":1000.0}}}"
        + "}";

    private Crac crac;

    @BeforeEach
    void setUp() {
        crac = mock(Crac.class);
        when(crac.getSortedInstants()).thenReturn(List.of());
    }

    private String serialize(TimeCoupledCostResult result) throws IOException {
        SerializerProvider serializerProvider = mock(SerializerProvider.class);
        when(serializerProvider.getAttribute("crac")).thenReturn(crac);
        StringWriter writer = new StringWriter();
        try (JsonGenerator jsonGenerator = new JsonFactory().createGenerator(writer)) {
            new JsonTimeCoupledCostResult().serialize(result, jsonGenerator, serializerProvider);
        }
        return writer.toString();
    }

    private TimeCoupledCostResult deserialize(String json) throws IOException {
        DeserializationContext context = mock(DeserializationContext.class);
        when(context.getAttribute("crac")).thenReturn(crac);
        try (JsonParser parser = new JsonFactory().createParser(json)) {
            parser.nextToken();
            return new JsonTimeCoupledCostResult().deserialize(parser, context);
        }
    }

    @Test
    void testMetadata() {
        JsonTimeCoupledCostResult serializer = new JsonTimeCoupledCostResult();
        assertEquals("time-coupled-cost-results", serializer.getExtensionName());
        assertEquals("rao-result", serializer.getCategoryName());
        assertEquals(TimeCoupledCostResult.class, serializer.getExtensionClass());
    }

    @Test
    void testSerialize() throws IOException {
        TimeCoupledCostResult result = TimeCoupledCostResult.builder()
            .addCostResult(TIMESTAMP_2, CostResult.builder()
                .addFunctionalCostResult(null, 200.0)
                .addVirtualCostResult(null, "sensi-failure", 1000.0)
                .build())
            .addCostResult(TIMESTAMP_1, CostResult.builder()
                .addFunctionalCostResult(null, 100.0)
                .addVirtualCostResult(null, "mnec", 10.0)
                .addVirtualCostResult(null, "loopflow", 5.0)
                .build())
            .build();
        assertEquals(JSON, serialize(result));
    }

    @Test
    void testSerializeEmpty() throws IOException {
        assertEquals("{}", serialize(TimeCoupledCostResult.builder().build()));
    }

    @Test
    void testDeserialize() throws IOException {
        TimeCoupledCostResult result = deserialize(JSON);
        assertEquals(List.of(TIMESTAMP_1, TIMESTAMP_2), result.getTimestamps());
        assertEquals(100.0, result.getFunctionalCost(null, TIMESTAMP_1));
        assertEquals(15.0, result.getVirtualCost(null, TIMESTAMP_1));
        assertEquals(10.0, result.getVirtualCost(null, "mnec", TIMESTAMP_1));
        assertEquals(5.0, result.getVirtualCost(null, "loopflow", TIMESTAMP_1));
        assertEquals(1200.0, result.getCost(null, TIMESTAMP_2));
        assertEquals(Set.of("mnec", "loopflow", "sensi-failure"), result.getVirtualCostNames());
    }

    @Test
    void testDeserializeEmpty() throws IOException {
        assertTrue(deserialize("{}").getTimestamps().isEmpty());
    }

    @Test
    void testRoundTrip() throws IOException {
        assertEquals(JSON, serialize(deserialize(JSON)));
    }
}
