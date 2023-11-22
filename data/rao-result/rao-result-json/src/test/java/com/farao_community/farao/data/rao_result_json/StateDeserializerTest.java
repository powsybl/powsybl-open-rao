/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.rao_result_json.deserializers.StateDeserializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class StateDeserializerTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void testGetState() {
        Crac crac = Mockito.mock(Crac.class);
        State preventiveState = Mockito.mock(State.class);
        State curativeState = Mockito.mock(State.class);
        State outageState = Mockito.mock(State.class);
        String contingencyId = "contingency";
        Mockito.when(crac.getPreventiveState()).thenReturn(preventiveState);
        Crac cracForInstants = new CracImpl("test-cracForInstants")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        Instant preventiveInstant = cracForInstants.getInstant(PREVENTIVE_INSTANT_ID);
        Instant outageInstant = cracForInstants.getInstant(OUTAGE_INSTANT_ID);
        Instant curativeInstant = cracForInstants.getInstant(CURATIVE_INSTANT_ID);
        Mockito.when(crac.getState(contingencyId, curativeInstant)).thenReturn(curativeState);
        Mockito.when(crac.getState(contingencyId, outageInstant)).thenReturn(outageState);
        Mockito.when(crac.getInstant(PREVENTIVE_INSTANT_ID)).thenReturn(preventiveInstant);
        Mockito.when(crac.getInstant(OUTAGE_INSTANT_ID)).thenReturn(outageInstant);
        Mockito.when(crac.getInstant(CURATIVE_INSTANT_ID)).thenReturn(curativeInstant);

        FaraoException exception = assertThrows(FaraoException.class, () -> StateDeserializer.getState(null, contingencyId, crac, "type"));
        assertEquals("Cannot deserialize RaoResult: no instant defined in activated states of type", exception.getMessage());
        assertEquals(preventiveState, StateDeserializer.getState(PREVENTIVE_INSTANT_ID, null, crac, null));
        exception = assertThrows(FaraoException.class, () -> StateDeserializer.getState(OUTAGE_INSTANT_ID, null, crac, "type"));
        assertEquals("Cannot deserialize RaoResult: no contingency defined in N-k activated states of type", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> StateDeserializer.getState(OUTAGE_INSTANT_ID, "wrongContingencyId", crac, "type"));
        assertEquals("Cannot deserialize RaoResult: State at instant outage with contingency wrongContingencyId not found in Crac", exception.getMessage());
        assertEquals(outageState, StateDeserializer.getState(OUTAGE_INSTANT_ID, contingencyId, crac, "type"));
        assertEquals(curativeState, StateDeserializer.getState(CURATIVE_INSTANT_ID, contingencyId, crac, "type"));
    }
}
