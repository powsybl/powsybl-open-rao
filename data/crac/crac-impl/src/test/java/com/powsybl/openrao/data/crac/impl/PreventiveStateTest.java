/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PreventiveStateTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);

    @Test
    void testEqualsForPreventive() {
        PreventiveState state1 = new PreventiveState(PREVENTIVE_INSTANT, null);
        PreventiveState state2 = new PreventiveState(PREVENTIVE_INSTANT, null);

        assertEquals(state1, state2);
    }

    @Test
    void testHashCodeForPreventive() {
        PreventiveState state = new PreventiveState(PREVENTIVE_INSTANT, null);
        assertEquals("preventive".hashCode(), state.hashCode());
    }

    @Test
    void testToStringForPreventive() {
        PreventiveState state = new PreventiveState(PREVENTIVE_INSTANT, null);
        assertEquals("preventive", state.toString());
    }

    @Test
    void testCannotCreatePreventiveStateWithNonPreventiveInstant() {
        Instant instant = new InstantImpl("my instant", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new PreventiveState(instant, null));
        assertEquals("Instant must be preventive", exception.getMessage());
    }
}
