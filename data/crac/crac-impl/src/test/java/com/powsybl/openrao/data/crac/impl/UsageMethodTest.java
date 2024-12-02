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
