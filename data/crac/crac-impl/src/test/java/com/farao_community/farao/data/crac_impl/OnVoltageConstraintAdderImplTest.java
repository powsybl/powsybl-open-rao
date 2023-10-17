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
    private static final Instant instantPrev = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant instantOutage = new InstantImpl("outage", InstantKind.OUTAGE, instantPrev);
    private static final Instant instantAuto = new InstantImpl("auto", InstantKind.AUTO, instantOutage);
    private static final Instant instantCurative = new InstantImpl("curative", InstantKind.CURATIVE, instantAuto);
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
            .withInstant(instantCurative)
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
            .withInstant(instantPrev)
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(networkAction.getUsageRules().get(0) instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) networkAction.getUsageRules().get(0);
        assertEquals(instantPrev, onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), instantCurative)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnVoltageConstraintUsageRule()
            .withInstant(instantCurative)
            .withVoltageCnec("cnec2stateCurativeContingency1")
            .add()
            .add();

        assertEquals(1, networkAction.getUsageRules().size());
        assertTrue(networkAction.getUsageRules().get(0) instanceof OnVoltageConstraint);
        OnVoltageConstraint onVoltageConstraint = (OnVoltageConstraint) networkAction.getUsageRules().get(0);
        assertEquals(instantCurative, onVoltageConstraint.getInstant());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), instantCurative)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantOutage).withVoltageCnec("cnec2stateCurativeContingency1");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testAbsentCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantPrev)
            .withVoltageCnec("fake_cnec");
        assertThrows(FaraoException.class, adder::add);
    }

    @Test
    void testNoCnecException() {
        OnVoltageConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantPrev);
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
        if (!instant.equals(instantPrev)) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {

        addCnec("cnec-prev", instantPrev);
        addCnec("cnec-out", instantOutage);
        addCnec("cnec-auto", instantAuto);
        addCnec("cnec-cur", instantCurative);

        OnVoltageConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantPrev).withVoltageCnec("cnec-prev").add(); // ok
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantPrev).withVoltageCnec("cnec-out").add(); // ok
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantPrev).withVoltageCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantPrev).withVoltageCnec("cnec-cur").add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantAuto).withVoltageCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantAuto).withVoltageCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantAuto).withVoltageCnec("cnec-auto").add(); // ok
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantAuto).withVoltageCnec("cnec-cur"); // nok
        assertThrows(FaraoException.class, adder::add);

        // CURATIVE RA
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantCurative).withVoltageCnec("cnec-prev"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantCurative).withVoltageCnec("cnec-out"); // nok
        assertThrows(FaraoException.class, adder::add);
        adder = remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantCurative).withVoltageCnec("cnec-auto"); // nok
        assertThrows(FaraoException.class, adder::add);
        remedialActionAdder.newOnVoltageConstraintUsageRule().withInstant(instantCurative).withVoltageCnec("cnec-cur").add(); // ok
    }
}
