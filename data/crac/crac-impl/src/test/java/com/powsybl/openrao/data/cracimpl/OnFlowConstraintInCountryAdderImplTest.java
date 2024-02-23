/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.powsybl.openrao.data.cracimpl.utils.ExhaustiveCracCreation.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintInCountryAdderImplTest {

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
        RemedialAction<?> remedialAction = remedialActionAdder.newOnFlowConstraintInCountryUsageRule()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withCountry(Country.FR)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().iterator().next() instanceof OnFlowConstraintInCountry);
        OnFlowConstraintInCountry onFlowConstraint = (OnFlowConstraintInCountry) remedialAction.getUsageRules().iterator().next();
        assertEquals(preventiveInstant, onFlowConstraint.getInstant());
        // Default UsageMethod is AVAILABLE
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertEquals(Country.FR, onFlowConstraint.getCountry());
    }

    @Test
    void testOutageException() {
        OnFlowConstraintInCountryAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(OUTAGE_INSTANT_ID).withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("OnFlowConstraintInCountry usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testAbsentCountryException() {
        OnFlowConstraintInCountryAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnFlowConstraintInCountry without a country. Please use withCountry() with a non null value", exception.getMessage());
    }

    @Test
    void testNoInstantException() {
        OnFlowConstraintInCountryAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE);
        Exception exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnFlowConstraintInCountry without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethodException() {
        OnFlowConstraintInCountryAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCountry(Country.FR);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnFlowConstraintInCountry without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }
}
