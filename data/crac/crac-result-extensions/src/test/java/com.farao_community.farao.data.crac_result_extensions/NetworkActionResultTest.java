/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.State;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionResultTest {
    private NetworkActionResult networkActionResult;
    private State initialState;
    private State outage1;
    private State curative1;
    private State outage2;
    private State curative2;

    @Before
    public void setUp() {

        initialState = Mockito.mock(State.class);
        outage1 = Mockito.mock(State.class);
        curative1 = Mockito.mock(State.class);
        outage2 = Mockito.mock(State.class);
        curative2 = Mockito.mock(State.class);

        Mockito.when(initialState.getId()).thenReturn("preventive");
        Mockito.when(outage1.getId()).thenReturn("co1 - outage");
        Mockito.when(curative1.getId()).thenReturn("co1 - curative");
        Mockito.when(outage2.getId()).thenReturn("co2 - outage");
        Mockito.when(curative2.getId()).thenReturn("co2 - curative");

        Set<String> states = new HashSet<>();
        states.add(initialState.getId());
        states.add(outage1.getId());
        states.add(curative1.getId());
        states.add(outage2.getId());
        states.add(curative2.getId());
        networkActionResult = new NetworkActionResult(states);
    }

    @Test
    public void constructor() {
        assertTrue(networkActionResult.activationMap.containsKey(initialState.getId()));
        assertTrue(networkActionResult.activationMap.containsKey(outage1.getId()));
        assertTrue(networkActionResult.activationMap.containsKey(curative1.getId()));
        assertTrue(networkActionResult.activationMap.containsKey(outage2.getId()));
        assertTrue(networkActionResult.activationMap.containsKey(curative2.getId()));
        assertEquals(5, networkActionResult.activationMap.size());
    }

    @Test
    public void activate() {
        networkActionResult.activate(initialState.getId());
        assertTrue(networkActionResult.isActivated(initialState.getId()));
        assertFalse(networkActionResult.isActivated(outage1.getId()));
    }

    @Test
    public void deactivate() {
        networkActionResult.activate(initialState.getId());
        assertTrue(networkActionResult.isActivated(initialState.getId()));
        networkActionResult.deactivate(initialState.getId());
        assertFalse(networkActionResult.isActivated(initialState.getId()));
    }
}
