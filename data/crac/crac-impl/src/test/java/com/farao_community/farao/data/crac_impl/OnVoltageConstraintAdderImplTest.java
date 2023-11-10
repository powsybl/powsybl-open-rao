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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
class OnVoltageConstraintAdderImplTest {
    private Crac crac;
    private Instant curativeInstant;
    private NetworkActionAdder remedialActionAdder;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
        curativeInstant = crac.getInstant("curative");

        crac.newContingency()
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withNetworkElement("FFR1AA1  FFR3AA1  1")
            .add();

        crac.newVoltageCnec()
            .withId("cnec2stateCurativeContingency1")
            .withInstant("curative")
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
            .withInstant("preventive")
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) usageRule;
        assertEquals("preventive", onVoltageConstraint.getInstant().getId());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnVoltageConstraintUsageRule()
            .withInstant("curative")
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) usageRule;
        assertEquals(curativeInstant, onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("outage").withVoltageCnec("cnec2stateCurativeContingency1");
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("OnVoltageConstraint usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testAbsentCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("preventive")
            .withVoltageCnec("fake_cnec");
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("VoltageCnec fake_cnec does not exist in crac. Consider adding it first.", exception.getMessage());
    }

    @Test
    void testNoCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("preventive");
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnVoltageConstraint without a voltage cnec. Please use withVoltageCnec() with a non null value", exception.getMessage());
    }

    @Test
    void testNoInstantException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withVoltageCnec("cnec2stateCurativeContingency1");
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnInstant without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    private void addCnec(String id, String instantId) {
        VoltageCnecAdder adder = crac.newVoltageCnec()
            .withId(id)
            .withInstant(instantId)
            .withOperator("operator2")
            .withNetworkElement(id)
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-1500.).withMax(1500.).add();
        if (!instantId.equals("preventive")) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {

        addCnec("cnec-prev", "preventive");
        addCnec("cnec-out", "outage");
        addCnec("cnec-auto", "auto");
        addCnec("cnec-cur", "curative");

        OnVoltageConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("preventive").withVoltageCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("preventive").withVoltageCnec("cnec-out").add(); // ok
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("preventive").withVoltageCnec("cnec-auto"); // nok
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant PREVENTIVE on a CNEC constraint at instant AUTO are not allowed.", exception.getMessage());
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("preventive").withVoltageCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("auto").withVoltageCnec("cnec-prev"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant AUTO on a CNEC constraint at instant PREVENTIVE are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("auto").withVoltageCnec("cnec-out"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant AUTO on a CNEC constraint at instant OUTAGE are not allowed.", exception.getMessage());
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("auto").withVoltageCnec("cnec-auto").add(); // ok
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("auto").withVoltageCnec("cnec-cur"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant AUTO on a CNEC constraint at instant CURATIVE are not allowed.", exception.getMessage());

        // CURATIVE RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("curative").withVoltageCnec("cnec-prev"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant CURATIVE on a CNEC constraint at instant PREVENTIVE are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("curative").withVoltageCnec("cnec-out"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant CURATIVE on a CNEC constraint at instant OUTAGE are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("curative").withVoltageCnec("cnec-auto"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant CURATIVE on a CNEC constraint at instant AUTO are not allowed.", exception.getMessage());
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant("curative").withVoltageCnec("cnec-cur").add(); // ok
    }
}
