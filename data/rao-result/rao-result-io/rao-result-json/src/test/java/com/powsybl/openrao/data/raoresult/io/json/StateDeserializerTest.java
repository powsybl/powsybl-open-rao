/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.io.json.deserializers.StateDeserializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class StateDeserializerTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void testGetState() {
        Crac crac = mock(Crac.class);
        State preventiveState = mock(State.class);
        State curativeState = mock(State.class);
        State outageState = mock(State.class);
        String contingencyId = "contingency";
        when(crac.getPreventiveState()).thenReturn(preventiveState);
        Instant preventiveInstant = mockInstant(true);
        Instant outageInstant = mockInstant(false);
        Instant curativeInstant = mockInstant(false);
        when(crac.getState(contingencyId, curativeInstant)).thenReturn(curativeState);
        when(crac.getState(contingencyId, outageInstant)).thenReturn(outageState);
        when(crac.getInstant(PREVENTIVE_INSTANT_ID)).thenReturn(preventiveInstant);
        when(crac.getInstant(OUTAGE_INSTANT_ID)).thenReturn(outageInstant);
        when(crac.getInstant(CURATIVE_INSTANT_ID)).thenReturn(curativeInstant);

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> StateDeserializer.getState(null, contingencyId, crac, "type"));
        assertEquals("Cannot deserialize RaoResult: no instant defined in activated states of type", exception.getMessage());
        assertEquals(preventiveState, StateDeserializer.getState(PREVENTIVE_INSTANT_ID, null, crac, null));
        exception = assertThrows(OpenRaoException.class, () -> StateDeserializer.getState(OUTAGE_INSTANT_ID, null, crac, "type"));
        assertEquals("Cannot deserialize RaoResult: no contingency defined in N-k activated states of type", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> StateDeserializer.getState(OUTAGE_INSTANT_ID, "wrongContingencyId", crac, "type"));
        assertEquals("Cannot deserialize RaoResult: State at instant outage with contingency wrongContingencyId not found in Crac", exception.getMessage());
        assertEquals(outageState, StateDeserializer.getState(OUTAGE_INSTANT_ID, contingencyId, crac, "type"));
        assertEquals(curativeState, StateDeserializer.getState(CURATIVE_INSTANT_ID, contingencyId, crac, "type"));
    }

    private static Instant mockInstant(boolean isPreventive) {
        Instant instant = mock(Instant.class);
        when(instant.isPreventive()).thenReturn(isPreventive);
        return instant;
    }
}
