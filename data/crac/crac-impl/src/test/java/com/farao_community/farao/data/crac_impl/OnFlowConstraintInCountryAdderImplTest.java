/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintInCountryAdderImplTest {
    private Crac crac;
    private NetworkActionAdder remedialActionAdder;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");

        crac.newContingency()
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withNetworkElement("FFR1AA1  FFR3AA1  1")
            .add();

        crac.newFlowCnec()
            .withId("cnec2stateCurativeContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(Instant.CURATIVE)
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
            .withInstant(Instant.PREVENTIVE)
            .withCountry(Country.FR)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().iterator().next() instanceof OnFlowConstraintInCountry);
        OnFlowConstraintInCountry onFlowConstraint = (OnFlowConstraintInCountry) remedialAction.getUsageRules().iterator().next();
        assertEquals(Instant.PREVENTIVE, onFlowConstraint.getInstant());
        // Default UsageMethod is AVAILABLE
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), Instant.CURATIVE)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertEquals(Country.FR, onFlowConstraint.getCountry());
    }

    @Test
    void testOutageException() {
        OnFlowConstraintInCountryAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(Instant.OUTAGE).withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testAbsentCountryException() {
        OnFlowConstraintInCountryAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoInstantException() {
        OnFlowConstraintInCountryAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoUsageMethodException() {
        OnFlowConstraintInCountryAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(Instant.PREVENTIVE).withCountry(Country.FR);
        assertThrows(FaraoException.class, adder::add);
    }
}
