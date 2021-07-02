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
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_json.deserializers.StateDeserializer;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class StateDeserializerTest {
    @Test
    public void testGetState() {
        Crac crac = Mockito.mock(Crac.class);
        State preventiveState = Mockito.mock(State.class);
        State curativeState = Mockito.mock(State.class);
        State outageState = Mockito.mock(State.class);
        String contingencyId = "contingency";
        Mockito.when(crac.getPreventiveState()).thenReturn(preventiveState);
        Mockito.when(crac.getState(contingencyId, Instant.CURATIVE)).thenReturn(curativeState);
        Mockito.when(crac.getState(contingencyId, Instant.OUTAGE)).thenReturn(outageState);

        assertThrows(FaraoException.class, () -> StateDeserializer.getState(null, contingencyId, crac, "type"));
        assertEquals(preventiveState, StateDeserializer.getState(Instant.PREVENTIVE, null, crac, null));
        assertThrows(FaraoException.class, () -> StateDeserializer.getState(Instant.OUTAGE, null, crac, "type"));
        assertThrows(FaraoException.class, () -> StateDeserializer.getState(Instant.OUTAGE, "wrongContingencyId", crac, "type"));
        assertEquals(outageState, StateDeserializer.getState(Instant.OUTAGE, contingencyId, crac, "type"));
        assertEquals(curativeState, StateDeserializer.getState(Instant.CURATIVE, contingencyId, crac, "type"));
    }

}
