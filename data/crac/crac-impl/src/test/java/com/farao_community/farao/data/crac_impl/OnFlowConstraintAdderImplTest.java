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
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintAdderImplTest {
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
        NetworkAction remedialAction = remedialActionAdder.newOnFlowConstraintUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withFlowCnec("cnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) usageRule;
        assertEquals(Instant.PREVENTIVE, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), Instant.CURATIVE)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        NetworkAction remedialAction = remedialActionAdder.newOnFlowConstraintUsageRule()
            .withInstant(Instant.CURATIVE)
            .withFlowCnec("cnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.UNAVAILABLE)
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) usageRule;
        assertEquals(Instant.CURATIVE, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.UNAVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.UNAVAILABLE, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), Instant.CURATIVE)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.OUTAGE).withFlowCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testAbsentCnecException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE)
            .withFlowCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoCnecException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoInstantException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withFlowCnec("cnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoUsageMethodException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withFlowCnec("cnec2stateCurativeContingency1").withInstant(Instant.PREVENTIVE);
        assertThrows(FaraoException.class, adder::add);
    }

    private void addCnec(String id, Instant instant) {
        FlowCnecAdder adder = crac.newFlowCnec()
            .withId(id)
            .withNetworkElement(id)
            .withInstant(instant)
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(Side.LEFT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(Side.LEFT).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.);
        if (!instant.equals(Instant.PREVENTIVE)) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {
        // todo : mm chose pour on flow constraint in country, dans le code
        addCnec("cnec-prev", Instant.PREVENTIVE);
        addCnec("cnec-out", Instant.OUTAGE);
        addCnec("cnec-auto", Instant.AUTO);
        addCnec("cnec-cur", Instant.CURATIVE);

        OnFlowConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE); // nok
        assertThrows(FaraoException.class, adder::add);

        // CURATIVE RA
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.CURATIVE).withFlowCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.CURATIVE).withFlowCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.CURATIVE).withFlowCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(Instant.CURATIVE).withFlowCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
    }
}
