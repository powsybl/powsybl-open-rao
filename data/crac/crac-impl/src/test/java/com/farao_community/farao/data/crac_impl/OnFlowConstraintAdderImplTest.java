/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintAdderImplTest {
    private Crac crac;
    private NetworkActionAdder remedialActionAdder;

    @Before
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
            .withRule(BranchThresholdRule.ON_LEFT_SIDE)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .newThreshold()
            .withUnit(Unit.PERCENT_IMAX)
            .withRule(BranchThresholdRule.ON_LEFT_SIDE)
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
    public void testOkPreventive() {
        RemedialAction remedialAction = remedialActionAdder.newOnFlowConstraintUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withFlowCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) remedialAction.getUsageRules().get(0);
        assertEquals(Instant.PREVENTIVE, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), Instant.CURATIVE)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    public void testOkCurative() {
        RemedialAction remedialAction = remedialActionAdder.newOnFlowConstraintUsageRule()
            .withInstant(Instant.CURATIVE)
            .withFlowCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) remedialAction.getUsageRules().get(0);
        assertEquals(Instant.CURATIVE, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), Instant.CURATIVE)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    public void testOutageException() {
        OnFlowConstraintAdder adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.OUTAGE).withFlowCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    public void testAbsentCnecException() {
        OnFlowConstraintAdder adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE)
            .withFlowCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    public void testNoCnecException() {
        OnFlowConstraintAdder adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE);
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    public void testNoInstantException() {
        OnFlowConstraintAdder adder = remedialActionAdder.newOnFlowConstraintUsageRule().withFlowCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }
}
