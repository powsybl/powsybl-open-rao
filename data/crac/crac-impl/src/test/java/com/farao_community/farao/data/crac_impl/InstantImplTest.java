/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

class InstantImplTest {

    @Test
    void testPreventive() {
        Instant instant = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
        assertEquals(0, instant.getOrder());
        assertEquals("preventive", instant.toString());
        assertEquals(InstantKind.PREVENTIVE, instant.getInstantKind());
    }

    @Test
    void testCombineInstants() {
        Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
        Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
        Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
        Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);

        assertEquals(0, INSTANT_PREV.getOrder());
        assertEquals(1, INSTANT_OUTAGE.getOrder());
        assertEquals(2, INSTANT_AUTO.getOrder());
        assertEquals(3, INSTANT_CURATIVE.getOrder());

        assertNull(INSTANT_PREV.getPreviousInstant());
        assertEquals(INSTANT_PREV, INSTANT_OUTAGE.getPreviousInstant());
        assertEquals(INSTANT_OUTAGE, INSTANT_AUTO.getPreviousInstant());
        assertEquals(INSTANT_AUTO, INSTANT_CURATIVE.getPreviousInstant());

        assertFalse(INSTANT_AUTO.comesBefore(INSTANT_AUTO));
        assertTrue(INSTANT_OUTAGE.comesBefore(INSTANT_CURATIVE));
        assertFalse(INSTANT_OUTAGE.comesBefore(INSTANT_PREV));
    }
}
