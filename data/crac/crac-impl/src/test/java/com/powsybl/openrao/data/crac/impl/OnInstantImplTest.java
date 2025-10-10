/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class OnInstantImplTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant OUTAGE_INSTANT = new InstantImpl("outage", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
    private static final Instant AUTO_INSTANT = new InstantImpl("auto", InstantKind.AUTO, OUTAGE_INSTANT);
    private static final Instant CURATIVE_INSTANT = new InstantImpl("curative", InstantKind.CURATIVE, AUTO_INSTANT);

    @Test
    void testGetterSetter() {
        OnInstantImpl onInstant = new OnInstantImpl(PREVENTIVE_INSTANT);
        assertEquals(PREVENTIVE_INSTANT, onInstant.getInstant());
    }

    @Test
    void testEqualsSameObject() {
        OnInstantImpl rule1 = new OnInstantImpl(PREVENTIVE_INSTANT);
        assertEquals(rule1, rule1);
    }

    @Test
    void testEqualsTrue() {
        OnInstantImpl rule1 = new OnInstantImpl(PREVENTIVE_INSTANT);
        OnInstantImpl rule2 = new OnInstantImpl(PREVENTIVE_INSTANT);

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testEqualsFalseForInstant() {
        OnInstantImpl rule1 = new OnInstantImpl(PREVENTIVE_INSTANT);
        OnInstantImpl rule2 = new OnInstantImpl(CURATIVE_INSTANT);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }
}
