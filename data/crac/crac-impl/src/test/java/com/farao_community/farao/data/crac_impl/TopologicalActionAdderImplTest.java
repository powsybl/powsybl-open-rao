/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.network_action.TopologicalAction;
import com.farao_community.farao.data.crac_impl.CracImplFactory;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class TopologicalActionAdderImplTest {

    private NetworkActionAdder networkActionAdder;

    @Before
    public void setUp() {
        Crac crac = new CracImplFactory().create("cracId");
        networkActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator");
    }

    @Test
    public void testOk() {
        NetworkAction networkAction = networkActionAdder.newTopologicalAction()
            .withNetworkElement("branchNetworkElementId")
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        TopologicalAction topologicalAction = (TopologicalAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("branchNetworkElementId", topologicalAction.getNetworkElement().getId());
        assertEquals(ActionType.OPEN, topologicalAction.getActionType());
    }

    @Test (expected = FaraoException.class)
    public void testNoNetworkElement() {
        networkActionAdder.newTopologicalAction()
            .withActionType(ActionType.OPEN)
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoActionType() {
        networkActionAdder.newTopologicalAction()
            .withNetworkElement("branchNetworkElementId")
            .add()
            .add();
    }
}
