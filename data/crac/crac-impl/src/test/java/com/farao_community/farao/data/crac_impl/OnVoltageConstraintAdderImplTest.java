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
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnVoltageConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnVoltageConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
class OnVoltageConstraintAdderImplTest {
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

        crac.newVoltageCnec()
            .withId("cnec2stateCurativeContingency1")
            .withInstant(crac.getInstant(Instant.Kind.CURATIVE))
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
        NetworkAction networkAction = remedialActionAdder.newOnVoltageConstraintUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.PREVENTIVE))
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(networkAction.getUsageRules().get(0) instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) networkAction.getUsageRules().get(0);
        assertEquals(crac.getInstant(Instant.Kind.PREVENTIVE), onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), crac.getInstant(Instant.Kind.CURATIVE))));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        NetworkAction networkAction = remedialActionAdder.newOnVoltageConstraintUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.CURATIVE))
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(networkAction.getUsageRules().get(0) instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) networkAction.getUsageRules().get(0);
        assertEquals(crac.getInstant(Instant.Kind.CURATIVE), onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), crac.getInstant(Instant.Kind.CURATIVE))));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.OUTAGE)).withVoltageCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testAbsentCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE))
            .withVoltageCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE));
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
            .withInstant(instant)
            .withOperator("operator2")
            .withNetworkElement(id)
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-1500.).withMax(1500.).add();
        if (!instant.isPreventive()) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {

        addCnec("cnec-prev", crac.getInstant(Instant.Kind.PREVENTIVE));
        addCnec("cnec-out", crac.getInstant(Instant.Kind.OUTAGE));
        addCnec("cnec-auto", crac.getInstant(Instant.Kind.AUTO));
        addCnec("cnec-cur", crac.getInstant(Instant.Kind.CURATIVE));

        OnVoltageConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withVoltageCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withVoltageCnec("cnec-out").add(); // ok
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withVoltageCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withVoltageCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.AUTO)).withVoltageCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.AUTO)).withVoltageCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.AUTO)).withVoltageCnec("cnec-auto").add(); // ok
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.AUTO)).withVoltageCnec("cnec-cur"); // nok
        assertThrows(FaraoException.class, adder::add);

        // CURATIVE RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withVoltageCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withVoltageCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withVoltageCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withVoltageCnec("cnec-cur").add(); // ok
    }
}
