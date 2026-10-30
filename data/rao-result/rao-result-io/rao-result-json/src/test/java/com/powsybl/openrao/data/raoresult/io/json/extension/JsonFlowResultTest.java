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
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class JsonFlowResultTest {
    private Crac crac;
    private FlowCnec flowCnec;
    private State state;
    private FlowResult.FlowResultBuilder flowResultBuilder;
    private SerializerProvider serializerProvider;

    @BeforeEach
    void setUp() {
        crac = mock(Crac.class);
        flowCnec = mock(FlowCnec.class);
        state = mock(State.class);
        when(flowCnec.getId()).thenReturn("cnec1");
        when(crac.getFlowCnecs()).thenReturn(Set.of(flowCnec));
        flowResultBuilder = FlowResult.build();
        serializerProvider = mock(SerializerProvider.class);
        when(serializerProvider.getAttribute("crac")).thenReturn(crac);
        when(serializerProvider.getAttribute("flowUnits")).thenReturn(Set.of(Unit.AMPERE, Unit.MEGAWATT));
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        private String serializeToJson() throws IOException {
            StringWriter writer = new StringWriter();
            JsonFactory factory = new JsonFactory();
            try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
                new JsonFlowResult().serialize(flowResultBuilder.build(), jsonGenerator, serializerProvider);
            }
            return writer.toString();
        }

        @Test
        @DisplayName("Serialize empty FlowResult produces empty array")
        void testSerializeEmpty() throws IOException {
            when(crac.getSortedInstants()).thenReturn(List.of());
            assertEquals("[]", serializeToJson());
        }

        @Test
        @DisplayName("Serialize Cnec with only commercial flow or PTDF (no flows) produces flowCnec empty array")
        void testSerializeCnecWithoutFlows() throws IOException {
            when(crac.getSortedInstants()).thenReturn(List.of());
            flowResultBuilder.addCommercialFlowMeasurement(100.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addPtdfZonalSumMeasurement(0.5, null, flowCnec, TwoSides.ONE);

            String json = serializeToJson();
            assertEquals("[]", json);
        }

        @Test
        @DisplayName("Serialize initial instant with flow only (MW)")
        void testSerializeInitialFlowOnly() throws IOException {
            when(crac.getSortedInstants()).thenReturn(List.of());
            flowResultBuilder.addFlowMeasurement(200.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);

            String expectedJson = "[{"
                + "\"flowCnecId\":\"cnec1\","
                + "\"initial\":{"
                + "\"megawatt\":{"
                + "\"side1\":{\"flow\":200.0}"
                + "}"
                + "}"
                + "}]";
            assertEquals(expectedJson, serializeToJson());
        }

        @Test
        @DisplayName("Serialize initial instant with all MW fields: flow, commercial flow, loop flow, zonalPtdfSum")
        void testSerializeInitialAllMegawattFields() throws IOException {
            when(crac.getSortedInstants()).thenReturn(List.of());
            flowResultBuilder.addFlowMeasurement(300.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addCommercialFlowMeasurement(100.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addPtdfZonalSumMeasurement(0.5, null, flowCnec, TwoSides.ONE);

            String expectedJson = "[{"
                + "\"flowCnecId\":\"cnec1\","
                + "\"initial\":{"
                + "\"megawatt\":{"
                + "\"side1\":{"
                + "\"flow\":300.0,"
                + "\"commercialFlow\":100.0,"
                + "\"zonalPtdfSum\":0.5"
                + "}"
                + "}"
                + "}"
                + "}]";
            assertEquals(expectedJson, serializeToJson());
        }

        @Test
        @DisplayName("Serialize AMPERE unit does not include zonalPtdfSum even if PTDF measurement is present")
        void testSerializeAmpereUnitDoesNotIncludeZonalPtdfSum() throws IOException {
            when(crac.getSortedInstants()).thenReturn(List.of());
            flowResultBuilder.addFlowMeasurement(500.0, null, flowCnec, TwoSides.TWO, Unit.AMPERE);
            flowResultBuilder.addCommercialFlowMeasurement(200.0, null, flowCnec, TwoSides.TWO, Unit.AMPERE);
            flowResultBuilder.addPtdfZonalSumMeasurement(0.4, null, flowCnec, TwoSides.TWO);

            String expectedJson = "[{"
                + "\"flowCnecId\":\"cnec1\","
                + "\"initial\":{"
                + "\"ampere\":{"
                + "\"side2\":{"
                + "\"flow\":500.0,"
                + "\"commercialFlow\":200.0"
                + "}"
                + "}"
                + "}"
                + "}]";
            assertEquals(expectedJson, serializeToJson());
        }

        @Test
        @DisplayName("Serialize with multiple sides (side1 and side2)")
        void testSerializeMultipleSides() throws IOException {
            when(crac.getSortedInstants()).thenReturn(List.of());
            flowResultBuilder.addFlowMeasurement(200.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addFlowMeasurement(250.0, null, flowCnec, TwoSides.TWO, Unit.MEGAWATT);

            String json = serializeToJson();

            // Verify structure includes flowCnecId, initial, megawatt, margin = 750.0, side1 flow = 200.0, side2 flow = 250.0
            assertTrue(json.contains("\"flowCnecId\":\"cnec1\""));
            assertTrue(json.contains("\"side1\":{\"flow\":200.0}"));
            assertTrue(json.contains("\"side2\":{\"flow\":250.0}"));
        }

        @Test
        @DisplayName("Serialize with multiple units (megawatt and ampere)")
        void testSerializeMultipleUnits() throws IOException {
            when(crac.getSortedInstants()).thenReturn(List.of());
            flowResultBuilder.addFlowMeasurement(200.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addFlowMeasurement(400.0, null, flowCnec, TwoSides.ONE, Unit.AMPERE);

            String json = serializeToJson();
            assertTrue(json.contains("\"megawatt\":{\"side1\":{\"flow\":200.0}}"));
            assertTrue(json.contains("\"ampere\":{\"side1\":{\"flow\":400.0}}"));
        }

        @Test
        @DisplayName("Serialize multiple FlowCnecs sorted alphabetically by ID")
        void testSerializeMultipleFlowCnecsSortedById() throws IOException {
            FlowCnec cnecB = mock(FlowCnec.class);
            when(cnecB.getId()).thenReturn("cnec-b");
            when(cnecB.getLowerBound(Mockito.any(), Mockito.any())).thenReturn(Optional.of(-1000.0));
            when(cnecB.getUpperBound(Mockito.any(), Mockito.any())).thenReturn(Optional.of(1000.0));

            FlowCnec cnecA = mock(FlowCnec.class);
            when(cnecA.getId()).thenReturn("cnec-a");
            when(cnecA.getLowerBound(Mockito.any(), Mockito.any())).thenReturn(Optional.of(-1000.0));
            when(cnecA.getUpperBound(Mockito.any(), Mockito.any())).thenReturn(Optional.of(1000.0));

            when(crac.getFlowCnecs()).thenReturn(Set.of(flowCnec, cnecA, cnecB));
            when(crac.getSortedInstants()).thenReturn(List.of());

            // Add cnecB first, then cnecA
            flowResultBuilder.addFlowMeasurement(200.0, null, cnecB, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addFlowMeasurement(100.0, null, cnecA, TwoSides.ONE, Unit.MEGAWATT);

            String expectedJson = "["
                + "{\"flowCnecId\":\"cnec-a\",\"initial\":{\"megawatt\":{\"side1\":{\"flow\":100.0}}}},"
                + "{\"flowCnecId\":\"cnec-b\",\"initial\":{\"megawatt\":{\"side1\":{\"flow\":200.0}}}}"
                + "]";
            assertEquals(expectedJson, serializeToJson());
        }

        @Test
        @DisplayName("Serialize multiple Instants sorted: initial, then sorted instants")
        void testSerializeMultipleInstantsSorted() throws IOException {
            Instant instant1 = mock(Instant.class);
            when(instant1.getId()).thenReturn("instant1");
            when(instant1.isOutage()).thenReturn(false);
            when(instant1.getOrder()).thenReturn(0);

            Instant instant2 = mock(Instant.class);
            when(instant2.getId()).thenReturn("instant2");
            when(instant2.isOutage()).thenReturn(false);
            when(instant2.getOrder()).thenReturn(1);

            Instant instant3 = mock(Instant.class);
            when(instant3.getId()).thenReturn("instant3");
            when(instant3.isOutage()).thenReturn(true);
            when(instant3.getOrder()).thenReturn(2);

            when(crac.getSortedInstants()).thenReturn(List.of(instant1, instant2, instant3));
            when(state.getInstant()).thenReturn(instant3);
            when(flowCnec.getState()).thenReturn(state);

            // Add in reverse order: instant3, instant2, instant1, then null (initial)
            flowResultBuilder.addFlowMeasurement(400.0, instant3, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addFlowMeasurement(300.0, instant2, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addFlowMeasurement(200.0, instant1, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResultBuilder.addFlowMeasurement(100.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);

            // instant3 is outage so it should not be serialized
            String expectedJson = "[{"
                + "\"flowCnecId\":\"cnec1\","
                + "\"initial\":{\"megawatt\":{\"side1\":{\"flow\":100.0}}},"
                + "\"instant1\":{\"megawatt\":{\"side1\":{\"flow\":200.0}}},"
                + "\"instant2\":{\"megawatt\":{\"side1\":{\"flow\":300.0}}}"
                + "}]";
            assertEquals(expectedJson, serializeToJson());
        }

        @Test
        @DisplayName("Serialize when only non-initial instant has flows")
        void testSerializeOnlyNonInitialInstantHasFlow() throws IOException {
            Instant preventiveInstant = mock(Instant.class);
            when(preventiveInstant.getId()).thenReturn("preventive");
            when(preventiveInstant.isOutage()).thenReturn(false);
            when(state.getInstant()).thenReturn(preventiveInstant);
            when(flowCnec.getState()).thenReturn(state);

            when(crac.getSortedInstants()).thenReturn(List.of(preventiveInstant));

            flowResultBuilder.addFlowMeasurement(300.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);

            // Initial has no flows, so only preventive instant appears
            String expectedJson = "[{"
                + "\"flowCnecId\":\"cnec1\","
                + "\"preventive\":{\"megawatt\":{\"side1\":{\"flow\":300.0}}}"
                + "}]";
            assertEquals(expectedJson, serializeToJson());
        }
    }
}
