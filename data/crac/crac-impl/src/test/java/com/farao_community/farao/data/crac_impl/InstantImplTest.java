/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
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
        Instant prevInstant = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
        Instant outageInstant = new InstantImpl("outage", InstantKind.OUTAGE, prevInstant);
        Instant autoInstant = new InstantImpl("auto", InstantKind.AUTO, outageInstant);
        Instant curativeInstant = new InstantImpl("curative", InstantKind.CURATIVE, autoInstant);

        assertEquals(0, prevInstant.getOrder());
        assertEquals(1, outageInstant.getOrder());
        assertEquals(2, autoInstant.getOrder());
        assertEquals(3, curativeInstant.getOrder());

        assertNull(prevInstant.getPreviousInstant());
        assertEquals("preventive", outageInstant.getPreviousInstant().getId());
        assertEquals(outageInstant, autoInstant.getPreviousInstant());
        assertEquals(autoInstant, curativeInstant.getPreviousInstant());

        assertFalse(autoInstant.comesBefore(autoInstant));
        assertTrue(outageInstant.comesBefore(curativeInstant));
        assertFalse(outageInstant.comesBefore(prevInstant));
    }

    @Test
    void testInitialInstantIsProtected() {
        FaraoException exception = assertThrows(FaraoException.class, () -> new InstantImpl("initial", InstantKind.PREVENTIVE, null));
        assertEquals("Instant with id \"initial\" cannont be defined", exception.getMessage());
    }
}
