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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnAngleConstraintAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private Crac crac;
    private NetworkActionAdder remedialActionAdder;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        crac.newContingency()
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withNetworkElement("FFR1AA1  FFR3AA1  1")
            .add();

        crac.newAngleCnec()
            .withId("cnec2stateCurativeContingency1")
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstant(CURATIVE_INSTANT_ID)
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
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withAngleCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) usageRule;
        assertEquals(PREVENTIVE_INSTANT_ID, onAngleConstraint.getInstant().getId());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnAngleConstraintUsageRule()
            .withInstant(CURATIVE_INSTANT_ID)
            .withAngleCnec("cnec2stateCurativeContingency1")
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) usageRule;
        assertEquals(CURATIVE_INSTANT_ID, onAngleConstraint.getInstant().getId());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(OUTAGE_INSTANT_ID).withAngleCnec("cnec2stateCurativeContingency1");
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("OnAngleConstraint usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testAbsentCnecException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID)
            .withAngleCnec("fake_cnec");
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("AngleCnec fake_cnec does not exist in crac. Consider adding it first.", exception.getMessage());
    }

    @Test
    void testNoCnecException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID);
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnAngleConstraint without a angle cnec. Please use withAngleCnec() with a non null value", exception.getMessage());
    }

    @Test
    void testNoInstantException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withAngleCnec("cnec2stateCurativeContingency1");
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnInstant without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    private void addCnec(String id, String instantId) {
        AngleCnecAdder adder = crac.newAngleCnec()
            .withId(id)
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstant(instantId)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-1500.).withMax(1500.).add();
        if (!instantId.equals(PREVENTIVE_INSTANT_ID)) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {
        // todo : mm chose pour on flow constraint in country, dans le code
        addCnec("cnec-prev", PREVENTIVE_INSTANT_ID);
        addCnec("cnec-out", OUTAGE_INSTANT_ID);
        addCnec("cnec-auto", AUTO_INSTANT_ID);
        addCnec("cnec-cur", CURATIVE_INSTANT_ID);

        OnAngleConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec-out").add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec-auto").add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withAngleCnec("cnec-prev"); // nok
        Exception exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withAngleCnec("cnec-out"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withAngleCnec("cnec-auto").add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withAngleCnec("cnec-cur").add(); // ok

        // CURATIVE RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec("cnec-prev"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec("cnec-out"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec("cnec-auto"); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'auto' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec("cnec-cur").add(); // ok
    }
}
