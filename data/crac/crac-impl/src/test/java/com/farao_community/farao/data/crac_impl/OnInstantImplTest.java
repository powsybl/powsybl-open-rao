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
    private static final Instant instantPrev = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant instantOutage = new InstantImpl("outage", InstantKind.OUTAGE, instantPrev);
    private static final Instant instantAuto = new InstantImpl("auto", InstantKind.AUTO, instantOutage);
    private static final Instant instantCurative = new InstantImpl("curative", InstantKind.CURATIVE, instantAuto);

    @Test
    void testGetterSetter() {
        OnInstantImpl onInstant = new OnInstantImpl(UsageMethod.AVAILABLE, instantPrev);
        assertEquals(instantPrev, onInstant.getInstant());
    }

    @Test
    void testEqualsSameObject() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, instantPrev);
        assertEquals(rule1, rule1);
    }

    @Test
    void testEqualsTrue() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, instantPrev);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.AVAILABLE, instantPrev);

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testEqualsFalseForUsageMethod() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, instantPrev);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.FORCED, instantPrev);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testEqualsFalseForInstant() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, instantPrev);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.AVAILABLE, instantCurative);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }
}
