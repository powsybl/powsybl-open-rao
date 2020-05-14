/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SimpleCnecAdderTest {
    private SimpleCrac crac;
    private State state1;
    private State state2;
    private static final double DOUBLE_TOLERANCE = 1e-6;

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac");
        Contingency contingency1 = crac.newContingency().setId("conId1").add();
        Instant instant1 = crac.newInstant().setId("instId1").setSeconds(10).add();
        Instant instant2 = crac.newInstant().setId("instId2").setSeconds(20).add();
        state1 = crac.newState().setContingency(contingency1).setInstant(instant1).add();
        state2 = crac.newState().setInstant(instant2).add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        SimpleCnecAdder tmp = new SimpleCnecAdder(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullIdFail() {
        crac.newCnec().setId(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNameFail() {
        crac.newCnec().setName(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullStateFail() {
        crac.newCnec().setState(null);
    }

    @Test(expected = FaraoException.class)
    public void testUniqueNetworkElement() {
        crac.newCnec()
                .newNetworkElement().setId("neId1").setName("neName1").add()
                .newNetworkElement();
    }

    @Test(expected = FaraoException.class)
    public void testNoIdFail() {
        crac.newCnec()
                .setName("cnecName1")
                .setState(state1)
                .newNetworkElement().setId("neId1").setName("neName1").add()
                .newThreshold().setUnit(Unit.MEGAWATT).setSide(Side.LEFT).setDirection(Direction.BOTH).setMaxValue(1000.0).add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoStateFail() {
        crac.newCnec()
                .setId("cnecId1")
                .setName("cnecName1")
                .newNetworkElement().setId("neId1").setName("neName1").add()
                .newThreshold().setUnit(Unit.MEGAWATT).setSide(Side.LEFT).setDirection(Direction.BOTH).setMaxValue(1000.0).add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoNetworkElementFail() {
        crac.newCnec()
                .setId("cnecId1")
                .setName("cnecName1")
                .setState(state1)
                .newThreshold().setUnit(Unit.MEGAWATT).setSide(Side.LEFT).setDirection(Direction.BOTH).setMaxValue(1000.0).add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoThresholdFail() {
        crac.newCnec()
                .setId("cnecId1")
                .setName("cnecName1")
                .setState(state1)
                .newNetworkElement().setId("neId1").setName("neName1").add()
                .add();
    }

    @Test
    public void testAdd() {
        Cnec cnec1 = crac.newCnec()
                .setId("cnecId1")
                .setName("cnecName1")
                .setState(state1)
                .newNetworkElement().setId("neId1").setName("neName1").add()
                .newThreshold().setUnit(Unit.MEGAWATT).setSide(Side.LEFT).setDirection(Direction.BOTH).setMaxValue(1000.0).add()
                .add();
        Cnec cnec2 = crac.newCnec()
                .setId("cnecId2")
                .setState(state2)
                .newNetworkElement().setId("neId2").add()
                .newThreshold().setUnit(Unit.MEGAWATT).setSide(Side.LEFT).setDirection(Direction.DIRECT).setMaxValue(500.0).add()
                .add();
        assertEquals(2, crac.getCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(state1, cnec1.getState());
        assertEquals("neName1", cnec1.getNetworkElement().getName());
        assertEquals(1000.0, cnec1.getMaxThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getMinThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(state2, cnec2.getState());
        assertEquals("neId2", cnec2.getNetworkElement().getName());
        assertEquals(500.0, cnec2.getMaxThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
        assertEquals(Double.NEGATIVE_INFINITY, cnec2.getMinThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
    }

}
