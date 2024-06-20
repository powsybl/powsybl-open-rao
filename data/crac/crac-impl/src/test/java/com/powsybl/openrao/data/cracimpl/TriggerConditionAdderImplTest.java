/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerConditionAdder;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.ejml.UtilEjml.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class TriggerConditionAdderImplTest {

    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private NetworkActionAdder remedialActionAdder;
    private Instant preventiveInstant;
    private Instant curativeInstant;
    private Contingency contingencyFr1Fr3;
    private AngleCnec angleCnec;
    private FlowCnec flowCnec;
    private VoltageCnec voltageCnec;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        contingencyFr1Fr3 = crac.newContingency()
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withContingencyElement("FFR1AA1  FFR3AA1  1", ContingencyElementType.LINE)
            .add();

        crac.newContingency()
            .withId("Contingency FR2 FR3")
            .withName("Trip of FFR2AA1 FFR3AA1 1")
            .withContingencyElement("FFR2AA1  FFR3AA1  1", ContingencyElementType.LINE)
            .add();

        angleCnec = crac.newAngleCnec()
            .withId("angleCnec2stateCurativeContingency1")
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-1500.).withMax(1500.).add()
            .add();

        flowCnec = crac.newFlowCnec()
            .withId("flowCnec2stateCurativeContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(Side.LEFT)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .newThreshold()
            .withUnit(Unit.PERCENT_IMAX)
            .withSide(Side.LEFT)
            .withMin(-0.3)
            .withMax(0.3)
            .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        voltageCnec = crac.newVoltageCnec()
            .withId("voltageCnec2stateCurativeContingency1")
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
    void testInstantAndUsageMethodOnly() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(preventiveInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isEmpty());
        assertTrue(triggerCondition.getCnec().isEmpty());
        assertTrue(triggerCondition.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testContingency() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isPresent());
        assertEquals(contingencyFr1Fr3, triggerCondition.getContingency().get());
        assertTrue(triggerCondition.getCnec().isEmpty());
        assertTrue(triggerCondition.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testAngleCnec() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCnec("angleCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isEmpty());
        assertTrue(triggerCondition.getCnec().isPresent());
        assertEquals(angleCnec, triggerCondition.getCnec().get());
        assertTrue(triggerCondition.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testFlowCnec() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCnec("flowCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isEmpty());
        assertTrue(triggerCondition.getCnec().isPresent());
        assertEquals(flowCnec, triggerCondition.getCnec().get());
        assertTrue(triggerCondition.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testVoltageCnec() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCnec("voltageCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isEmpty());
        assertTrue(triggerCondition.getCnec().isPresent());
        assertEquals(voltageCnec, triggerCondition.getCnec().get());
        assertTrue(triggerCondition.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testCountry() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCountry(Country.FR)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isEmpty());
        assertTrue(triggerCondition.getCnec().isEmpty());
        assertTrue(triggerCondition.getCountry().isPresent());
        assertEquals(Country.FR, triggerCondition.getCountry().get());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testContingencyAndAngleCnec() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withCnec("angleCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isPresent());
        assertEquals(contingencyFr1Fr3, triggerCondition.getContingency().get());
        assertTrue(triggerCondition.getCnec().isPresent());
        assertEquals(angleCnec, triggerCondition.getCnec().get());
        assertTrue(triggerCondition.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testContingencyAndFlowCnec() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withCnec("flowCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isPresent());
        assertEquals(contingencyFr1Fr3, triggerCondition.getContingency().get());
        assertTrue(triggerCondition.getCnec().isPresent());
        assertEquals(flowCnec, triggerCondition.getCnec().get());
        assertTrue(triggerCondition.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testContingencyAndVoltageCnec() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withCnec("voltageCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isPresent());
        assertEquals(contingencyFr1Fr3, triggerCondition.getContingency().get());
        assertTrue(triggerCondition.getCnec().isPresent());
        assertEquals(voltageCnec, triggerCondition.getCnec().get());
        assertTrue(triggerCondition.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testContingencyAndCountry() {
        RemedialAction<?> remedialAction = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withCountry(Country.FR)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        TriggerCondition triggerCondition = remedialAction.getTriggerConditions().iterator().next();
        assertEquals(curativeInstant, triggerCondition.getInstant());
        assertTrue(triggerCondition.getContingency().isPresent());
        assertEquals(contingencyFr1Fr3, triggerCondition.getContingency().get());
        assertTrue(triggerCondition.getCnec().isEmpty());
        assertTrue(triggerCondition.getCountry().isPresent());
        assertEquals(Country.FR, triggerCondition.getCountry().get());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition.getUsageMethod());
    }

    @Test
    void testNoInstant() {
        TriggerConditionAdder<NetworkActionAdder> adder = remedialActionAdder.newTriggerCondition()
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add TriggerCondition without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoTriggerCondition() {
        TriggerConditionAdder<NetworkActionAdder> adder = remedialActionAdder.newTriggerCondition()
            .withInstant(PREVENTIVE_INSTANT_ID);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add TriggerCondition without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    @Test
    void testPreventiveInstantAndContingency() {
        TriggerConditionAdder<NetworkActionAdder> adder = remedialActionAdder.newTriggerCondition()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Preventive TriggerConditions are not allowed after a contingency, except when FORCED.", exception.getMessage());
    }

    @Test
    void testOutageInstant() {
        TriggerConditionAdder<NetworkActionAdder> adder = remedialActionAdder.newTriggerCondition()
            .withInstant(OUTAGE_INSTANT_ID)
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("TriggerConditions are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testCnecsContingencyAndDeclaredContingencyDifferent() {
        TriggerConditionAdder<NetworkActionAdder> adder = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR2 FR3")
            .withCnec("flowCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("The provided cnec is not monitored after the provided contingency, this is not supported.", exception.getMessage());
    }

    @Test
    void testCnecAndCountry() {
        TriggerConditionAdder<NetworkActionAdder> adder = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCnec("flowCnec2stateCurativeContingency1")
            .withCountry(Country.FR)
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("A country and a cnec cannot be provided simultaneously.", exception.getMessage());
    }

    @Test
    void testContingencyDoesNotExistInCrac() {
        TriggerConditionAdder<NetworkActionAdder> adder = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("unknown-contingency")
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Contingency unknown-contingency does not exist in crac. Consider adding it first.", exception.getMessage());
    }

    @Test
    void testCnecDoesNotExistInCrac() {
        TriggerConditionAdder<NetworkActionAdder> adder = remedialActionAdder.newTriggerCondition()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCnec("unknown-cnec")
            .withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cnec unknown-cnec does not exist in crac. Consider adding it first.", exception.getMessage());
    }
}
