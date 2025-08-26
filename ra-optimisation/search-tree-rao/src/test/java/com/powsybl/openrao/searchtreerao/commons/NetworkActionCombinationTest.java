/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionCombinationTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private NetworkAction networkAction1;
    private NetworkAction networkAction2;
    private NetworkAction networkAction3;
    private NetworkAction networkAction4;

    @BeforeEach
    public void setUp() {

        Crac crac = CracFactory.findDefault().create("crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);

        networkAction1 = crac.newNetworkAction()
            .withId("topological-action-1")
            .withOperator("operator-1")
            .newSwitchAction().withActionType(ActionType.OPEN).withNetworkElement("any-network-element").add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        networkAction2 = crac.newNetworkAction()
            .withId("topological-action-2")
            .withOperator("operator-2")
            .newTerminalsConnectionAction().withActionType(ActionType.CLOSE).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        networkAction3 = crac.newNetworkAction()
            .withId("pst-setpoint")
            .withOperator("operator-2")
            .newPhaseTapChangerTapPositionAction().withTapPosition(10).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        networkAction4 = crac.newNetworkAction()
            .withId("no-operator")
            .newPhaseTapChangerTapPositionAction().withTapPosition(10).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();
    }

    @Test
    void individualCombinationTest() {

        NetworkActionCombination naCombination = new NetworkActionCombination(networkAction1);

        assertEquals(Set.of(networkAction1), naCombination.getNetworkActionSet());
        assertEquals(Set.of("operator-1"), naCombination.getOperators());
        assertEquals("topological-action-1", naCombination.getConcatenatedId());
    }

    @Test
    void multipleCombinationTest() {

        NetworkActionCombination naCombination = new NetworkActionCombination(Set.of(networkAction1, networkAction2, networkAction3, networkAction4));

        assertEquals(Set.of(networkAction1, networkAction2, networkAction3, networkAction4),
            naCombination.getNetworkActionSet());

        assertEquals(Set.of("operator-1", "operator-2"), naCombination.getOperators());

        assertTrue(naCombination.getConcatenatedId().contains("topological-action-1"));
        assertTrue(naCombination.getConcatenatedId().contains("topological-action-2"));
        assertTrue(naCombination.getConcatenatedId().contains("pst-setpoint"));

    }
}
