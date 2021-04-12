/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.ContingencyImpl;
import com.farao_community.farao.data.crac_impl.OnStateImpl;
import com.farao_community.farao.data.crac_impl.PostContingencyState;
import com.farao_community.farao.data.crac_impl.PreventiveState;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class OnStateImplTest {

    private State initialState;
    private State curativeState1;
    private State curativeState2;

    @Before
    public void setUp() {
        initialState = new PreventiveState();
        curativeState1 = new PostContingencyState(new ContingencyImpl("contingency1"), Instant.CURATIVE);
        curativeState2 = new PostContingencyState(new ContingencyImpl("contingency2"), Instant.CURATIVE);
    }

    @Test
    public void testSetterGetter() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState1);
        assertEquals(curativeState1, rule1.getState());
        assertEquals("contingency1", rule1.getContingency().getId());
        assertEquals(Instant.CURATIVE, rule1.getInstant());
    }

    @Test
    public void testEqualsSameObject() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1, rule1);
    }

    @Test
    public void testEqualsTrue() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1, rule2);
    }

    @Test
    public void testEqualsFalseNotTheSameObject() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        assertNotEquals(rule1, Instant.PREVENTIVE);
    }

    @Test
    public void testEqualsFalseForUsageMethod() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.FORCED, initialState);
        assertNotEquals(rule1, rule2);
    }

    @Test
    public void testEqualsFalseForState() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState1);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState2);
        assertNotEquals(rule1, rule2);
    }

    @Test
    public void testHashCode() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testHashCodeFalseForUsageMethod() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.FORCED, initialState);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testHashCodeFalseForContingency() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState1);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

}
