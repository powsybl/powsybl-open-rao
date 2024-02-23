/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
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
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.OnVoltageConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.OnVoltageConstraintAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.powsybl.openrao.data.cracimpl.utils.ExhaustiveCracCreation.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
class OnVoltageConstraintAdderImplTest {

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

        crac.newVoltageCnec()
            .withId("cnec2stateCurativeContingency1")
            .withInstant(CURATIVE_INSTANT_ID)
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
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) usageRule;
        assertEquals(preventiveInstant, onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnVoltageConstraintUsageRule()
            .withInstant(CURATIVE_INSTANT_ID)
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) usageRule;
        assertEquals(curativeInstant, onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(OUTAGE_INSTANT_ID).withVoltageCnec("cnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("OnVoltageConstraint usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testAbsentCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID)
            .withVoltageCnec("fake_cnec").withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("VoltageCnec fake_cnec does not exist in crac. Consider adding it first.", exception.getMessage());
    }

    @Test
    void testNoCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnVoltageConstraint without a voltage cnec. Please use withVoltageCnec() with a non null value", exception.getMessage());
    }

    @Test
    void testNoInstantException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withVoltageCnec("cnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        Exception exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnVoltageConstraint without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethodException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withVoltageCnec("cnec2stateCurativeContingency1");
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnVoltageConstraint without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    private void addCnec(String id, String instantId) {
        VoltageCnecAdder adder = crac.newVoltageCnec()
            .withId(id)
            .withInstant(instantId)
            .withOperator("operator2")
            .withNetworkElement(id)
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-1500.).withMax(1500.).add();
        if (!crac.getInstant(instantId).isPreventive()) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {

        addCnec("cnec-prev", PREVENTIVE_INSTANT_ID);
        addCnec("cnec-out", OUTAGE_INSTANT_ID);
        addCnec("cnec-auto", AUTO_INSTANT_ID);
        addCnec("cnec-cur", CURATIVE_INSTANT_ID);

        OnVoltageConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withVoltageCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withVoltageCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withVoltageCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withVoltageCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withVoltageCnec("cnec-prev").withUsageMethod(UsageMethod.FORCED); // nok
        Exception exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withVoltageCnec("cnec-out").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withVoltageCnec("cnec-auto").withUsageMethod(UsageMethod.FORCED).add(); // ok
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withVoltageCnec("cnec-cur").withUsageMethod(UsageMethod.FORCED).add(); // ok

        // CURATIVE RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withVoltageCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withVoltageCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withVoltageCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'auto' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withVoltageCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
    }
}
