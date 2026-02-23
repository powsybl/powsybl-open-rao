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
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
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
class FlowCnecResultArrayDeserializerTest {

    private static JsonParser parserFrom(String json) throws IOException {
        JsonParser p = new JsonFactory().createParser(json);
        p.nextToken();
        return p;
    }

    @Test
    void deserializeSuccessWithSideOneAndSideTwo() throws Exception {
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"margin\":100.0,\"side1\":{\"flow\":500.0,\"commercialFlow\":50.0},\"side2\":{\"flow\":490.0}}}}]";
        Crac crac = mock(Crac.class);
        FlowCnec flowCnec = mock(FlowCnec.class);
        when(crac.getFlowCnec("fc1")).thenReturn(flowCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.4"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentFlowCnecResult(flowCnec);
        verifyNoMoreInteractions(raoResult);
    }

    @Test
    void deserializeThrowsWhenFirstFieldIsNotId() {
        String json = "[{\"notFlowCnecId\":1}]";
        Crac crac = mock(Crac.class);
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.4"));
            assertEquals("Cannot deserialize RaoResult: each flowCnecResults must start with an flowCnecId field", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWhenUnknownFlowCnec() {
        String json = "[{\"flowCnecId\":\"unknown\",\"initial\":{\"megawatt\":{}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("unknown")).thenReturn(null);
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.4"));
            assertEquals("Cannot deserialize RaoResult: flowCnec with id unknown does not exist in the Crac", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsOnUnexpectedUnitInsideElementary() {
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"kilovolt\":{}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.4"));
            assertEquals("Cannot deserialize RaoResult: unexpected field in flowCnecResults (kilovolt)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsOnUnexpectedFieldInsideUnit() {
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"unexpectedField\":1.5}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.4"));
            assertEquals("Cannot deserialize RaoResult: unexpected field in flowCnecResults (unexpectedField)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsOnUnexpectedFieldInsideSide() {
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"side1\":{\"unexpectedField\":100.0}}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.4"));
            assertEquals("Cannot deserialize RaoResult: unexpected field in flowCnecResults (unexpectedField)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeSuccessWithDeprecatedLeftRightSideInVersion13() throws Exception {
        // Before version 1.4, leftSide and rightSide were used
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"leftSide\":{\"flow\":500.0},\"rightSide\":{\"flow\":490.0}}}}]";
        Crac crac = mock(Crac.class);
        FlowCnec flowCnec = mock(FlowCnec.class);
        when(crac.getFlowCnec("fc1")).thenReturn(flowCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.3"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentFlowCnecResult(flowCnec);
        verifyNoMoreInteractions(raoResult);
    }

    @Test
    void deserializeThrowsWithDeprecatedLeftSideAfterVersion13() {
        // Since version 1.4, leftSide is deprecated
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"leftSide\":{\"flow\":500.0}}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.5"));
            assertEquals("Cannot deserialize RaoResult: field leftSide in flowCnecResults in not supported in file version 1.5 (last supported in version 1.4)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeThrowsWithDeprecatedRightSideAfterVersion13() {
        // Since version 1.4, rightSide is deprecated
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"rightSide\":{\"flow\":490.0}}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.5"));
            assertEquals("Cannot deserialize RaoResult: field rightSide in flowCnecResults in not supported in file version 1.5 (last supported in version 1.4)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeSuccessWithDeprecatedFlowFieldInVersion10() throws Exception {
        // Before version 1.1, flow field was at unit level
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"flow\":500.0}}}]";
        Crac crac = mock(Crac.class);
        FlowCnec flowCnec = mock(FlowCnec.class);
        when(crac.getFlowCnec("fc1")).thenReturn(flowCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.0"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentFlowCnecResult(flowCnec);
        verifyNoMoreInteractions(raoResult);
    }

    @Test
    void deserializeThrowsWithDeprecatedFlowFieldAfterVersion10() {
        // Since version 1.1, flow at unit level is deprecated
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"flow\":500.0}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.2"));
            assertEquals("Cannot deserialize RaoResult: field flow in flowCnecResults in not supported in file version 1.2 (last supported in version 1.1)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeSuccessWithDeprecatedCommercialFlowFieldInVersion10() throws Exception {
        // Before version 1.1, commercialFlow field was at unit level
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"commercialFlow\":50.0}}}]";
        Crac crac = mock(Crac.class);
        FlowCnec flowCnec = mock(FlowCnec.class);
        when(crac.getFlowCnec("fc1")).thenReturn(flowCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.0"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentFlowCnecResult(flowCnec);
        verifyNoMoreInteractions(raoResult);
    }

    @Test
    void deserializeThrowsWithDeprecatedCommercialFlowFieldAfterVersion10() {
        // Since version 1.1, commercialFlow at unit level is deprecated
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"commercialFlow\":50.0}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.2"));
            assertEquals("Cannot deserialize RaoResult: field commercialFlow in flowCnecResults in not supported in file version 1.2 (last supported in version 1.1)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeSuccessWithDeprecatedLoopFlowFieldInVersion10() throws Exception {
        // Before version 1.1, loopFlow field was at unit level
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"loopFlow\":30.0}}}]";
        Crac crac = mock(Crac.class);
        FlowCnec flowCnec = mock(FlowCnec.class);
        when(crac.getFlowCnec("fc1")).thenReturn(flowCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.0"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentFlowCnecResult(flowCnec);
        verifyNoMoreInteractions(raoResult);
    }

    @Test
    void deserializeThrowsWithDeprecatedLoopFlowFieldAfterVersion10() {
        // Since version 1.1, loopFlow at unit level is deprecated
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"loopFlow\":30.0}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.2"));
            assertEquals("Cannot deserialize RaoResult: field loopFlow in flowCnecResults in not supported in file version 1.2 (last supported in version 1.1)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeSuccessWithDeprecatedZonalPtdfSumFieldInVersion10() throws Exception {
        // Before version 1.1, zonalPtdfSum field was at unit level
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"zonalPtdfSum\":0.85}}]";
        Crac crac = mock(Crac.class);
        FlowCnec flowCnec = mock(FlowCnec.class);
        when(crac.getFlowCnec("fc1")).thenReturn(flowCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.0"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentFlowCnecResult(flowCnec);
        verifyNoMoreInteractions(raoResult);
    }

    @Test
    void deserializeThrowsWithDeprecatedZonalPtdfSumFieldAfterVersion10() {
        // Since version 1.1, zonalPtdfSum at elementary level is deprecated
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"zonalPtdfSum\":0.85}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.2"));
            assertEquals("Cannot deserialize RaoResult: field zonalPtdfSum in flowCnecResults in not supported in file version 1.2 (last supported in version 1.1)", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }

    @Test
    void deserializeSuccessWithZonalPtdfSumInSideAndMegawatt() throws Exception {
        // zonalPtdfSum inside side and megawatt is allowed
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"megawatt\":{\"side1\":{\"zonalPtdfSum\":0.85}}}}]";
        Crac crac = mock(Crac.class);
        FlowCnec flowCnec = mock(FlowCnec.class);
        when(crac.getFlowCnec("fc1")).thenReturn(flowCnec);

        RaoResultImpl raoResult = spy(new RaoResultImpl(crac));

        try (JsonParser parser = parserFrom(json)) {
            assertDoesNotThrow(() -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.4"));
        }

        verify(raoResult, atLeastOnce()).getAndCreateIfAbsentFlowCnecResult(flowCnec);
        verifyNoMoreInteractions(raoResult);
    }

    @Test
    void deserializeThrowsWithZonalPtdfSumInAmpereUnit() {
        // zonalPtdfSum can only be in MEGAWATT section
        String json = "[{\"flowCnecId\":\"fc1\",\"initial\":{\"ampere\":{\"side1\":{\"zonalPtdfSum\":0.85}}}}]";
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnec("fc1")).thenReturn(mock(FlowCnec.class));
        RaoResultImpl raoResult = new RaoResultImpl(crac);

        try (JsonParser parser = parserFrom(json)) {
            OpenRaoException ex = assertThrows(OpenRaoException.class,
                () -> FlowCnecResultArrayDeserializer.deserialize(parser, raoResult, crac, "1.4"));
            assertEquals("zonalPtdfSum can only be defined in the MEGAWATT section", ex.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON content");
        }
    }
}
