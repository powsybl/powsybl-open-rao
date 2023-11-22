/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class OnContingencyStateImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private static final Instant INSTANT_PREV = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl(CURATIVE_INSTANT_ID, InstantKind.CURATIVE, INSTANT_AUTO);
    private State initialState;
    private State curativeState1;
    private State curativeState2;

    @BeforeEach
    public void setUp() {
        initialState = new PreventiveState(INSTANT_PREV);
        Crac crac = new CracImplFactory().create("cracId");
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("anyNetworkElement")
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withNetworkElement("anyNetworkElement")
            .add();
        curativeState1 = new PostContingencyState(contingency1, INSTANT_CURATIVE);
        curativeState2 = new PostContingencyState(contingency2, INSTANT_CURATIVE);
    }

    @Test
    void testSetterGetter() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, curativeState1);
        assertEquals(curativeState1, rule1.getState());
        assertEquals("contingency1", rule1.getContingency().getId());
        assertEquals(INSTANT_CURATIVE, rule1.getInstant());
    }

    @Test
    void testEqualsSameObject() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1, rule1);
    }

    @Test
    void testEqualsTrue() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, initialState);
        OnContingencyStateImpl rule2 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1, rule2);
    }

    @Test
    void testEqualsFalseNotTheSameObject() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, initialState);
        assertNotEquals(INSTANT_PREV, rule1);
    }

    @Test
    void testEqualsFalseForUsageMethod() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, initialState);
        OnContingencyStateImpl rule2 = new OnContingencyStateImpl(UsageMethod.FORCED, initialState);
        assertNotEquals(rule1, rule2);
    }

    @Test
    void testEqualsFalseForState() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, curativeState1);
        OnContingencyStateImpl rule2 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, curativeState2);
        assertNotEquals(rule1, rule2);
    }

    @Test
    void testHashCode() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, initialState);
        OnContingencyStateImpl rule2 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testHashCodeFalseForUsageMethod() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, initialState);
        OnContingencyStateImpl rule2 = new OnContingencyStateImpl(UsageMethod.FORCED, initialState);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testHashCodeFalseForContingency() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, curativeState1);
        OnContingencyStateImpl rule2 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, curativeState2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

}
