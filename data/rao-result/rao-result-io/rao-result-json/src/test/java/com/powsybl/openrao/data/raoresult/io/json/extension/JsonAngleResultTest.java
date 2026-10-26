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
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.extension.AngleResult;
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
public class JsonAngleResultTest {
    private Crac crac;
    private AngleCnec angleCnec;
    private State state;
    private AngleResult.AngleResultBuilder angleResultBuilder;
    private SerializerProvider serializerProvider;

    @BeforeEach
    void setUp() {
        crac = mock(Crac.class);
        angleCnec = mock(AngleCnec.class);
        state = mock(State.class);
        when(angleCnec.getId()).thenReturn("cnec1");
        when(crac.getAngleCnecs()).thenReturn(Set.of(angleCnec));
        angleResultBuilder = AngleResult.build();
        serializerProvider = mock(SerializerProvider.class);
        when(serializerProvider.getAttribute("crac")).thenReturn(crac);
    }

    @Test
    void testSerialize() throws IOException {
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getId()).thenReturn("preventive");
        Mockito.when(preventiveInstant.isOutage()).thenReturn(false);
        Mockito.when(preventiveInstant.compareTo(Mockito.any())).thenReturn(-1);
        Mockito.when(crac.getSortedInstants()).thenReturn(List.of(preventiveInstant));

        Mockito.when(state.getInstant()).thenReturn(preventiveInstant);
        Mockito.when(angleCnec.getState()).thenReturn(state);

        AngleResult.AngleResultBuilder angleResultBuilder = AngleResult.build();
        angleResultBuilder.addMeasurement(25.0, null, angleCnec, Unit.DEGREE)
            .addMeasurement(17.0, preventiveInstant, angleCnec, Unit.DEGREE);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonAngleResult().serialize(angleResultBuilder.build(), jsonGenerator, serializerProvider);
        }

        String expectedJson = "[{"
            + "\"angleCnecId\":\"cnec1\","
            + "\"initial\":{\"degree\":{\"angle\":25.0}},"
            + "\"preventive\":{\"degree\":{\"angle\":17.0}}" +
            "}]";
        assertEquals(expectedJson, writer.toString());
    }

    @Test
    void testSerializeEmpty() throws IOException {
        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonAngleResult().serialize(angleResultBuilder.build(), jsonGenerator, serializerProvider);
        }
        assertEquals("[]", writer.toString());
    }

    @Test
    void testSerializeMultipleCnecs() throws IOException {
        AngleCnec cnec1 = Mockito.mock(AngleCnec.class);
        Mockito.when(cnec1.getId()).thenReturn("cnec1");
        Mockito.when(cnec1.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(cnec1.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));

        AngleCnec cnec2 = Mockito.mock(AngleCnec.class);
        Mockito.when(cnec2.getId()).thenReturn("cnec2");
        Mockito.when(cnec2.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(45.0));
        Mockito.when(cnec2.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(-45.0));

        Mockito.when(crac.getAngleCnecs()).thenReturn(Set.of(cnec1, cnec2));

        angleResultBuilder.addMeasurement(10.0, null, cnec2, Unit.DEGREE)
            .addMeasurement(25.0, null, cnec1, Unit.DEGREE);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonAngleResult().serialize(angleResultBuilder.build(), jsonGenerator, serializerProvider);
        }

        // Should be sorted by ID: cnec1 then cnec2
        String expectedJson = "["
            + "{\"angleCnecId\":\"cnec1\",\"initial\":{\"degree\":{\"angle\":25.0}}},"
            + "{\"angleCnecId\":\"cnec2\",\"initial\":{\"degree\":{\"angle\":10.0}}}"
            + "]";
        assertEquals(expectedJson, writer.toString());
    }

    @Test
    void testSerializeMultipleInstants() throws IOException {
        Mockito.when(angleCnec.getId()).thenReturn("cnec1");
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));

        Instant instant1 = Mockito.mock(Instant.class);
        Mockito.when(instant1.getId()).thenReturn("instant1");
        Mockito.when(instant1.getOrder()).thenReturn(0);
        Mockito.when(instant1.isOutage()).thenReturn(false);
        Instant instant2 = Mockito.mock(Instant.class);
        Mockito.when(instant2.getId()).thenReturn("instant2");
        Mockito.when(instant2.getOrder()).thenReturn(1);
        Mockito.when(instant2.isOutage()).thenReturn(false);

        Mockito.when(state.getInstant()).thenReturn(instant2);
        Mockito.when(angleCnec.getState()).thenReturn(state);
        Mockito.when(crac.getSortedInstants()).thenReturn(List.of(instant1, instant2));

        AngleResult.AngleResultBuilder angleResultBuilder = AngleResult.build();
        angleResultBuilder.addMeasurement(15.0, instant2, angleCnec, Unit.DEGREE)
            .addMeasurement(10.0, instant1, angleCnec, Unit.DEGREE)
            .addMeasurement(20.0, null, angleCnec, Unit.DEGREE);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            new JsonAngleResult().serialize(angleResultBuilder.build(), jsonGenerator, serializerProvider);
        }

        // Instants should be sorted: initial, then instant1, then instant2
        String expectedJson = "[{"
            + "\"angleCnecId\":\"cnec1\","
            + "\"initial\":{\"degree\":{\"angle\":20.0}},"
            + "\"instant1\":{\"degree\":{\"angle\":10.0}},"
            + "\"instant2\":{\"degree\":{\"angle\":15.0}}"
            + "}]";
        assertEquals(expectedJson, writer.toString());
    }
}
