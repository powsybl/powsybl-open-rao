/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.extension;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class JsonCostResultTest {
    @Test
    void testSerialize() throws IOException {
        Crac crac = Mockito.mock(Crac.class);

        Instant preventive = Mockito.mock(Instant.class);
        Mockito.when(preventive.getId()).thenReturn("preventive");
        Mockito.when(preventive.isOutage()).thenReturn(false);

        Instant curative = Mockito.mock(Instant.class);
        Mockito.when(curative.getId()).thenReturn("curative");
        Mockito.when(curative.isOutage()).thenReturn(false);

        Mockito.when(crac.getSortedInstants()).thenReturn(List.of(preventive, curative));

        SerializerProvider serializerProvider = Mockito.mock(SerializerProvider.class);
        when(serializerProvider.getAttribute("crac")).thenReturn(crac);

        CostResult costResult = CostResult.build()
            .addFunctionalCostResult(null, 10.0)
            .addVirtualCostResult(null, "v1", 1.0)
            .addFunctionalCostResult(preventive, 20.0)
            .addVirtualCostResult(preventive, "v2", 2.0)
            .addFunctionalCostResult(curative, 30.0)
            .addVirtualCostResult(curative, "v3", 3.0)
            .build();

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonCostResult().serialize(costResult, jsonGenerator, serializerProvider);
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
