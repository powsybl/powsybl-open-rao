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
import com.farao_community.farao.data.crac_api.cnec.AngleCnecAdder;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnAngleConstraintAdderImplTest {
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

        crac.newAngleCnec()
            .withId("cnec2stateCurativeContingency1")
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstant(Instant.CURATIVE)
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
    public void testOkPreventive() {
        RemedialAction remedialAction = remedialActionAdder.newOnAngleConstraintUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withAngleCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) remedialAction.getUsageRules().get(0);
        assertEquals(Instant.PREVENTIVE, onAngleConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), Instant.CURATIVE)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    public void testOkCurative() {
        RemedialAction remedialAction = remedialActionAdder.newOnAngleConstraintUsageRule()
            .withInstant(Instant.CURATIVE)
            .withAngleCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) remedialAction.getUsageRules().get(0);
        assertEquals(Instant.CURATIVE, onAngleConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), Instant.CURATIVE)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    public void testOutageException() {
        OnAngleConstraintAdder adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.OUTAGE).withAngleCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    public void testAbsentCnecException() {
        OnAngleConstraintAdder adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE)
            .withAngleCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    public void testNoCnecException() {
        OnAngleConstraintAdder adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE);
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    public void testNoInstantException() {
        OnAngleConstraintAdder adder = remedialActionAdder.newOnAngleConstraintUsageRule().withAngleCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    private void addCnec(String id, Instant instant) {
        AngleCnecAdder adder = crac.newAngleCnec()
            .withId(id)
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstant(instant)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-1500.).withMax(1500.).add();
        if (!instant.equals(Instant.PREVENTIVE)) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    public void testOnConstraintInstantCheck() {
        // todo : mm chose pour on flow constraint in country, dans le code
        addCnec("cnec-prev", Instant.PREVENTIVE);
        addCnec("cnec-out", Instant.OUTAGE);
        addCnec("cnec-auto", Instant.AUTO);
        addCnec("cnec-cur", Instant.CURATIVE);

        OnAngleConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE).withAngleCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE).withAngleCnec("cnec-out").add(); // ok
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE).withAngleCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE).withAngleCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.AUTO).withAngleCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.AUTO).withAngleCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.AUTO).withAngleCnec("cnec-auto").add(); // ok
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.AUTO).withAngleCnec("cnec-cur"); // nok
        assertThrows(FaraoException.class, adder::add);

        // CURATIVE RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec("cnec-cur").add(); // ok
    }
}
