/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.ThreeSides;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NETWORK;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class IneffectiveActionsFilterTests {
    @Test
    void testFilter() {
        // Following actions have no impact on network
        TerminalsConnectionAction terminalsConnectionAction12 = new TerminalsConnectionAction("tca-12", "FFR1AA1  FFR2AA1  1", ThreeSides.ONE, false);
        TerminalsConnectionAction terminalsConnectionAction23 = new TerminalsConnectionAction("tca-23", "FFR2AA1  FFR3AA1  1", ThreeSides.ONE, false);
        // Following actions have impact on network
        TerminalsConnectionAction terminalsConnectionAction13 = new TerminalsConnectionAction("tca-13", "FFR1AA1  FFR3AA1  1", ThreeSides.ONE, true);
        GeneratorAction generatorAction = new GeneratorActionBuilder()
            .withId("generator-action")
            .withGeneratorId("FFR1AA1 _generator")
            .withActivePowerValue(250.)
            .withActivePowerRelativeValue(false)
            .build();

        NetworkAction networkAction1 = mockNetworkAction(Set.of(terminalsConnectionAction12), 100.0);
        NetworkAction networkAction2 = mockNetworkAction(Set.of(terminalsConnectionAction23), 17.0);
        NetworkAction networkAction3 = mockNetworkAction(Set.of(terminalsConnectionAction13, generatorAction), 2000.0);
        NetworkAction networkAction4 = mockNetworkAction(Set.of(terminalsConnectionAction13, generatorAction), 42.0);
        NetworkActionCombination networkActionCombination1 = new NetworkActionCombination(Set.of(networkAction1));
        NetworkActionCombination networkActionCombination2 = new NetworkActionCombination(Set.of(networkAction3));
        NetworkActionCombination networkActionCombination3 = new NetworkActionCombination(Set.of(networkAction2, networkAction4));

        // combination 1 should be filtered out because it has no impact on network
        // combination 3 should be kept because it has impact on network
        // combination 2 should be filtered out because it has the exact same impact as combination 3 but is more expensive
        IneffectiveActionsFilter ineffectiveActionsFilter = new IneffectiveActionsFilter(NETWORK);
        assertEquals(Set.of(networkActionCombination3), ineffectiveActionsFilter.filter(Set.of(networkActionCombination1, networkActionCombination2, networkActionCombination3), null, ReportNode.NO_OP));
    }

    private static NetworkAction mockNetworkAction(Set<Action> elementaryActions, double activationCost) {
        NetworkAction networkAction = Mockito.mock(NetworkAction.class);
        Mockito.when(networkAction.getElementaryActions()).thenReturn(elementaryActions);
        Mockito.when(networkAction.getActivationCost()).thenReturn(Optional.of(activationCost));
        return networkAction;
    }
}
