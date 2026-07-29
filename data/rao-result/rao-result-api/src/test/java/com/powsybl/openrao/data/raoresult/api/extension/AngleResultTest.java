/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleResultTest {
    @Test
    void testAngleExtension() {
        AngleCnec angleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getOrder()).thenReturn(0);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.getOrder()).thenReturn(1);

        AngleResult angleResult = new AngleResult();
        assertEquals("angle-results", angleResult.getName());

        // initial results

        assertEquals(Double.NaN, angleResult.getAngle(null, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, angleResult.getMargin(null, angleCnec, Unit.DEGREE));

        assertEquals(Double.NaN, angleResult.getAngle(preventiveInstant, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, angleResult.getMargin(preventiveInstant, angleCnec, Unit.DEGREE));

        assertEquals(Double.NaN, angleResult.getAngle(curativeInstant, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, angleResult.getMargin(curativeInstant, angleCnec, Unit.DEGREE));

        // manually add results

        angleResult.addMeasurement(25.0, null, angleCnec, Unit.DEGREE);
        assertEquals(25.0, angleResult.getAngle(null, angleCnec, Unit.DEGREE));
        assertEquals(5.0, angleResult.getMargin(null, angleCnec, Unit.DEGREE));

        angleResult.addMeasurement(17.0, preventiveInstant, angleCnec, Unit.DEGREE);
        assertEquals(17.0, angleResult.getAngle(preventiveInstant, angleCnec, Unit.DEGREE));
        assertEquals(13.0, angleResult.getMargin(preventiveInstant, angleCnec, Unit.DEGREE));

        angleResult.addMeasurement(-5.0, curativeInstant, angleCnec, Unit.DEGREE);
        assertEquals(-5.0, angleResult.getAngle(curativeInstant, angleCnec, Unit.DEGREE));
        assertEquals(-5.0, angleResult.getMargin(curativeInstant, angleCnec, Unit.DEGREE));

        // invalid unit

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> angleResult.addMeasurement(25.0, null, angleCnec, Unit.MEGAWATT));
        assertEquals("AngleCNEC results are only allowed for degrees.", exception.getMessage());
    }

    @Test
    void testSerialize() throws IOException {
        AngleCnec angleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(angleCnec.getId()).thenReturn("cnec1");
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getId()).thenReturn("preventive");
        Mockito.when(preventiveInstant.compareTo(Mockito.any())).thenReturn(-1);

        AngleResult angleResult = new AngleResult();
        angleResult.addMeasurement(25.0, null, angleCnec, Unit.DEGREE);
        angleResult.addMeasurement(17.0, preventiveInstant, angleCnec, Unit.DEGREE);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            angleResult.serialize(jsonGenerator);
        }

        String expectedJson = "[{\"angleCnecId\":\"cnec1\",\"measurements\":{\"degree\":[{\"instant\":\"initial\",\"angle\":25.0,\"margin\":5.0},{\"instant\":\"preventive\",\"angle\":17.0,\"margin\":13.0}]}}]";
        assertEquals(expectedJson, writer.toString());
    }

    @Test
    void testSerializeEmpty() throws IOException {
        AngleResult angleResult = new AngleResult();
        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            angleResult.serialize(jsonGenerator);
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

        AngleResult angleResult = new AngleResult();
        angleResult.addMeasurement(10.0, null, cnec2, Unit.DEGREE);
        angleResult.addMeasurement(25.0, null, cnec1, Unit.DEGREE);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            angleResult.serialize(jsonGenerator);
        }

        // Should be sorted by ID: cnec1 then cnec2
        String expectedJson = "["
                + "{\"angleCnecId\":\"cnec1\",\"measurements\":{\"degree\":[{\"instant\":\"initial\",\"angle\":25.0,\"margin\":5.0}]}},"
                + "{\"angleCnecId\":\"cnec2\",\"measurements\":{\"degree\":[{\"instant\":\"initial\",\"angle\":10.0,\"margin\":35.0}]}}"
                + "]";
        assertEquals(expectedJson, writer.toString());
    }

    @Test
    void testSerializeMultipleInstants() throws IOException {
        AngleCnec angleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(angleCnec.getId()).thenReturn("cnec1");
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));

        Instant instant1 = Mockito.mock(Instant.class);
        Mockito.when(instant1.getId()).thenReturn("instant1");
        Instant instant2 = Mockito.mock(Instant.class);
        Mockito.when(instant2.getId()).thenReturn("instant2");

        // Mock compareTo for sorting
        Mockito.when(instant1.compareTo(instant2)).thenReturn(-1);
        Mockito.when(instant2.compareTo(instant1)).thenReturn(1);

        AngleResult angleResult = new AngleResult();
        angleResult.addMeasurement(15.0, instant2, angleCnec, Unit.DEGREE);
        angleResult.addMeasurement(10.0, instant1, angleCnec, Unit.DEGREE);
        angleResult.addMeasurement(20.0, null, angleCnec, Unit.DEGREE);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            angleResult.serialize(jsonGenerator);
        }

        // Instants should be sorted: initial, then instant1, then instant2
        String expectedJson = "[{\"angleCnecId\":\"cnec1\",\"measurements\":{\"degree\":["
                + "{\"instant\":\"initial\",\"angle\":20.0,\"margin\":10.0},"
                + "{\"instant\":\"instant1\",\"angle\":10.0,\"margin\":10.0},"
                + "{\"instant\":\"instant2\",\"angle\":15.0,\"margin\":15.0}"
                + "]}}]";
        assertEquals(expectedJson, writer.toString());
    }
}
