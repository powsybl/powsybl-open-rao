/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.CracImplFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class NetworkActionsCompatibilityCheckerTest {

    @Test
    void filterOutIncompatibleRemedialActions() {
        Crac crac = new CracImplFactory().create("crac", "crac")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);

        NetworkAction appliedRemedialAction1 = crac.newNetworkAction()
            .withId("applied-remedial-action-1")
            .newTopologicalAction()
            .withNetworkElement("switch-1")
            .withActionType(ActionType.OPEN)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        NetworkAction appliedRemedialAction2 = crac.newNetworkAction()
            .withId("applied-remedial-action-2")
            .newTopologicalAction()
            .withNetworkElement("switch-2")
            .withActionType(ActionType.CLOSE)
            .add()
            .newInjectionSetPoint()
            .withNetworkElement("generator-1")
            .withSetpoint(100d)
            .withUnit(Unit.MEGAWATT)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        NetworkAction availableRemedialAction1 = crac.newNetworkAction()
            .withId("available-remedial-action-1")
            .newTopologicalAction()
            .withNetworkElement("switch-2")
            .withActionType(ActionType.OPEN)
            .add()
            .newPstSetPoint()
            .withNetworkElement("pst-1")
            .withSetpoint(1)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        NetworkAction availableRemedialAction2 = crac.newNetworkAction()
            .withId("available-remedial-action-2")
            .newInjectionSetPoint()
            .withNetworkElement("generator-2")
            .withSetpoint(75d)
            .withUnit(Unit.MEGAWATT)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        NetworkAction availableRemedialAction3 = crac.newNetworkAction()
            .withId("available-remedial-action-3")
            .newInjectionSetPoint()
            .withNetworkElement("generator-1")
            .withSetpoint(100d)
            .withUnit(Unit.MEGAWATT)
            .add()
            .newTopologicalAction()
            .withNetworkElement("switch-3")
            .withActionType(ActionType.CLOSE)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        NetworkAction availableRemedialAction4 = crac.newNetworkAction()
            .withId("available-remedial-action-4")
            .newSwitchPair()
            .withSwitchToOpen("switch-2")
            .withSwitchToClose("switch-1")
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(
            Set.of(availableRemedialAction2, availableRemedialAction3, availableRemedialAction4),
            NetworkActionsCompatibilityChecker.filterOutIncompatibleRemedialActions(
                Set.of(appliedRemedialAction1, appliedRemedialAction2),
                Set.of(availableRemedialAction1, availableRemedialAction2, availableRemedialAction3, availableRemedialAction4)
            )
        );
    }
}
