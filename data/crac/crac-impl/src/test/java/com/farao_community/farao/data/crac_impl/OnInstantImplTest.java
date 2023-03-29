/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class OnInstantImplTest {

    @Test
    void testGetterSetter() {
        OnInstantImpl onInstant = new OnInstantImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        assertEquals(Instant.PREVENTIVE, onInstant.getInstant());
    }

    @Test
    void testEqualsSameObject() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        assertEquals(rule1, rule1);
    }

    @Test
    void testEqualsTrue() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testEqualsFalseForUsageMethod() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.FORCED_IF_AVAILABLE, Instant.PREVENTIVE);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testEqualsFalseForInstant() {
        OnInstantImpl rule1 = new OnInstantImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        OnInstantImpl rule2 = new OnInstantImpl(UsageMethod.AVAILABLE, Instant.CURATIVE);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }
}
