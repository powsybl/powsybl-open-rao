/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ComplexContingencyAdderTest {

    private Crac crac;

    @Before
    public void setUp() {
        crac = (new SimpleCracFactory()).create("test-crac");
    }

    @Test
    public void testAddContingencies() {
        Contingency con1 = crac.newContingency()
                .setId("conId1")
                .setName("conName1")
                .newNetworkElement()
                .setId("neId1")
                .setName("neName1")
                .add()
                .add();
        Contingency con2 = crac.newContingency()
                .newNetworkElement()
                .setId("neId2-1")
                .add()
                .newNetworkElement()
                .setId("neId2-2")
                .setName("neName2-2")
                .add()
                .setId("conId2")
                .add();
        assertEquals(2, crac.getContingencies().size());

        // Verify 1st contingency content
        assertEquals("conName1", crac.getContingency("conId1").getName());
        assertEquals(1, crac.getContingency("conId1").getNetworkElements().size());
        assertEquals("neId1", crac.getContingency("conId1").getNetworkElements().iterator().next().getId());
        assertEquals("neName1", crac.getContingency("conId1").getNetworkElements().iterator().next().getName());
        assertEquals(con1.getId(), crac.getContingency("conId1").getId());

        // Verify 2nd contingency content
        assertEquals("conId2", crac.getContingency("conId2").getName());
        assertEquals(2, crac.getContingency("conId2").getNetworkElements().size());
        assertEquals(con2.getId(), crac.getContingency("conId2").getId());
        Iterator<NetworkElement> iter = crac.getContingency("conId2").getNetworkElements().iterator();
        NetworkElement ne1 = iter.next();
        NetworkElement ne2 = iter.next();
        // Order the network elements from the Set
        if (ne2.getId().compareTo(ne1.getId()) < 0) {
            NetworkElement tmp = ne2;
            ne2 = ne1;
            ne1 = tmp;
        }
        assertEquals("neId2-1", ne1.getId());
        assertEquals("neId2-1", ne1.getName());
        assertEquals("neId2-2", ne2.getId());
        assertEquals("neName2-2", ne2.getName());
    }

    @Test(expected = FaraoException.class)
    public void testAddWithNoIdFail() {
        crac.newContingency()
                .setName("conName1")
                .newNetworkElement()
                .setId("neId1")
                .setName("neName1")
                .add()
                .add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        ComplexContingencyAdder tmp = new ComplexContingencyAdder(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullIdFail() {
        crac.newContingency().setId(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNameFail() {
        crac.newContingency().setName(null);
    }
}
