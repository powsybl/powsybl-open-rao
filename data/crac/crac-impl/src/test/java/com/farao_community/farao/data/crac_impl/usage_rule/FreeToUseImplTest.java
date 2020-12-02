/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FreeToUseImplTest {

    @Test
    public void testGetterSetter() {
        Instant n = new Instant("initial-instant", 0);
        Instant outage = new Instant("outage", 60);

        FreeToUseImpl freeToUse = new FreeToUseImpl(UsageMethod.AVAILABLE, n);
        assertEquals(n, freeToUse.getInstant());

        freeToUse.setIntant(outage);
        assertEquals(outage, freeToUse.getInstant());
    }

    @Test
    public void testEqualsSameObject() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, new Instant("initial-instant", 0));
        assertEquals(rule1, rule1);
    }

    @Test
    public void testEqualsTrue() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, new Instant("initial-instant", 0));
        FreeToUseImpl rule2 = new FreeToUseImpl(UsageMethod.AVAILABLE, new Instant("initial-instant", 0));

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testEqualsFalseNotTheSameObject() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, new Instant("initial-instant", 0));

        assertNotEquals(rule1, new Instant("fail", 10));
    }

    @Test
    public void testEqualsFalseForUsageMethod() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, new Instant("initial-instant", 0));
        FreeToUseImpl rule2 = new FreeToUseImpl(UsageMethod.FORCED, new Instant("initial-instant", 0));

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testEqualsFalseForInstant() {
        FreeToUseImpl rule1 = new FreeToUseImpl(UsageMethod.AVAILABLE, new Instant("initial-instant", 0));
        FreeToUseImpl rule2 = new FreeToUseImpl(UsageMethod.AVAILABLE, new Instant("initial-instant-2", 0));

        assertNotEquals(rule1, rule2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());

    }
}
