/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.TapConvention;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.network_action.PstSetpoint;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstSetpointAdderImplTest {

    private Crac crac;
    private NetworkActionAdder networkActionAdder;

    @Before
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        networkActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator");
    }

    @Test
    public void testOk() {

        NetworkAction networkAction = networkActionAdder.newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(0)
            .withTapConvention(TapConvention.STARTS_AT_ONE)
            .add()
            .add();

        PstSetpoint pstSetpoint = (PstSetpoint) networkAction.getElementaryActions().iterator().next();
        assertEquals("pstNetworkElementId", pstSetpoint.getNetworkElement().getId());
        assertEquals(0, pstSetpoint.getSetpoint(), 1e-3);
        assertEquals(TapConvention.STARTS_AT_ONE, pstSetpoint.getTapConvention());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("pstNetworkElementId"));
    }

    @Test (expected = FaraoException.class)
    public void testNoNetworkElement() {
        networkActionAdder.newPstSetPoint()
            .withSetpoint(0)
            .withTapConvention(TapConvention.STARTS_AT_ONE)
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoSetpoint() {
        networkActionAdder.newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withTapConvention(TapConvention.STARTS_AT_ONE)
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoRangeDefinition() {
        networkActionAdder.newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(0)
            .add()
            .add();
    }
}
