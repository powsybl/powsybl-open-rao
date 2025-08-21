/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraintAdder;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class OnConstraintAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

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
            .withContingencyElement("FFR1AA1  FFR3AA1  1", ContingencyElementType.LINE)
            .add();

        crac.newAngleCnec()
            .withId("angleCnec2stateCurativeContingency1")
            .withExportingNetworkElement("FFR2AA1")
            .withImportingNetworkElement("DDE3AA1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-1500.).withMax(1500.).add()
            .add();

        crac.newFlowCnec()
            .withId("flowCnec2stateCurativeContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .newThreshold()
            .withUnit(Unit.PERCENT_IMAX)
            .withSide(TwoSides.ONE)
            .withMin(-0.3)
            .withMax(0.3)
            .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        crac.newVoltageCnec()
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
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add();
    }

    private Set<UsageRule> getOnConstraintUsageRulesForGivenCnecType(Set<UsageRule> usageRules, Class<? extends Cnec<?>> cnecType) {
        return usageRules.stream().filter(OnConstraint.class::isInstance).filter(ur -> cnecType.isInstance(((OnConstraint<?>) ur).getCnec())).collect(Collectors.toSet());
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnConstraintUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withCnec("angleCnec2stateCurativeContingency1")
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .newOnConstraintUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withCnec("flowCnec2stateCurativeContingency1")
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .newOnConstraintUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withCnec("voltageCnec2stateCurativeContingency1")
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .add();

        assertEquals(3, networkAction.getUsageRules().size());
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        OnConstraint<?> onConstraint;

        Set<UsageRule> onAngleConstraintUsageRules = getOnConstraintUsageRulesForGivenCnecType(networkAction.getUsageRules(), AngleCnec.class);
        assertEquals(1, onAngleConstraintUsageRules.size());
        onConstraint = (OnConstraint<?>) onAngleConstraintUsageRules.iterator().next();
        assertEquals(preventiveInstant, onConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals("angleCnec2stateCurativeContingency1", onConstraint.getCnec().getId());
        assertInstanceOf(AngleCnec.class, onConstraint.getCnec());

        Set<UsageRule> onFlowConstraintUsageRules = getOnConstraintUsageRulesForGivenCnecType(networkAction.getUsageRules(), FlowCnec.class);
        assertEquals(1, onFlowConstraintUsageRules.size());
        onConstraint = (OnConstraint<?>) onFlowConstraintUsageRules.iterator().next();
        assertEquals(preventiveInstant, onConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals("flowCnec2stateCurativeContingency1", onConstraint.getCnec().getId());
        assertInstanceOf(FlowCnec.class, onConstraint.getCnec());

        Set<UsageRule> onVoltageConstraintUsageRules = getOnConstraintUsageRulesForGivenCnecType(networkAction.getUsageRules(), VoltageCnec.class);
        assertEquals(1, onVoltageConstraintUsageRules.size());
        onConstraint = (OnConstraint<?>) onVoltageConstraintUsageRules.iterator().next();
        assertEquals(preventiveInstant, onConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals("voltageCnec2stateCurativeContingency1", onConstraint.getCnec().getId());
        assertInstanceOf(VoltageCnec.class, onConstraint.getCnec());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> networkAction = remedialActionAdder.newOnConstraintUsageRule()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCnec("angleCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .newOnConstraintUsageRule()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCnec("flowCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .newOnConstraintUsageRule()
            .withInstant(CURATIVE_INSTANT_ID)
            .withCnec("voltageCnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(3, networkAction.getUsageRules().size());
        assertEquals(1, crac.getStates().size());
        OnConstraint<?> onConstraint;

        Set<UsageRule> onAngleConstraintUsageRules = getOnConstraintUsageRulesForGivenCnecType(networkAction.getUsageRules(), AngleCnec.class);
        assertEquals(1, onAngleConstraintUsageRules.size());
        onConstraint = (OnConstraint<?>) onAngleConstraintUsageRules.iterator().next();
        assertEquals(curativeInstant, onConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals("angleCnec2stateCurativeContingency1", onConstraint.getCnec().getId());
        assertInstanceOf(AngleCnec.class, onConstraint.getCnec());

        Set<UsageRule> onFlowConstraintUsageRules = getOnConstraintUsageRulesForGivenCnecType(networkAction.getUsageRules(), FlowCnec.class);
        assertEquals(1, onFlowConstraintUsageRules.size());
        onConstraint = (OnConstraint<?>) onFlowConstraintUsageRules.iterator().next();
        assertEquals(curativeInstant, onConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals("flowCnec2stateCurativeContingency1", onConstraint.getCnec().getId());
        assertInstanceOf(FlowCnec.class, onConstraint.getCnec());

        Set<UsageRule> onVoltageConstraintUsageRules = getOnConstraintUsageRulesForGivenCnecType(networkAction.getUsageRules(), VoltageCnec.class);
        assertEquals(1, onVoltageConstraintUsageRules.size());
        onConstraint = (OnConstraint<?>) onVoltageConstraintUsageRules.iterator().next();
        assertEquals(curativeInstant, onConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals("voltageCnec2stateCurativeContingency1", onConstraint.getCnec().getId());
        assertInstanceOf(VoltageCnec.class, onConstraint.getCnec());
    }

    @Test
    void testOutageException() {
        OnConstraintAdder<NetworkActionAdder, ?> adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(OUTAGE_INSTANT_ID).withCnec("flowCnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("OnConstraint usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testAbsentCnecException() {
        OnConstraintAdder<NetworkActionAdder, ?> adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID)
            .withCnec("fake_cnec").withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cnec fake_cnec does not exist in crac. Consider adding it first.", exception.getMessage());
    }

    @Test
    void testNoCnecException() {
        OnConstraintAdder<NetworkActionAdder, ?> adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnConstraint without a cnec. Please use withCnec() with a non null value", exception.getMessage());
    }

    @Test
    void testNoInstantException() {
        OnConstraintAdder<NetworkActionAdder, ?> adder = remedialActionAdder.newOnConstraintUsageRule().withCnec("angleCnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        Exception exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnConstraint without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethodException() {
        OnConstraintAdder<NetworkActionAdder, ?> adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("voltageCnec2stateCurativeContingency1");
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Cannot add OnConstraint without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    private void addAngleCnec(String id, String instantId) {
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

    private void addFlowCnec(String id, String instantId) {
        FlowCnecAdder adder = crac.newFlowCnec()
            .withId(id)
            .withNetworkElement(id)
            .withInstant(instantId)
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(TwoSides.ONE).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.);
        if (!crac.getInstant(instantId).isPreventive()) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    private void addVoltageCnec(String id, String instantId) {
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
        // todo : same thing for on flow constraint in country, in the code
        addAngleCnec("angle-cnec-prev", PREVENTIVE_INSTANT_ID);
        addAngleCnec("angle-cnec-out", OUTAGE_INSTANT_ID);
        addAngleCnec("angle-cnec-auto", AUTO_INSTANT_ID);
        addAngleCnec("angle-cnec-cur", CURATIVE_INSTANT_ID);

        addFlowCnec("flow-cnec-prev", PREVENTIVE_INSTANT_ID);
        addFlowCnec("flow-cnec-out", OUTAGE_INSTANT_ID);
        addFlowCnec("flow-cnec-auto", AUTO_INSTANT_ID);
        addFlowCnec("flow-cnec-cur", CURATIVE_INSTANT_ID);

        addVoltageCnec("voltage-cnec-prev", PREVENTIVE_INSTANT_ID);
        addVoltageCnec("voltage-cnec-out", OUTAGE_INSTANT_ID);
        addVoltageCnec("voltage-cnec-auto", AUTO_INSTANT_ID);
        addVoltageCnec("voltage-cnec-cur", CURATIVE_INSTANT_ID);

        OnConstraintAdder<NetworkActionAdder, ?> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("angle-cnec-prev").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("angle-cnec-out").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("angle-cnec-auto").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("angle-cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("flow-cnec-prev").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("flow-cnec-out").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("flow-cnec-auto").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("flow-cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("voltage-cnec-prev").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("voltage-cnec-out").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("voltage-cnec-auto").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("voltage-cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        Exception exception;

        // AUTO RA
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("angle-cnec-prev").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("angle-cnec-out").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("angle-cnec-auto").withUsageMethod(UsageMethod.FORCED).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("angle-cnec-cur").withUsageMethod(UsageMethod.FORCED).add(); // ok

        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("flow-cnec-prev").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("flow-cnec-out").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("flow-cnec-auto").withUsageMethod(UsageMethod.FORCED).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("flow-cnec-cur").withUsageMethod(UsageMethod.FORCED).add(); // ok

        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("voltage-cnec-prev").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("voltage-cnec-out").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("voltage-cnec-auto").withUsageMethod(UsageMethod.FORCED).add(); // ok
        remedialActionAdder.newOnConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withCnec("voltage-cnec-cur").withUsageMethod(UsageMethod.FORCED).add(); // ok

        // CURATIVE RA
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("angle-cnec-prev").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("angle-cnec-out").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("angle-cnec-auto").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'auto' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("angle-cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("flow-cnec-prev").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("flow-cnec-out").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("flow-cnec-auto").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'auto' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("flow-cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("voltage-cnec-prev").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("voltage-cnec-out").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("voltage-cnec-auto").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'auto' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("voltage-cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
    }
}
