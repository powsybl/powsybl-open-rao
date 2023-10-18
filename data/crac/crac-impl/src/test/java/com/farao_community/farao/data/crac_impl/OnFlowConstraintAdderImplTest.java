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
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintAdderImplTest {
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);
    private Crac crac;
    private NetworkActionAdder remedialActionAdder;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        crac.addInstant(INSTANT_PREV);
        crac.addInstant(INSTANT_OUTAGE);
        crac.addInstant(INSTANT_AUTO);
        crac.addInstant(INSTANT_CURATIVE);

        crac.newContingency()
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withNetworkElement("FFR1AA1  FFR3AA1  1")
            .add();

        crac.newFlowCnec()
            .withId("cnec2stateCurativeContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstantId(INSTANT_CURATIVE.getId())
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
        RemedialAction remedialAction = remedialActionAdder.newOnFlowConstraintUsageRule()
            .withInstantId(INSTANT_PREV.getId())
            .withFlowCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) usageRule;
        assertEquals(INSTANT_PREV, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), INSTANT_CURATIVE)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction remedialAction = remedialActionAdder.newOnFlowConstraintUsageRule()
            .withInstantId(INSTANT_CURATIVE.getId())
            .withFlowCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) usageRule;
        assertEquals(INSTANT_CURATIVE, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), INSTANT_CURATIVE)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnFlowConstraintAdder adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_OUTAGE.getId()).withFlowCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testAbsentCnecException() {
        OnFlowConstraintAdder adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_PREV.getId())
            .withFlowCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoCnecException() {
        OnFlowConstraintAdder adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_PREV.getId());
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoInstantException() {
        OnFlowConstraintAdder adder = remedialActionAdder.newOnFlowConstraintUsageRule().withFlowCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    private void addCnec(String id, Instant instant) {
        FlowCnecAdder adder = crac.newFlowCnec()
            .withId(id)
            .withNetworkElement(id)
            .withInstantId(instant.getId())
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(Side.LEFT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(Side.LEFT).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.);
        if (!instant.equals(INSTANT_PREV)) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {
        // todo : mm chose pour on flow constraint in country, dans le code
        addCnec("cnec-prev", INSTANT_PREV);
        addCnec("cnec-out", INSTANT_OUTAGE);
        addCnec("cnec-auto", INSTANT_AUTO);
        addCnec("cnec-cur", INSTANT_CURATIVE);

        OnFlowConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withFlowCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withFlowCnec("cnec-out").add(); // ok
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withFlowCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withFlowCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withFlowCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withFlowCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withFlowCnec("cnec-auto").add(); // ok
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withFlowCnec("cnec-cur"); // nok
        assertThrows(FaraoException.class, adder::add);

        // CURATIVE RA
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withFlowCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withFlowCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withFlowCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withFlowCnec("cnec-cur").add(); // ok
    }
}
