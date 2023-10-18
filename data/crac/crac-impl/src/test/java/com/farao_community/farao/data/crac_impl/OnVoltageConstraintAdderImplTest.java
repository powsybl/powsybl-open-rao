/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnVoltageConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnVoltageConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
class OnVoltageConstraintAdderImplTest {
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

        crac.newVoltageCnec()
            .withId("cnec2stateCurativeContingency1")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency("Contingency FR1 FR3")
            .withNetworkElement("FFR1AA1")
            .withOperator("operator2")
            .withOptimized(false)
            .withReliabilityMargin(55.0)
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-1500.).withMax(1500.).add()
            .add();

        remedialActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add();
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnVoltageConstraintUsageRule()
            .withInstantId(INSTANT_PREV.getId())
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) usageRule;
        assertEquals(INSTANT_PREV, onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), INSTANT_CURATIVE)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnVoltageConstraintUsageRule()
            .withInstantId(INSTANT_CURATIVE.getId())
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) usageRule;
        assertEquals(INSTANT_CURATIVE, onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), INSTANT_CURATIVE)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_OUTAGE.getId()).withVoltageCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testAbsentCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_PREV.getId())
            .withVoltageCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_PREV.getId());
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoInstantException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withVoltageCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    private void addCnec(String id, Instant instant) {
        VoltageCnecAdder adder = crac.newVoltageCnec()
            .withId(id)
            .withInstantId(instant.getId())
            .withOperator("operator2")
            .withNetworkElement(id)
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-1500.).withMax(1500.).add();
        if (!instant.equals(INSTANT_PREV)) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {

        addCnec("cnec-prev", INSTANT_PREV);
        addCnec("cnec-out", INSTANT_OUTAGE);
        addCnec("cnec-auto", INSTANT_AUTO);
        addCnec("cnec-cur", INSTANT_CURATIVE);

        OnVoltageConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withVoltageCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withVoltageCnec("cnec-out").add(); // ok
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withVoltageCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withVoltageCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withVoltageCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withVoltageCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withVoltageCnec("cnec-auto").add(); // ok
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_AUTO.getId()).withVoltageCnec("cnec-cur"); // nok
        assertThrows(FaraoException.class, adder::add);

        // CURATIVE RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withVoltageCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withVoltageCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withVoltageCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withVoltageCnec("cnec-cur").add(); // ok
    }
}
