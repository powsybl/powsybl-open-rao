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
import com.powsybl.openrao.data.cracapi.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.OnAngleConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.OnAngleConstraintAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.powsybl.openrao.data.cracimpl.utils.ExhaustiveCracCreation.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnAngleConstraintAdderImplTest {

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
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) usageRule;
        assertEquals(preventiveInstant, onAngleConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnAngleConstraintUsageRule()
            .withInstant(CURATIVE_INSTANT_ID)
            .withAngleCnec("cnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = networkAction.getUsageRules().iterator().next();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) usageRule;
        assertEquals(curativeInstant, onAngleConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(OUTAGE_INSTANT_ID).withAngleCnec("cnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("OnAngleConstraint usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testAbsentCnecException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID)
            .withAngleCnec("fake_cnec").withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("AngleCnec fake_cnec does not exist in crac. Consider adding it first.", exception.getMessage());
    }

    @Test
    void testNoCnecException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnAngleConstraint without a angle cnec. Please use withAngleCnec() with a non null value", exception.getMessage());
    }

    @Test
    void testNoInstantException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withAngleCnec("cnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        Exception exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnAngleConstraint without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethodException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec2stateCurativeContingency1");
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnAngleConstraint without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    private void addCnec(String id, String instantId) {
        AngleCnecAdder adder = crac.newAngleCnec()
            .withId(id)
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstant(instantId)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-1500.).withMax(1500.).add();
        if (!crac.getInstant(instantId).isPreventive()) {
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
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withAngleCnec("cnec-prev").withUsageMethod(UsageMethod.FORCED); // nok
        Exception exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withAngleCnec("cnec-out").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withAngleCnec("cnec-auto").withUsageMethod(UsageMethod.FORCED).add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withAngleCnec("cnec-cur").withUsageMethod(UsageMethod.FORCED).add(); // ok

        // CURATIVE RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'auto' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
    }
}
