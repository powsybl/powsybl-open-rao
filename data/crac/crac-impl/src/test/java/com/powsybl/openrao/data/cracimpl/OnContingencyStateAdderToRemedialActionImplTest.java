/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyState;
import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyStateAdderToRemedialAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnContingencyStateAdderToRemedialActionImplTest {

    private Crac crac;
    private Contingency contingency;
    private RemedialAction<?> remedialAction = null;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant("preventive");
        outageInstant = crac.getInstant("outage");
        curativeInstant = crac.getInstant("curative");
        ((CracImpl) crac).addPreventiveState();

        contingency = crac.newContingency()
            .withId("contingencyId")
            .withContingencyElement("networkElementId", ContingencyElementType.LINE)
            .add();

        ((CracImpl) crac).addState(contingency, curativeInstant);

        remedialAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
            .add();
    }

    @Test
    void testOk() {
        remedialAction.newOnStateUsageRule().withState(crac.getState(contingency, curativeInstant)).withUsageMethod(UsageMethod.FORCED).add();

        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();
        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals(curativeInstant, ((OnContingencyState) usageRule).getState().getInstant());
        assertEquals(contingency, ((OnContingencyState) usageRule).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.FORCED, usageRule.getUsageMethod());
    }

    @Test
    void testOkPreventive() {
        remedialAction.newOnStateUsageRule().withState(crac.getPreventiveState()).withUsageMethod(UsageMethod.FORCED).add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals(preventiveInstant, ((OnContingencyState) usageRule).getState().getInstant());
        assertEquals(UsageMethod.FORCED, usageRule.getUsageMethod());
    }

    @Test
    void testNoState() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withUsageMethod(UsageMethod.FORCED);
        OpenRaoException exception = assertThrows(OpenRaoException.class, onStateAdderToRemedialAction::add);
        assertEquals("Cannot add OnState without a state. Please use withState() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethod() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(crac.getState(contingency, curativeInstant));
        OpenRaoException exception = assertThrows(OpenRaoException.class, onStateAdderToRemedialAction::add);
        assertEquals("Cannot add OnState without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    @Test
    void testPreventiveInstantNotForced() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(crac.getPreventiveState())
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, onStateAdderToRemedialAction::add);
        assertEquals("OnContingencyState usage rules are not allowed for PREVENTIVE instant except when FORCED. Please use newOnInstantUsageRule() instead.", exception.getMessage());
    }

    @Test
    void testOutageInstant() {
        State outageState = ((CracImpl) crac).addState(contingency, outageInstant);
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(outageState)
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, onStateAdderToRemedialAction::add);
        assertEquals("OnContingencyState usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }
}
