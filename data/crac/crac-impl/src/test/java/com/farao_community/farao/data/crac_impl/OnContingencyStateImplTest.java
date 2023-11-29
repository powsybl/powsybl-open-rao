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

    private static final Instant PREVENTIVE_INSTANT = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
    private static final Instant OUTAGE_INSTANT = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.OUTAGE, PREVENTIVE_INSTANT);
    private static final Instant AUTO_INSTANT = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, OUTAGE_INSTANT);
    private static final Instant CURATIVE_INSTANT = new InstantImpl(CURATIVE_INSTANT_ID, InstantKind.CURATIVE, AUTO_INSTANT);
    private State initialState;
    private State curativeState1;
    private State curativeState2;

    @BeforeEach
    public void setUp() {
        initialState = new PreventiveState(PREVENTIVE_INSTANT);
        Crac crac = new CracImplFactory().create("cracId");
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("anyNetworkElement")
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withNetworkElement("anyNetworkElement")
            .add();
        curativeState1 = new PostContingencyState(contingency1, CURATIVE_INSTANT);
        curativeState2 = new PostContingencyState(contingency2, CURATIVE_INSTANT);
    }

    @Test
    void testSetterGetter() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, curativeState1);
        assertEquals(curativeState1, rule1.getState());
        assertEquals("contingency1", rule1.getContingency().getId());
        assertEquals(CURATIVE_INSTANT, rule1.getInstant());
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
        assertNotEquals(PREVENTIVE_INSTANT, rule1);
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
