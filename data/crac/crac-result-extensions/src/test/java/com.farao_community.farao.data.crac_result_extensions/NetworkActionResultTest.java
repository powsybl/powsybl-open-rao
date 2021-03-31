/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleState;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionResultTest {
    private NetworkActionResult networkActionResult;
    private Set<String> states;
    private State initialState;
    private State outage1;
    private State curative1;
    private State outage2;
    private State curative2;

    @Before
    public void setUp() {
        states = new HashSet<>();
        initialState = new SimpleState(Optional.empty(), Instant.PREVENTIVE);
        outage1 = new SimpleState(Optional.of(new ComplexContingency("co1")), Instant.OUTAGE);
        curative1 = new SimpleState(Optional.of(new ComplexContingency("co1")), Instant.CURATIVE);
        outage2 = new SimpleState(Optional.of(new ComplexContingency("co2")), Instant.OUTAGE);
        curative2 = new SimpleState(Optional.of(new ComplexContingency("co2")), Instant.CURATIVE);
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
