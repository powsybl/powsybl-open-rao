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
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class JsonMetadataTest {
    @Test
    void testSerialize() throws IOException {
        Crac crac = Mockito.mock(Crac.class);

        SerializerProvider serializerProvider = mock(SerializerProvider.class);
        when(serializerProvider.getAttribute("crac")).thenReturn(crac);

        Instant instant1 = Mockito.mock(Instant.class);
        when(instant1.getId()).thenReturn("i1");
        State state1 = Mockito.mock(State.class);
        when(state1.getInstant()).thenReturn(instant1);
        when(state1.getContingency()).thenReturn(Optional.empty());
        when(state1.getTimestamp()).thenReturn(Optional.empty());
        when(state1.compareTo(Mockito.any())).thenAnswer(invocation -> {
            State other = invocation.getArgument(0);
            return instant1.getOrder() - other.getInstant().getOrder();
        });

        Instant instant2 = Mockito.mock(Instant.class);
        when(instant2.getId()).thenReturn("i2");
        Contingency contingency2 = Mockito.mock(Contingency.class);
        when(contingency2.getId()).thenReturn("c2");
        State state2 = Mockito.mock(State.class);
        when(state2.getInstant()).thenReturn(instant2);
        when(state2.getContingency()).thenReturn(Optional.of(contingency2));
        when(state2.getTimestamp()).thenReturn(Optional.empty());
        when(state2.compareTo(Mockito.any())).thenAnswer(invocation -> {
            State other = invocation.getArgument(0);
            return instant2.getOrder() - other.getInstant().getOrder();
        });

        Instant instant3 = Mockito.mock(Instant.class);
        when(instant3.getId()).thenReturn("i3");
        OffsetDateTime timestamp3 = OffsetDateTime.of(2026, 8, 18, 10, 0, 0, 0, ZoneOffset.UTC);
        State state3 = Mockito.mock(State.class);
        when(state3.getInstant()).thenReturn(instant3);
        when(state3.getContingency()).thenReturn(Optional.empty());
        when(state3.getTimestamp()).thenReturn(Optional.of(timestamp3));
        when(state3.compareTo(Mockito.any())).thenAnswer(invocation -> {
            State other = invocation.getArgument(0);
            return instant3.getOrder() - other.getInstant().getOrder();
        });

        Instant instant4 = Mockito.mock(Instant.class);
        when(instant4.getId()).thenReturn("i4");
        Contingency contingency4 = Mockito.mock(Contingency.class);
        when(contingency4.getId()).thenReturn("c4");
        OffsetDateTime timestamp4 = OffsetDateTime.of(2026, 8, 18, 11, 0, 0, 0, ZoneOffset.UTC);
        State state4 = Mockito.mock(State.class);
        when(state4.getInstant()).thenReturn(instant4);
        when(state4.getContingency()).thenReturn(Optional.of(contingency4));
        when(state4.getTimestamp()).thenReturn(Optional.of(timestamp4));
        when(state4.compareTo(Mockito.any())).thenAnswer(invocation -> {
            State other = invocation.getArgument(0);
            return instant4.getOrder() - other.getInstant().getOrder();
        });

        // Configure comparison for sorting
        when(instant1.getOrder()).thenReturn(1);
        when(instant2.getOrder()).thenReturn(2);
        when(instant3.getOrder()).thenReturn(3);
        when(instant4.getOrder()).thenReturn(4);

        when(crac.getStates()).thenReturn(Set.of(state1, state2, state3, state4));

        Metadata metadata = Metadata.build()
            .addComputationStatus(state1, ComputationStatus.DEFAULT)
            .addComputationStatus(state2, ComputationStatus.FAILURE)
            .addComputationStatus(state3, ComputationStatus.PARTIAL_FAILURE)
            .addComputationStatus(state4, ComputationStatus.DEFAULT)
            .addExecutionDetails("Some details")
            .build();

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonMetadata().serialize(metadata, jsonGenerator, serializerProvider);
        }

        String expectedJson = "{"
            + "\"computationStatus\":\"partial-failure\","
            + "\"executionDetails\":\"Some details\","
            + "\"computationStatusMap\":["
            + "{\"computationStatus\":\"failure\",\"instant\":\"i2\",\"contingency\":\"c2\"},"
            + "{\"computationStatus\":\"partial-failure\",\"instant\":\"i3\",\"timestamp\":\"2026-08-18T10:00:00Z\"}"
            + "]"
            + "}";
        assertEquals(expectedJson, writer.toString());
    }
}
