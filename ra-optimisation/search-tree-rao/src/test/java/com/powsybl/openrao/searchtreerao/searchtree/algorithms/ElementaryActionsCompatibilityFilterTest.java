/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class ElementaryActionsCompatibilityFilterTest {
    @Test
    void removeIncompatibleCombinations() {
        Crac crac = new CracImplFactory().create("crac", "crac")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);

        NetworkAction appliedRemedialAction1 = crac.newNetworkAction()
            .withId("applied-remedial-action-1")
            .withOperator("FR")
            .newSwitchAction()
            .withNetworkElement("switch-1")
            .withActionType(ActionType.OPEN)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();

        NetworkAction appliedRemedialAction2 = crac.newNetworkAction()
            .withId("applied-remedial-action-2")
            .withOperator("FR")
            .newSwitchAction()
            .withNetworkElement("switch-2")
            .withActionType(ActionType.CLOSE)
            .add()
            .newGeneratorAction()
            .withNetworkElement("generator-1")
            .withActivePowerValue(100d)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();

        NetworkAction availableRemedialAction1 = crac.newNetworkAction()
            .withId("available-remedial-action-1")
            .withOperator("FR")
            .newSwitchAction()
            .withNetworkElement("switch-2")
            .withActionType(ActionType.OPEN)
            .add()
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("pst-1")
            .withTapPosition(1)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();

        NetworkAction availableRemedialAction2 = crac.newNetworkAction()
            .withId("available-remedial-action-2")
            .withOperator("FR")
            .newGeneratorAction()
            .withNetworkElement("generator-2")
            .withActivePowerValue(75d)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();

        NetworkAction availableRemedialAction3 = crac.newNetworkAction()
            .withId("available-remedial-action-3")
            .withOperator("FR")
            .newGeneratorAction()
            .withNetworkElement("generator-1")
            .withActivePowerValue(100d)
            .add()
            .newSwitchAction()
            .withNetworkElement("switch-3")
            .withActionType(ActionType.CLOSE)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();

        NetworkAction availableRemedialAction4 = crac.newNetworkAction()
            .withId("available-remedial-action-4")
            .withOperator("FR")
            .newSwitchPair()
            .withSwitchToOpen("switch-2")
            .withSwitchToClose("switch-1")
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();

        NetworkActionCombination networkActionCombination1 = new NetworkActionCombination(Set.of(availableRemedialAction1, availableRemedialAction2));
        NetworkActionCombination networkActionCombination2 = new NetworkActionCombination(Set.of(availableRemedialAction3));
        NetworkActionCombination networkActionCombination3 = new NetworkActionCombination(Set.of(availableRemedialAction4));
        NetworkActionCombination networkActionCombination4 = new NetworkActionCombination(Set.of(availableRemedialAction2, availableRemedialAction3));

        ElementaryActionsCompatibilityFilter naFilter = new ElementaryActionsCompatibilityFilter();
        Set<NetworkActionCombination> naCombinations = Set.of(networkActionCombination1, networkActionCombination2, networkActionCombination3, networkActionCombination4);

        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Set.of(appliedRemedialAction1, appliedRemedialAction2));

        assertEquals(
            Set.of(networkActionCombination2, networkActionCombination3, networkActionCombination4),
            naFilter.filter(naCombinations, previousLeaf)
        );
    }
}
