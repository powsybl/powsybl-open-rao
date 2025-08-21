/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageMethodTest {
    @Test
    void testGetStrongestUsageMethod() {
        // asserts FORCED prevails over AVAILABLE
        Set<UsageMethod> usageMethods = new HashSet<>(Set.of(UsageMethod.AVAILABLE, UsageMethod.FORCED));
        assertEquals(UsageMethod.FORCED, UsageMethod.getStrongestUsageMethod(usageMethods));
        // asserts UNAVAILABLE prevails over all others
        usageMethods.add(UsageMethod.UNAVAILABLE);
        assertEquals(UsageMethod.UNAVAILABLE, UsageMethod.getStrongestUsageMethod(usageMethods));
        // asserts that the method default return value is UNAVAILABLE
        assertEquals(UsageMethod.UNAVAILABLE, UsageMethod.getStrongestUsageMethod(Set.of()));
    }
}
