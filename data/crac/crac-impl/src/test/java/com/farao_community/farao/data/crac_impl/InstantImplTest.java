/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
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
        Instant instantOutage = new InstantImpl("outage", InstantKind.OUTAGE, prevInstant);
        Instant instantAuto = new InstantImpl("auto", InstantKind.AUTO, instantOutage);
        Instant curativeInstant = new InstantImpl("curative", InstantKind.CURATIVE, instantAuto);

        assertEquals(0, prevInstant.getOrder());
        assertEquals(1, instantOutage.getOrder());
        assertEquals(2, instantAuto.getOrder());
        assertEquals(3, curativeInstant.getOrder());

        assertNull(prevInstant.getPreviousInstant());
        assertEquals("preventive", instantOutage.getPreviousInstant().getId());
        assertEquals(instantOutage, instantAuto.getPreviousInstant());
        assertEquals(instantAuto, curativeInstant.getPreviousInstant());

        assertFalse(instantAuto.comesBefore(instantAuto));
        assertTrue(instantOutage.comesBefore(curativeInstant));
        assertFalse(instantOutage.comesBefore(prevInstant));
    }
}
