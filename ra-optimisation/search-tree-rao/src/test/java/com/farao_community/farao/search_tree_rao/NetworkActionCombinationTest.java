/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionCombinationTest {

    private NetworkAction networkAction1;
    private NetworkAction networkAction2;
    private NetworkAction networkAction3;

    @Before
    public void setUp() {

        Crac crac = CracFactory.findDefault().create("crac");

        networkAction1 = crac.newNetworkAction()
            .withId("topological-action-1")
            .withOperator("operator-1")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("any-network-element").add()
            .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();

        networkAction2 = crac.newNetworkAction()
            .withId("topological-action-2")
            .withOperator("operator-2")
            .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("any-other-network-element").add()
            .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();

        networkAction3 = crac.newNetworkAction()
            .withId("pst-setpoint")
            .withOperator("operator-2")
            .newPstSetPoint().withSetpoint(10).withNetworkElement("any-other-network-element").add()
            .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();
    }

    @Test
    public void individualCombinationTest() {

        NetworkActionCombination naCombination = new NetworkActionCombination(networkAction1);

        assertEquals(1, naCombination.getNetworkActionSet().size());
        assertTrue(naCombination.getNetworkActionSet().contains(networkAction1));
        assertEquals(1, naCombination.getOperators().size());
        assertTrue(naCombination.getOperators().contains("operator-1"));
        assertEquals("topological-action-1", naCombination.getConcatenatedId());
    }

    @Test
    public void multipleCombinationTest() {

        NetworkActionCombination naCombination = new NetworkActionCombination(Set.of(networkAction1, networkAction2, networkAction3));

        assertEquals(3, naCombination.getNetworkActionSet().size());
        assertTrue(naCombination.getNetworkActionSet().contains(networkAction1));
        assertTrue(naCombination.getNetworkActionSet().contains(networkAction2));
        assertTrue(naCombination.getNetworkActionSet().contains(networkAction3));

        assertEquals(2, naCombination.getOperators().size());
        assertTrue(naCombination.getOperators().contains("operator-1"));
        assertTrue(naCombination.getOperators().contains("operator-2"));

        assertTrue(naCombination.getConcatenatedId().contains("topological-action-1"));
        assertTrue(naCombination.getConcatenatedId().contains("topological-action-2"));
        assertTrue(naCombination.getConcatenatedId().contains("pst-setpoint"));

    }
}
