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
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class JsonVoltageResultTest {
    private Crac crac;
    private VoltageCnec voltageCnec;
    private State state;
    private VoltageResult.VoltageResultBuilder voltageResultBuilder;
    private SerializerProvider serializerProvider;

    @BeforeEach
    void setUp() {
        crac = mock(Crac.class);
        voltageCnec = mock(VoltageCnec.class);
        state = mock(State.class);
        when(voltageCnec.getId()).thenReturn("cnec1");
        when(crac.getVoltageCnecs()).thenReturn(Set.of(voltageCnec));
        voltageResultBuilder = VoltageResult.build();
        serializerProvider = mock(SerializerProvider.class);
        when(serializerProvider.getAttribute("crac")).thenReturn(crac);
    }

    @Test
    void testSerialize() throws IOException {
        Mockito.when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(440.0));
        Mockito.when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getId()).thenReturn("preventive");
        Mockito.when(preventiveInstant.isOutage()).thenReturn(false);
        Mockito.when(preventiveInstant.compareTo(Mockito.any())).thenReturn(-1);

        Mockito.when(state.getInstant()).thenReturn(preventiveInstant);
        Mockito.when(voltageCnec.getState()).thenReturn(state);

        Mockito.when(crac.getSortedInstants()).thenReturn(List.of(preventiveInstant));

        voltageResultBuilder.addMeasurement(400.0, 405.0, null, voltageCnec, Unit.KILOVOLT)
            .addMeasurement(417.0, 437.0, preventiveInstant, voltageCnec, Unit.KILOVOLT);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonVoltageResult().serialize(voltageResultBuilder.build(), jsonGenerator, serializerProvider);
        }

        String expectedJson = "[{\"voltageCnecId\":\"cnec1\","
            + "\"initial\":{\"kilovolt\":{\"minVoltage\":400.0,\"maxVoltage\":405.0}},"
            + "\"preventive\":{\"kilovolt\":{\"minVoltage\":417.0,\"maxVoltage\":437.0}}}]";
        assertEquals(expectedJson, writer.toString());
    }

    @Test
    void testSerializeEmpty() throws IOException {
        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonVoltageResult().serialize(voltageResultBuilder.build(), jsonGenerator, serializerProvider);
        }
        assertEquals("[]", writer.toString());
    }

    @Test
    void testSerializeMultipleCnecs() throws IOException {
        VoltageCnec cnec1 = Mockito.mock(VoltageCnec.class);
        Mockito.when(cnec1.getId()).thenReturn("cnec1");
        Mockito.when(cnec1.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(440.0));
        Mockito.when(cnec1.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.0));

        VoltageCnec cnec2 = Mockito.mock(VoltageCnec.class);
        Mockito.when(cnec2.getId()).thenReturn("cnec2");
        Mockito.when(cnec2.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(245.0));
        Mockito.when(cnec2.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(210.0));

        Mockito.when(crac.getVoltageCnecs()).thenReturn(Set.of(cnec1, cnec2));

        voltageResultBuilder.addMeasurement(215.0, 220.0, null, cnec2, Unit.KILOVOLT)
            .addMeasurement(400.0, 405.0, null, cnec1, Unit.KILOVOLT);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonVoltageResult().serialize(voltageResultBuilder.build(), jsonGenerator, serializerProvider);
        }

        // Should be sorted by ID: cnec1 then cnec2
        String expectedJson = "[{\"voltageCnecId\":\"cnec1\","
            + "\"initial\":{\"kilovolt\":{\"minVoltage\":400.0,\"maxVoltage\":405.0}}},"
            + "{\"voltageCnecId\":\"cnec2\","
            + "\"initial\":{\"kilovolt\":{\"minVoltage\":215.0,\"maxVoltage\":220.0}}}]";
        assertEquals(expectedJson, writer.toString());
    }

    @Test
    void testSerializeMultipleInstants() throws IOException {
        Mockito.when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(440.0));
        Mockito.when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.0));

        Instant instant1 = Mockito.mock(Instant.class);
        Mockito.when(instant1.getId()).thenReturn("instant1");
        Mockito.when(instant1.getOrder()).thenReturn(0);
        Instant instant2 = Mockito.mock(Instant.class);
        Mockito.when(instant2.getId()).thenReturn("instant2");
        Mockito.when(instant2.getOrder()).thenReturn(1);

        Mockito.when(crac.getSortedInstants()).thenReturn(List.of(instant1, instant2));
        Mockito.when(state.getInstant()).thenReturn(instant2);
        Mockito.when(voltageCnec.getState()).thenReturn(state);

        voltageResultBuilder.addMeasurement(415.0, 425.0, instant2, voltageCnec, Unit.KILOVOLT)
            .addMeasurement(405.0, 410.0, instant1, voltageCnec, Unit.KILOVOLT)
            .addMeasurement(400.0, 405.0, null, voltageCnec, Unit.KILOVOLT);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonVoltageResult().serialize(voltageResultBuilder.build(), jsonGenerator, serializerProvider);
        }

        // Instants should be sorted: initial, then instant1, then instant2
        String expectedJson = "[{\"voltageCnecId\":\"cnec1\","
            + "\"initial\":{\"kilovolt\":{\"minVoltage\":400.0,\"maxVoltage\":405.0}},"
            + "\"instant1\":{\"kilovolt\":{\"minVoltage\":405.0,\"maxVoltage\":410.0}},"
            + "\"instant2\":{\"kilovolt\":{\"minVoltage\":415.0,\"maxVoltage\":425.0}}}]";
        assertEquals(expectedJson, writer.toString());
    }
}
