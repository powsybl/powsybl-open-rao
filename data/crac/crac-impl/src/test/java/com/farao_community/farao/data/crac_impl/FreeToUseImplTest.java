/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FreeToUseImplTest {

    @Test
    public void testGetterSetter() {
        FreeToUseImpl freeToUse = new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        assertEquals(Instant.PREVENTIVE, freeToUse.getInstant());
    }

    @Test
    public void testEqualsSameObject() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        assertEquals(rule1, rule1);
    }

    @Test
    public void testEqualsTrue() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        FreeToUseImpl rule2 = new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testEqualsFalseForUsageMethod() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        FreeToUseImpl rule2 = new FreeToUseImpl(UsageMethod.FORCED, Instant.PREVENTIVE);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testEqualsFalseForInstant() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
        FreeToUseImpl rule2 = new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.CURATIVE);

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }
}
