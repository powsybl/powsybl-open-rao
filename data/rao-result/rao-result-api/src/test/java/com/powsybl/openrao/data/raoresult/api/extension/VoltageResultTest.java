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
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
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
public class VoltageResultTest {
    @Test
    void testVoltageExtension() {
        VoltageCnec voltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(440.0));
        Mockito.when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getOrder()).thenReturn(0);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.getOrder()).thenReturn(1);

        VoltageResult voltageResult = new VoltageResult();
        assertEquals("voltage-results", voltageResult.getName());

        // initial results

        assertEquals(Double.NaN, voltageResult.getMinVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, voltageResult.getMaxVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, voltageResult.getMargin(null, voltageCnec, Unit.KILOVOLT));

        assertEquals(Double.NaN, voltageResult.getMinVoltage(preventiveInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, voltageResult.getMaxVoltage(preventiveInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, voltageResult.getMargin(preventiveInstant, voltageCnec, Unit.KILOVOLT));

        assertEquals(Double.NaN, voltageResult.getMinVoltage(curativeInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, voltageResult.getMaxVoltage(curativeInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(Double.NaN, voltageResult.getMargin(curativeInstant, voltageCnec, Unit.KILOVOLT));

        // manually add results

        voltageResult.addMeasurement(400.0, 405.0, null, voltageCnec, Unit.KILOVOLT);
        assertEquals(400.0, voltageResult.getMinVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(405.0, voltageResult.getMaxVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(20.0, voltageResult.getMargin(null, voltageCnec, Unit.KILOVOLT));

        voltageResult.addMeasurement(417.0, 437.0, preventiveInstant, voltageCnec, Unit.KILOVOLT);
        assertEquals(417.0, voltageResult.getMinVoltage(preventiveInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(437.0, voltageResult.getMaxVoltage(preventiveInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(3.0, voltageResult.getMargin(preventiveInstant, voltageCnec, Unit.KILOVOLT));

        voltageResult.addMeasurement(370.0, 461.0, curativeInstant, voltageCnec, Unit.KILOVOLT);
        assertEquals(370.0, voltageResult.getMinVoltage(curativeInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(461.0, voltageResult.getMaxVoltage(curativeInstant, voltageCnec, Unit.KILOVOLT));
        assertEquals(-21.0, voltageResult.getMargin(curativeInstant, voltageCnec, Unit.KILOVOLT));

        // invalid unit

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> voltageResult.addMeasurement(410.0, 420.0, null, voltageCnec, Unit.MEGAWATT));
        assertEquals("VoltageCNEC results are only allowed for kilovolts.", exception.getMessage());
    }

    @Test
    void testSerialize() throws IOException {
        VoltageCnec voltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(voltageCnec.getId()).thenReturn("cnec1");
        Mockito.when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(440.0));
        Mockito.when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getId()).thenReturn("preventive");
        Mockito.when(preventiveInstant.compareTo(Mockito.any())).thenReturn(-1);

        VoltageResult voltageResult = new VoltageResult();
        voltageResult.addMeasurement(400.0, 405.0, null, voltageCnec, Unit.KILOVOLT);
        voltageResult.addMeasurement(417.0, 437.0, preventiveInstant, voltageCnec, Unit.KILOVOLT);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            voltageResult.serialize(jsonGenerator);
        }

        String expectedJson = "[{\"voltageCnecId\":\"cnec1\",\"measurements\":{\"kilovolt\":[{\"instant\":\"initial\",\"minVoltage\":400.0,\"maxVoltage\":405.0,\"margin\":20.0},{\"instant\":\"preventive\",\"minVoltage\":417.0,\"maxVoltage\":437.0,\"margin\":3.0}]}}]";
        assertEquals(expectedJson, writer.toString());
    }

    @Test
    void testSerializeEmpty() throws IOException {
        VoltageResult voltageResult = new VoltageResult();
        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            voltageResult.serialize(jsonGenerator);
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

        VoltageResult voltageResult = new VoltageResult();
        voltageResult.addMeasurement(215.0, 220.0, null, cnec2, Unit.KILOVOLT);
        voltageResult.addMeasurement(400.0, 405.0, null, cnec1, Unit.KILOVOLT);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            voltageResult.serialize(jsonGenerator);
        }

        // Should be sorted by ID: cnec1 then cnec2
        String expectedJson = "["
                + "{\"voltageCnecId\":\"cnec1\",\"measurements\":{\"kilovolt\":[{\"instant\":\"initial\",\"minVoltage\":400.0,\"maxVoltage\":405.0,\"margin\":20.0}]}},"
                + "{\"voltageCnecId\":\"cnec2\",\"measurements\":{\"kilovolt\":[{\"instant\":\"initial\",\"minVoltage\":215.0,\"maxVoltage\":220.0,\"margin\":5.0}]}}"
                + "]";
        assertEquals(expectedJson, writer.toString());
    }

    @Test
    void testSerializeMultipleInstants() throws IOException {
        VoltageCnec voltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(voltageCnec.getId()).thenReturn("cnec1");
        Mockito.when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(440.0));
        Mockito.when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.0));

        Instant instant1 = Mockito.mock(Instant.class);
        Mockito.when(instant1.getId()).thenReturn("instant1");
        Instant instant2 = Mockito.mock(Instant.class);
        Mockito.when(instant2.getId()).thenReturn("instant2");

        // Mock compareTo for sorting
        Mockito.when(instant1.compareTo(instant2)).thenReturn(-1);
        Mockito.when(instant2.compareTo(instant1)).thenReturn(1);

        VoltageResult voltageResult = new VoltageResult();
        voltageResult.addMeasurement(415.0, 425.0, instant2, voltageCnec, Unit.KILOVOLT);
        voltageResult.addMeasurement(405.0, 410.0, instant1, voltageCnec, Unit.KILOVOLT);
        voltageResult.addMeasurement(400.0, 405.0, null, voltageCnec, Unit.KILOVOLT);

        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jsonGenerator = factory.createGenerator(writer)) {
            voltageResult.serialize(jsonGenerator);
        }

        // Instants should be sorted: initial, then instant1, then instant2
        String expectedJson = "[{\"voltageCnecId\":\"cnec1\",\"measurements\":{\"kilovolt\":["
                + "{\"instant\":\"initial\",\"minVoltage\":400.0,\"maxVoltage\":405.0,\"margin\":20.0},"
                + "{\"instant\":\"instant1\",\"minVoltage\":405.0,\"maxVoltage\":410.0,\"margin\":25.0},"
                + "{\"instant\":\"instant2\",\"minVoltage\":415.0,\"maxVoltage\":425.0,\"margin\":15.0}"
                + "]}}]";
        assertEquals(expectedJson, writer.toString());
    }
}
