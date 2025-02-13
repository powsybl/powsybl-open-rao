/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class OnContingencyStateImplTest {

    private State initialState;
    private State curativeState1;
    private State curativeState2;
    private Instant preventiveInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        Crac crac = new CracImplFactory().create("cracId")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant("preventive");
        curativeInstant = crac.getInstant("curative");
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withContingencyElement("anyNetworkElement", ContingencyElementType.LINE)
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withContingencyElement("anyNetworkElement", ContingencyElementType.LINE)
            .add();
        initialState = new PreventiveState(preventiveInstant, null);
        curativeState1 = new PostContingencyState(contingency1, curativeInstant, null);
        curativeState2 = new PostContingencyState(contingency2, curativeInstant, null);
    }

    @Test
    void testSetterGetter() {
        OnContingencyStateImpl rule1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, curativeState1);
        assertEquals(curativeState1, rule1.getState());
        assertEquals("contingency1", rule1.getContingency().getId());
        assertEquals(curativeInstant, rule1.getInstant());
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
        assertNotEquals(preventiveInstant, rule1);
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
