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
import com.farao_community.farao.data.crac_api.cnec.AngleCnecAdder;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnAngleConstraintAdderImplTest {
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

        crac.newAngleCnec()
            .withId("cnec2stateCurativeContingency1")
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstant(crac.getInstant(Instant.Kind.CURATIVE))
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
        NetworkAction networkAction = remedialActionAdder.newOnAngleConstraintUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.PREVENTIVE))
            .withAngleCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(networkAction.getUsageRules().get(0) instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) networkAction.getUsageRules().get(0);
        assertEquals(crac.getInstant(Instant.Kind.PREVENTIVE), onAngleConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), crac.getInstant(Instant.Kind.CURATIVE))));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        NetworkAction networkAction = remedialActionAdder.newOnAngleConstraintUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.CURATIVE))
            .withAngleCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(networkAction.getUsageRules().get(0) instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) networkAction.getUsageRules().get(0);
        assertEquals(crac.getInstant(Instant.Kind.CURATIVE), onAngleConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onAngleConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), crac.getInstant(Instant.Kind.CURATIVE))));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.OUTAGE)).withAngleCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testAbsentCnecException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE))
            .withAngleCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoCnecException() {
        OnAngleConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE));
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
            .withInstant(instant)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-1500.).withMax(1500.).add();
        if (!instant.isPreventive()) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {
        // todo : mm chose pour on flow constraint in country, dans le code
        addCnec("cnec-prev", crac.getInstant(Instant.Kind.PREVENTIVE));
        addCnec("cnec-out", crac.getInstant(Instant.Kind.OUTAGE));
        addCnec("cnec-auto", crac.getInstant(Instant.Kind.AUTO));
        addCnec("cnec-cur", crac.getInstant(Instant.Kind.CURATIVE));

        OnAngleConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withAngleCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withAngleCnec("cnec-out").add(); // ok
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withAngleCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withAngleCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.AUTO)).withAngleCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.AUTO)).withAngleCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.AUTO)).withAngleCnec("cnec-auto").add(); // ok
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.AUTO)).withAngleCnec("cnec-cur"); // nok
        assertThrows(FaraoException.class, adder::add);

        // CURATIVE RA
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withAngleCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withAngleCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withAngleCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnAngleConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withAngleCnec("cnec-cur").add(); // ok
    }
}
