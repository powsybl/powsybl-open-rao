/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PreventiveStateTest {
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);

    @Test
    void testEqualsForPreventive() {
        PreventiveState state1 = new PreventiveState(INSTANT_PREV);
        PreventiveState state2 = new PreventiveState(INSTANT_PREV);

        assertEquals(state1, state2);
    }

    @Test
    void testHashCodeForPreventive() {
        PreventiveState state = new PreventiveState(INSTANT_PREV);
        assertEquals("preventive".hashCode(), state.hashCode());
    }

    @Test
    void testToStringForPreventive() {
        PreventiveState state = new PreventiveState(INSTANT_PREV);
        assertEquals("preventive", state.toString());
    }
}
