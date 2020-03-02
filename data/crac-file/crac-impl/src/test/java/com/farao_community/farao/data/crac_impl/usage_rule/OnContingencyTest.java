/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleState;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class OnContingencyTest {

    @Test
    public void getCnec() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertEquals("contingency", rule1.getContingency().getId());
    }

    @Test
    public void testEqualsSameObject() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertEquals(rule1, rule1);
    }

    @Test
    public void testEqualsTrue() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        OnContingency rule2 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertEquals(rule1, rule2);
    }

    @Test
    public void testEqualsFalseNotTheSameObject() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertFalse(rule1.equals(new Instant("fail", 10)));
    }

    @Test
    public void testEqualsFalseForUsageMethod() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        OnContingency rule2 = new OnContingency(
            UsageMethod.FORCED,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertNotEquals(rule1, rule2);
    }

    @Test
    public void testEqualsFalseForContingency() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        OnContingency rule2 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency2",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertNotEquals(rule1, rule2);
    }

    @Test
    public void testHashCode() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        OnContingency rule2 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testHashCodeFalseForUsageMethod() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        OnContingency rule2 = new OnContingency(
            UsageMethod.FORCED,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void testHashCodeFalseForContingency() {
        State initialState = new SimpleState(
            Optional.empty(),
            new Instant("initial-instant", 0)
        );

        OnContingency rule1 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        OnContingency rule2 = new OnContingency(
            UsageMethod.AVAILABLE,
            initialState,
            new ComplexContingency(
                "contingency2",
                Collections.singleton(new NetworkElement("ne1"))
            )
        );

        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

}
