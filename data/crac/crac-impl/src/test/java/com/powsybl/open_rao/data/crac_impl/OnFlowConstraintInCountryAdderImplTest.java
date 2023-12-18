/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.InstantKind;
import com.powsybl.open_rao.data.crac_api.RemedialAction;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.network_action.ActionType;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkActionAdder;
import com.powsybl.open_rao.data.crac_api.usage_rule.*;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintInCountryAdderImplTest {
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
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withNetworkElement("FFR1AA1  FFR3AA1  1")
            .add();

        crac.newFlowCnec()
            .withId("cnec2stateCurativeContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(Side.LEFT)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .newThreshold()
            .withUnit(Unit.PERCENT_IMAX)
            .withSide(Side.LEFT)
            .withMin(-0.3)
            .withMax(0.3)
            .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        remedialActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add();
    }

    @Test
    void testOkPreventive() {
        RemedialAction remedialAction = remedialActionAdder.newOnFlowConstraintInCountryUsageRule()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withCountry(Country.FR)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().iterator().next() instanceof OnFlowConstraintInCountry);
        OnFlowConstraintInCountry onFlowConstraint = (OnFlowConstraintInCountry) remedialAction.getUsageRules().iterator().next();
        assertEquals(preventiveInstant, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertEquals(Country.FR, onFlowConstraint.getCountry());
    }

    @Test
    void testOutageException() {
        OnFlowConstraintInCountryAdder adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(OUTAGE_INSTANT_ID).withCountry(Country.FR);
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("OnFlowConstraintInCountry usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testAbsentCountryException() {
        OnFlowConstraintInCountryAdder adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(PREVENTIVE_INSTANT_ID);
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnFlowConstraintInCountry without a country. Please use withCountry() with a non null value", exception.getMessage());
    }

    @Test
    void testNoInstantException() {
        OnFlowConstraintInCountryAdder adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withCountry(Country.FR);
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnInstant without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }
}
