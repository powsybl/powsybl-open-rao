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
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.AngleCnecAdder;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraintAdder;
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
class OnAngleConstraintAdderImplTest {
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

        crac.newAngleCnec()
            .withId("cnec2stateCurativeContingency1")
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency("Contingency FR1 FR3")
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-1500.).withMax(1500.).add()
            .add();

        remedialActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add();
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnAngleConstraintUsageRule()
            .withInstantId(INSTANT_PREV.getId())
            .withAngleCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) usageRule;
        assertEquals(INSTANT_PREV, onAngleConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), INSTANT_CURATIVE)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnAngleConstraintUsageRule()
            .withInstantId(INSTANT_CURATIVE.getId())
            .withAngleCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) usageRule;
        assertEquals(INSTANT_CURATIVE, onAngleConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), INSTANT_CURATIVE)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_OUTAGE.getId()).withAngleCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testAbsentCnecException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_PREV.getId())
            .withAngleCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoCnecException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_PREV.getId());
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoInstantException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withAngleCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    private void addCnec(String id, Instant instant) {
        AngleCnecAdder adder = crac.newAngleCnec()
            .withId(id)
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstantId(instant.getId())
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-1500.).withMax(1500.).add();
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

        OnAngleConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withAngleCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withAngleCnec("cnec-out").add(); // ok
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withAngleCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withAngleCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withAngleCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withAngleCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withAngleCnec("cnec-auto").add(); // ok
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withAngleCnec("cnec-cur"); // nok
        assertThrows(FaraoException.class, adder::add);

        // CURATIVE RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withAngleCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withAngleCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withAngleCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withAngleCnec("cnec-cur").add(); // ok
    }
}
