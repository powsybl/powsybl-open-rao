/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.SimpleState;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FreeToUseTest {

    @Test
    public void testEqualsSameObject() {
        FreeToUse rule1 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        assertEquals(rule1, rule1);
    }

    @Test
    public void testEqualsTrue() {
        FreeToUse rule1 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        FreeToUse rule2 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        assertEquals(rule1, rule2);
    }

    @Test
    public void testEqualsFalseNotTheSameObject() {
        FreeToUse rule1 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        assertFalse(rule1.equals(new Instant("fail", 10)));
    }

    @Test
    public void testEqualsFalseForUsageMethod() {
        FreeToUse rule1 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        FreeToUse rule2 = new FreeToUse(UsageMethod.FORCED, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        assertNotEquals(rule1, rule2);
    }

    @Test
    public void testEqualsFalseForInstant() {
        FreeToUse rule1 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        FreeToUse rule2 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant-2", 0)
        ));

        assertNotEquals(rule1, rule2);
    }

    @Test
    public void testHashCode() {
        FreeToUse rule1 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        FreeToUse rule2 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testHashCodeFalseForUsageMethod() {
        FreeToUse rule1 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        FreeToUse rule2 = new FreeToUse(UsageMethod.FORCED, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testHashCodeFalseForInstant() {
        FreeToUse rule1 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        ));

        FreeToUse rule2 = new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(
            Optional.empty(),
            new Instant("initial-instant-2", 0)
        ));

        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }
}
