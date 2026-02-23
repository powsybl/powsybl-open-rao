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
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NetworkActionResultArrayDeserializerTest {

    private static JsonParser parserFrom(String json) throws IOException {
        JsonParser p = new JsonFactory().createParser(json);
        p.nextToken();
        return p;
    }

    @Test
    void deserializeWithEmptyStatesCallsGetAndCreate() {
        String json = "[{\"networkActionId\":\"na1\",\"activatedStates\":[]}]";
        Crac crac = mock(Crac.class);
        NetworkAction networkAction = mock(NetworkAction.class);
        when(crac.getNetworkAction("na1")).thenReturn(networkAction);
        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));
        try (JsonParser parser = parserFrom(json)) {
            NetworkActionResultArrayDeserializer.deserialize(parser, raoResult, crac);
            verify(raoResult, atLeastOnce()).getAndCreateIfAbsentNetworkActionResult(networkAction);
            verifyNoMoreInteractions(raoResult);
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWhenFirstFieldIsNotId() {
        String json = "[{\"statesActivated\":[]}]";
        Crac crac = mock(Crac.class);
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException exception = assertThrows(OpenRaoException.class,
                () -> NetworkActionResultArrayDeserializer.deserialize(parser, raoResult, crac));
            assertEquals("Cannot deserialize RaoResult: each networkActionResults must start with an networkActionId field", exception.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWhenUnknownNetworkAction() {
        String json = "[{\"networkActionId\":\"unknownId\",\"statesActivated\":[]}]";
        Crac crac = mock(Crac.class);
        when(crac.getNetworkAction("unknown")).thenReturn(null);
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException exception = assertThrows(OpenRaoException.class,
                () -> NetworkActionResultArrayDeserializer.deserialize(parser, raoResult, crac));
            assertEquals("Cannot deserialize RaoResult: cannot deserialize RaoResult: networkAction with id unknownId does not exist in the Crac", exception.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsOnUnexpectedField() {
        String json = "[{\"networkActionId\":\"na1\",\"unexpectedField\":true}]";
        Crac crac = mock(Crac.class);
        when(crac.getNetworkAction("na1")).thenReturn(mock(NetworkAction.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException exception = assertThrows(OpenRaoException.class,
                () -> NetworkActionResultArrayDeserializer.deserialize(parser, raoResult, crac));
            assertEquals("Cannot deserialize RaoResult: unexpected field in networkActionResults (unexpectedField)", exception.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }
}
