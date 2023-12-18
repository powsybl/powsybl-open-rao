/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PreventiveStateTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);

    @Test
    void testEqualsForPreventive() {
        PreventiveState state1 = new PreventiveState(PREVENTIVE_INSTANT);
        PreventiveState state2 = new PreventiveState(PREVENTIVE_INSTANT);

        assertEquals(state1, state2);
    }

    @Test
    void testHashCodeForPreventive() {
        PreventiveState state = new PreventiveState(PREVENTIVE_INSTANT);
        assertEquals("preventive".hashCode(), state.hashCode());
    }

    @Test
    void testToStringForPreventive() {
        PreventiveState state = new PreventiveState(PREVENTIVE_INSTANT);
        assertEquals("preventive", state.toString());
    }

    @Test
    void testCannotCreatePreventiveStateWithNonPreventiveInstant() {
        Instant instant = new InstantImpl("my instant", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
        FaraoException exception = assertThrows(FaraoException.class, () -> new PreventiveState(instant));
        assertEquals("Instant must be preventive", exception.getMessage());
    }
}
