/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionCombinationTest {

    private NetworkAction networkAction1;
    private NetworkAction networkAction2;
    private NetworkAction networkAction3;

    @BeforeEach
    public void setUp() {

        Crac crac = CracFactory.findDefault().create("crac");

        networkAction1 = (NetworkAction) crac.newNetworkAction()
            .withId("topological-action-1")
            .withOperator("operator-1")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("any-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();

        networkAction2 = (NetworkAction) crac.newNetworkAction()
            .withId("topological-action-2")
            .withOperator("operator-2")
            .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();

        networkAction3 = (NetworkAction) crac.newNetworkAction()
            .withId("pst-setpoint")
            .withOperator("operator-2")
            .newPstSetPoint().withSetpoint(10).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();
    }

    @Test
    void individualCombinationTest() {

        NetworkActionCombination naCombination = new NetworkActionCombination(networkAction1);

        assertEquals(1, naCombination.getNetworkActionSet().size());
        assertTrue(naCombination.getNetworkActionSet().contains(networkAction1));
        assertEquals(1, naCombination.getOperators().size());
        assertTrue(naCombination.getOperators().contains("operator-1"));
        assertEquals("topological-action-1", naCombination.getConcatenatedId());
    }

    @Test
    void multipleCombinationTest() {

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
