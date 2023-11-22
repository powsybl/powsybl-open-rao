/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class OnInstantImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private static final Instant INSTANT_PREV = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl(CURATIVE_INSTANT_ID, InstantKind.CURATIVE, INSTANT_AUTO);

    @Test
    void testGetterSetter() {
        OnInstantImpl onInstant = new OnInstantImpl(UsageMethod.AVAILABLE, INSTANT_PREV);
        assertEquals(INSTANT_PREV, onInstant.getInstant());
    }

    @Test
    void testEqualsSameObject() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, INSTANT_PREV);
        assertEquals(rule1, rule1);
    }

    @Test
    void testEqualsTrue() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, INSTANT_PREV);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.AVAILABLE, INSTANT_PREV);

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testEqualsFalseForUsageMethod() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, INSTANT_PREV);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.FORCED, INSTANT_PREV);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testEqualsFalseForInstant() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, INSTANT_PREV);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.AVAILABLE, INSTANT_CURATIVE);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }
}
