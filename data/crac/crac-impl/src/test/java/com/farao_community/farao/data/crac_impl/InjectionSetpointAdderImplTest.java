/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionSetpointAdderImplTest {

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
        NetworkAction networkAction = networkActionAdder.newInjectionSetPoint()
            .withNetworkElement("groupNetworkElementId")
            .withSetpoint(100.)
            .add()
            .add();

        InjectionSetpoint injectionSetpoint = (InjectionSetpoint) networkAction.getElementaryActions().iterator().next();
        assertEquals("groupNetworkElementId", injectionSetpoint.getNetworkElement().getId());
        assertEquals(100., injectionSetpoint.getSetpoint(), 1e-3);

        // check that network element have been added to CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("groupNetworkElementId"));
    }

    @Test (expected = FaraoException.class)
    public void testNoNetworkElement() {
        networkActionAdder.newInjectionSetPoint()
            .withSetpoint(100.)
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoSetpoint() {
        networkActionAdder.newInjectionSetPoint()
            .withNetworkElement("groupNetworkElementId")
            .add()
            .add();
    }
}
