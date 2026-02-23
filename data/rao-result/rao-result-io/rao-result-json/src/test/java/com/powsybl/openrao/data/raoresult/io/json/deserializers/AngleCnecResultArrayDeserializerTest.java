/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class AngleCnecResultArrayDeserializerTest {

    private static JsonParser parserFrom(String json) throws IOException {
        JsonParser p = new JsonFactory().createParser(json);
        p.nextToken();
        return p;
    }

    @Test
    void deserializeSuccessWithInitialStateAndDegreeValues() throws Exception {
        String json = "[{\"angleCnecId\":\"ac1\",\"initial\":{\"degree\":{\"angle\":12.3,\"margin\":-1.1}}}]";
        Crac crac = mock(Crac.class);
        AngleCnec angleCnec = mock(AngleCnec.class);
        when(crac.getAngleCnec("ac1")).thenReturn(angleCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> AngleCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.3"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentAngleCnecResult(angleCnec);
        verifyNoMoreInteractions(raoResult);
        assertEquals(12.3, raoResult.getAngle(null, angleCnec, Unit.DEGREE));
        assertEquals(-1.1, raoResult.getMargin(null, angleCnec, Unit.DEGREE));
    }

    @Test
    void deserializeThrowsWhenFirstFieldIsNotId() {
        String json = "[{\"notAngleCnecId\":1}]";
        Crac crac = mock(Crac.class);
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> AngleCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.3"));
            assertEquals("Cannot deserialize RaoResult: each angleCnecResults must start with an angleCnecId field", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWhenUnknownAngleCnec() {
        String json = "[{\"angleCnecId\":\"unknown\",\"initial\":{\"degree\":{}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getAngleCnec("unknown")).thenReturn(null);
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> AngleCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.3"));
            assertEquals("Cannot deserialize RaoResult: angleCnec with id unknown does not exist in the Crac", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "radian, '[{\"angleCnecId\":\"ac1\",\"initial\":{\"radian\":{}}}]'",
        "foo, '[{\"angleCnecId\":\"ac1\",\"initial\":{\"degree\":{\"foo\":1}}}]'"
    })
    void deserializeThrowsOnUnexpectedField(String fieldName, String json) {
        Crac crac = mock(Crac.class);
        when(crac.getAngleCnec("ac1")).thenReturn(mock(AngleCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> AngleCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.3"));
            assertEquals(String.format("Cannot deserialize RaoResult: unexpected field in angleCnecResults (%s)", fieldName), ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }
}
