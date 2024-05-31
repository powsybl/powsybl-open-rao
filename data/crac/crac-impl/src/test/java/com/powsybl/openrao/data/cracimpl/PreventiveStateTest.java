/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PreventiveStateTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);

    @Test
    void testToStringForPreventive() {
        PreventiveState state = new PreventiveState(PREVENTIVE_INSTANT);
        assertEquals("preventive", state.toString());
    }

    @Test
    void testCannotCreatePreventiveStateWithNonPreventiveInstant() {
        Instant instant = new InstantImpl("my instant", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new PreventiveState(instant));
        assertEquals("Instant must be preventive", exception.getMessage());
    }
}
