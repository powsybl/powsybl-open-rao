/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PreventiveStateTest {

    @Test
    void testEqualsForPreventive() {
        Instant instant = new Instant(0, "preventive", Instant.Kind.PREVENTIVE);
        PreventiveState state1 = new PreventiveState(instant);
        PreventiveState state2 = new PreventiveState(instant);

        assertEquals(state1, state2);
    }

    @Test
    void testHashCodeForPreventive() {
        Instant instant = new Instant(0, "preventive", Instant.Kind.PREVENTIVE);
        PreventiveState state = new PreventiveState(instant);
        assertEquals("preventive".hashCode(), state.hashCode());
    }

    @Test
    void testToStringForPreventive() {
        Instant instant = new Instant(0, "preventive", Instant.Kind.PREVENTIVE);
        PreventiveState state = new PreventiveState(instant);
        assertEquals("preventive", state.toString());
    }
}
