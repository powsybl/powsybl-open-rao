/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElementAdder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkElementAdderImplTest {

    private Crac crac;

    @Before
    public void setUp() {
        crac = new SimpleCracFactory().create("test-crac", Collections.emptySet());
    }

    @Test
    public void testCracAddNetworkElement() {
        crac.newNetworkElement()
                .setId("neID")
                .setName("neName")
                .add();
        crac.newNetworkElement()
                .setId("neID2")
                .add();
        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertEquals("neName", crac.getNetworkElement("neID").getName());
        assertNotNull(crac.getNetworkElement("neID2"));
        assertEquals("neID2", crac.getNetworkElement("neID2").getName());
    }

    @Test(expected = FaraoException.class)
    public void testCracAddNetworkElementNoIdFail() {
        crac.newNetworkElement()
                .setName("neName")
                .add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        NetworkElementAdder tmp = new NetworkElementAdderImpl(null);
    }
}
