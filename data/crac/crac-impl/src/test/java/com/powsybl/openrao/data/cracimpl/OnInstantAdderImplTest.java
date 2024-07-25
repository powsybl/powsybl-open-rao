/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.OnInstant;
import com.powsybl.openrao.data.cracapi.usagerule.OnInstantAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class OnInstantAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private Crac crac;
    private NetworkActionAdder remedialActionAdder;
    private Instant preventiveInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        crac.newContingency()
                .withId("contingencyId")
                .withContingencyElement("networkElementId", ContingencyElementType.LINE)
                .add();
        remedialActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add();
    }

    @Test
    void testOkPreventive() {
        RemedialAction remedialAction = remedialActionAdder.newOnInstantUsageRule()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnInstant);
        assertEquals(preventiveInstant, usageRule.getInstant());
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction remedialAction = remedialActionAdder.newOnInstantUsageRule()
                .withInstant(CURATIVE_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnInstant);
        assertEquals(curativeInstant, usageRule.getInstant());
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
    }

    @Test
    void testNoInstant() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, onInstantAdder::add);
        assertEquals("Cannot add OnInstant without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethod() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withInstant(PREVENTIVE_INSTANT_ID);
        OpenRaoException exception = assertThrows(OpenRaoException.class, onInstantAdder::add);
        assertEquals("Cannot add OnInstant without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    @Test
    void testOutageInstant() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withInstant(OUTAGE_INSTANT_ID)
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, onInstantAdder::add);
        assertEquals("OnInstant usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }
}
