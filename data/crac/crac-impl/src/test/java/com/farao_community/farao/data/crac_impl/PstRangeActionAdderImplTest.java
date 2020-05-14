/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Unit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstRangeActionAdderImplTest {
    private SimpleCrac crac;

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac");
    }

    @Test
    public void testAdd() {
        Crac crac1 = crac.newPstRangeAction()
                .setId("id1")
                .setUnit(Unit.DEGREE)
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId1").setName("neName1").add()
                .add();
        Crac crac2 = crac.newPstRangeAction()
                .setId("id2")
                .setUnit(Unit.DEGREE)
                .setMinValue(5.0)
                .setMaxValue(50.0)
                .newNetworkElement().setId("neId2").add()
                .add();
        assertSame(crac, crac1);
        assertSame(crac, crac2);
        assertEquals(2, crac.getRangeActions().size());

        // Verify 1st range action
        assertEquals("neId1", crac.getRangeAction("id1").getNetworkElements().iterator().next().getId());
        // TO DO : verify unit, minValue, maxValue

        // Verify 1st range action
        assertEquals("neId2", crac.getRangeAction("id2").getNetworkElements().iterator().next().getId());
        // TO DO : verify unit, minValue, maxValue
    }

    @Test(expected = FaraoException.class)
    public void testNoIdFail() {
        crac.newPstRangeAction()
                .setUnit(Unit.DEGREE)
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoUnitFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoMinValueFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setUnit(Unit.DEGREE)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoMaxValueFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setUnit(Unit.DEGREE)
                .setMinValue(0.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoNetworkElementFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setUnit(Unit.DEGREE)
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testOnlyOneNetworkElement() {
        crac.newPstRangeAction()
                .newNetworkElement().setId("neId1").setName("neName1").add()
                .newNetworkElement().setId("neId2").setName("neName2").add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        PstRangeActionAdderImpl tmp = new PstRangeActionAdderImpl(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullIdFail() {
        crac.newPstRangeAction().setId(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullUnitFail() {
        crac.newPstRangeAction().setUnit(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullMinValueFail() {
        crac.newPstRangeAction().setMinValue(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullMaxValueFail() {
        crac.newPstRangeAction().setMaxValue(null);
    }
}
