/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PreventiveStateTest {

    @Test
    public void testEqualsForPreventive() {
        PreventiveState state1 = new PreventiveState();
        PreventiveState state2 = new PreventiveState();

        assertEquals(state1, state2);
    }

    @Test
    public void testHashCodeForPreventive() {
        PreventiveState state = new PreventiveState();
        assertEquals("preventive".hashCode(), state.hashCode());
    }

    @Test
    public void testToStringForPreventive() {
        PreventiveState state = new PreventiveState();
        assertEquals("preventive", state.toString());
    }
}
