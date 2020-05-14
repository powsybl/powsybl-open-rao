/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SimpleStateAdderTest {
    private SimpleCrac crac;

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac");
    }

    @Test
    public void addStates() {
        Contingency contingency = crac.newContingency().setName("contingency").setId("neID").add();
        Instant instant = crac.newInstant().setId("instant").setSeconds(5).add();
        State state = crac.newState()
                .setContingency(contingency)
                .setInstant(instant)
                .add();
        assertEquals(contingency, state.getContingency().get());
        assertEquals(instant, state.getInstant());
        assertEquals(1, crac.getStates().size());
        assertEquals(state, crac.getStates().iterator().next());
    }

    @Test
    public void addStatesPreventive() {
        Instant instant = crac.addInstant("instant", 5);
        State state = crac.newState()
                .setInstant(instant)
                .add();
        assertEquals(Optional.empty(), state.getContingency());
        assertEquals(instant, state.getInstant());
        assertEquals(1, crac.getStates().size());
        assertEquals(state, crac.getStates().iterator().next());
    }

    @Test(expected = FaraoException.class)
    public void addStatesFail() {
        Contingency contingency = crac.addContingency("contingency", "neID");
        crac.newState().setContingency(contingency).add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        SimpleStateAdder tmp = new SimpleStateAdder(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullInstantFail() {
        crac.newState().setInstant(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullContingencyFail() {
        crac.newState().setContingency(null);
    }
}
