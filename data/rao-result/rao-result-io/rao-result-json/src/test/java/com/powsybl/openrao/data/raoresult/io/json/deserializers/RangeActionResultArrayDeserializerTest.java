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
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RangeActionResultArrayDeserializerTest {

    private static JsonParser parserFrom(String json) throws IOException {
        JsonParser p = new JsonFactory().createParser(json);
        // Position the parser on the first token (START_ARRAY for our payloads)
        p.nextToken();
        return p;
    }

    @Test
    void deserializeSuccessWithInitialSetpointOnly() throws Exception {
        // Version 1.7 accepts initialSetpoint for all range actions
        String json = "[{\"rangeActionId\":\"ra1\",\"initialSetpoint\":10.5}]";
        Crac crac = mock(Crac.class);
        RangeAction<?> rangeAction = mock(RangeAction.class);
        doReturn(rangeAction).when(crac).getRangeAction("ra1");
        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> RangeActionResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.7"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentRangeActionResult(rangeAction);
        verifyNoMoreInteractions(raoResult);
        assertEquals(10.5, raoResult.getPreOptimizationSetPointOnState(null, rangeAction));
    }

    @Test
    void deserializeThrowsWhenFirstFieldIsNotId() {
        String json = "[{\"notRangeActionId\":1}]";
        Crac crac = mock(Crac.class);
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> RangeActionResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.7"));
            assertEquals("Cannot deserialize RaoResult: each rangeActionResults must start with an rangeActionId field", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWhenUnknownRangeAction() {
        String json = "[{\"rangeActionId\":\"unknown\",\"statesActivated\":[]}]";
        Crac crac = mock(Crac.class);
        doReturn(null).when(crac).getRangeAction("unknown");
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> RangeActionResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.7"));
            assertEquals("Cannot deserialize RaoResult: cannot deserialize RaoResult: RangeAction with id unknown does not exist in the Crac", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsOnUnexpectedTopLevelField() {
        String json = "[{\"rangeActionId\":\"ra1\",\"unexpected\":true}]";
        Crac crac = mock(Crac.class);
        RangeAction<?> rangeAction = mock(RangeAction.class);
        doReturn(rangeAction).when(crac).getRangeAction("ra1");
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> RangeActionResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.7"));
            assertEquals("Cannot deserialize RaoResult: unexpected field in rangeActionResults (unexpected)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsForPstInitialSetpointSince18() {
        // Since 1.8, PST actions must use taps, not initialSetpoint
        String json = "[{\"rangeActionId\":\"pst1\",\"initialSetpoint\":5.0}]";
        Crac crac = mock(Crac.class);
        PstRangeAction pst = mock(PstRangeAction.class);
        doReturn(pst).when(crac).getRangeAction("pst1");
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> RangeActionResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.8"));
            assertEquals("Since version 1.8, only the initial taps are reported for PST range actions.", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsOnTapForNonPst() {
        // With version >= 1.8, taps must be PST-only
        String json = "[{\"rangeActionId\":\"ra1\",\"activatedStates\":[{\"instant\":\"preventive\",\"tap\":3}]}]";
        Crac crac = mock(Crac.class);
        RangeAction<?> rangeAction = mock(RangeAction.class);
        doReturn(rangeAction).when(crac).getRangeAction("ra1");
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> RangeActionResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.8"));
            assertEquals("Taps can only be defined for PST range actions.", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWhenSetpointMissingInState() {
        // For version 1.7 (pre-tap enforcement), a state without setpoint should fail
        String json = "[{\"rangeActionId\":\"ra1\",\"activatedStates\":[{\"instant\":\"preventive\"}]}]";
        Crac crac = mock(Crac.class);
        RangeAction<?> rangeAction = mock(RangeAction.class);
        doReturn(rangeAction).when(crac).getRangeAction("ra1");
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> RangeActionResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.7"));
            assertEquals("Cannot deserialize RaoResult: setpoint is required in rangeActionResults", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsOnInitialTapForNonPstSince18() {
        // Cover top-level INITIAL_TAP else-branch error for non-PST at version >= 1.8
        String json = "[{\"rangeActionId\":\"ra1\",\"initialTap\":3}]";
        Crac crac = mock(Crac.class);
        RangeAction<?> rangeAction = mock(RangeAction.class);
        doReturn(rangeAction).when(crac).getRangeAction("ra1");
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> RangeActionResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.8"));
            assertEquals("Initial taps can only be defined for PST range actions.", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }
}
