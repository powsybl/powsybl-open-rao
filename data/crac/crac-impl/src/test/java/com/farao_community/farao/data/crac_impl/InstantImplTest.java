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
        Instant instantPrev = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
        Instant instantOutage = new InstantImpl("outage", InstantKind.OUTAGE, instantPrev);
        Instant instantAuto = new InstantImpl("auto", InstantKind.AUTO, instantOutage);
        Instant instantCurative = new InstantImpl("curative", InstantKind.CURATIVE, instantAuto);

        assertEquals(0, instantPrev.getOrder());
        assertEquals(1, instantOutage.getOrder());
        assertEquals(2, instantAuto.getOrder());
        assertEquals(3, instantCurative.getOrder());

        assertNull(instantPrev.getPreviousInstant());
        assertEquals(instantPrev, instantOutage.getPreviousInstant());
        assertEquals(instantOutage, instantAuto.getPreviousInstant());
        assertEquals(instantAuto, instantCurative.getPreviousInstant());

        assertFalse(instantAuto.comesBefore(instantAuto));
        assertTrue(instantOutage.comesBefore(instantCurative));
        assertFalse(instantOutage.comesBefore(instantPrev));
    }
}
