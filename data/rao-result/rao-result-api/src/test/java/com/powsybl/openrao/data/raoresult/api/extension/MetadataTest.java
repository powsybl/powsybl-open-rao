/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MetadataTest {

    private Metadata metadata;

    @BeforeEach
    void setUp() {
        metadata = new Metadata();
    }

    @Test
    void testConstructor() {
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());
        assertTrue(metadata.getExecutionDetails().isEmpty());
    }

    @Test
    void testGetName() {
        assertEquals("metadata", metadata.getName());
    }

    @Test
    void testGetComputationStatus() {
        // Case 1: Empty map -> DEFAULT
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());

        State preventive = Mockito.mock(State.class);
        when(preventive.isPreventive()).thenReturn(true);
        State curative = Mockito.mock(State.class);
        when(curative.isPreventive()).thenReturn(false);

        // Case 2: Only DEFAULT statuses -> DEFAULT
        metadata.setComputationStatus(preventive, ComputationStatus.DEFAULT);
        metadata.setComputationStatus(curative, ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());

        // Case 3: Only PARTIAL_FAILURE statuses -> DEFAULT
        // (The implementation currently only looks for ComputationStatus.FAILURE)
        metadata = new Metadata();
        metadata.setComputationStatus(preventive, ComputationStatus.PARTIAL_FAILURE);
        metadata.setComputationStatus(curative, ComputationStatus.PARTIAL_FAILURE);
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());

        // Case 4: One non-preventive FAILURE -> PARTIAL_FAILURE
        metadata = new Metadata();
        metadata.setComputationStatus(curative, ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.PARTIAL_FAILURE, metadata.getComputationStatus());

        // Case 5: One preventive FAILURE -> FAILURE
        metadata = new Metadata();
        metadata.setComputationStatus(preventive, ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, metadata.getComputationStatus());

        // Case 6: Mixed statuses (preventive FAILURE + others) -> FAILURE
        metadata.setComputationStatus(curative, ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, metadata.getComputationStatus());

        // Case 7: Mixed statuses (non-preventive FAILURE + DEFAULT) -> PARTIAL_FAILURE
        metadata = new Metadata();
        metadata.setComputationStatus(preventive, ComputationStatus.DEFAULT);
        metadata.setComputationStatus(curative, ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.PARTIAL_FAILURE, metadata.getComputationStatus());
    }

    @Test
    void testSetAndGetComputationStatusPerState() {
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);

        // Default value for unknown state
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus(state1));

        metadata.setComputationStatus(state1, ComputationStatus.FAILURE);
        metadata.setComputationStatus(state2, ComputationStatus.PARTIAL_FAILURE);

        assertEquals(ComputationStatus.FAILURE, metadata.getComputationStatus(state1));
        assertEquals(ComputationStatus.PARTIAL_FAILURE, metadata.getComputationStatus(state2));

        // Unknown state still returns DEFAULT
        State state3 = Mockito.mock(State.class);
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus(state3));
    }

    @Test
    void testSetAndGetExecutionDetails() {
        String details = "Step 1: Success\nStep 2: Warning";
        metadata.setExecutionDetails(details);
        assertEquals(Optional.of(details), metadata.getExecutionDetails());

        metadata.setExecutionDetails(null);
        assertTrue(metadata.getExecutionDetails().isEmpty());
    }

    @Test
    void testSerialize() throws IOException {
        metadata.setExecutionDetails("Some details");

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

        metadata.setComputationStatus(state1, ComputationStatus.DEFAULT);
        metadata.setComputationStatus(state2, ComputationStatus.FAILURE);
        metadata.setComputationStatus(state3, ComputationStatus.PARTIAL_FAILURE);
        metadata.setComputationStatus(state4, ComputationStatus.DEFAULT);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator gen = factory.createGenerator(writer)) {
            metadata.serialize(gen);
        }

        String expectedJson = "{"
            + "\"computationStatus\":\"partial-failure\","
            + "\"executionDetails\":\"Some details\","
            + "\"computationStatusMap\":["
            + "{\"computationStatus\":\"default\",\"instant\":\"i1\"},"
            + "{\"computationStatus\":\"failure\",\"instant\":\"i2\",\"contingency\":\"c2\"},"
            + "{\"computationStatus\":\"partial-failure\",\"instant\":\"i3\",\"timestamp\":\"2026-08-18T10:00:00Z\"},"
            + "{\"computationStatus\":\"default\",\"instant\":\"i4\",\"contingency\":\"c4\",\"timestamp\":\"2026-08-18T11:00:00Z\"}"
            + "]"
            + "}";
        assertEquals(expectedJson, writer.toString());
    }
}
