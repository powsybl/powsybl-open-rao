/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.data.raoresult.io.json.deserializers.TestUtils.parserFrom;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class VoltageCnecResultArrayDeserializerTest {

    @Test
    void deserializeSuccessWithMinMaxVoltage() throws Exception {
        String json = "[{\"voltageCnecId\":\"vc1\",\"initial\":{\"kilovolt\":{\"minVoltage\":380.5,\"maxVoltage\":400.2,\"margin\":10.0}}}]";
        Crac crac = mock(Crac.class);
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        when(crac.getVoltageCnec("vc1")).thenReturn(voltageCnec);
        when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(350.2));
        when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(410.2));

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> VoltageCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.6"));
        }

        VoltageResult voltageResult = raoResult.getExtension(VoltageResult.class);
        assertNotNull(voltageResult);
        assertEquals(380.5, voltageResult.getMinVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(400.2, voltageResult.getMaxVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(10.0, voltageResult.getMargin(null, voltageCnec, Unit.KILOVOLT));
    }

    @Test
    void deserializeThrowsWhenFirstFieldIsNotId() {
        String json = "[{\"notVoltageCnecId\":1}]";
        Crac crac = mock(Crac.class);
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> VoltageCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.6"));
            assertEquals("Cannot deserialize RaoResult: each voltageCnecResults must start with an voltageCnecId field", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWhenUnknownVoltageCnec() {
        String json = "[{\"voltageCnecId\":\"unknown\",\"initial\":{\"kilovolt\":{}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getVoltageCnec("unknown")).thenReturn(null);
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> VoltageCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.6"));
            assertEquals("Cannot deserialize RaoResult: voltageCnec with id unknown does not exist in the Crac", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "ampere, '[{\"voltageCnecId\":\"vc1\",\"initial\":{\"ampere\":{}}}]'",
        "unexpectedField, '[{\"voltageCnecId\":\"vc1\",\"initial\":{\"kilovolt\":{\"unexpectedField\":1.5}}}]'"
    })
    void deserializeThrowsOnUnexpectedField(String fieldName, String json) {
        Crac crac = mock(Crac.class);
        when(crac.getVoltageCnec("vc1")).thenReturn(mock(VoltageCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> VoltageCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.6"));
            assertEquals(String.format("Cannot deserialize RaoResult: unexpected field in voltageCnecResults (%s)", fieldName), ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWhenVoltageFieldUsedAfterVersion15() {
        // Since version 1.6, voltage field is deprecated and should throw
        String json = "[{\"voltageCnecId\":\"vc1\",\"initial\":{\"kilovolt\":{\"voltage\":390.0}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getVoltageCnec("vc1")).thenReturn(mock(VoltageCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> VoltageCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.6"));
            assertEquals("Since RaoResult version 1.6, voltage values are divided into min and max.", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeSuccessWithVoltageFieldInVersion15() throws Exception {
        // Before version 1.6, voltage field was allowed
        String json = "[{\"voltageCnecId\":\"vc1\",\"initial\":{\"kilovolt\":{\"voltage\":390.0}}}]";
        Crac crac = mock(Crac.class);
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        when(crac.getVoltageCnec("vc1")).thenReturn(voltageCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> VoltageCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.5"));
        }

        VoltageResult voltageResult = raoResult.getExtension(VoltageResult.class);
        assertNotNull(voltageResult);
        assertEquals(390.0, voltageResult.getMinVoltage(null, voltageCnec, Unit.KILOVOLT));
        assertEquals(390.0, voltageResult.getMaxVoltage(null, voltageCnec, Unit.KILOVOLT));
    }
}
