/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.util;

import com.powsybl.contingency.ContingencyElementFactory;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CracValidatorTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";

    private Crac crac;
    private Network network;
    private Instant outageInstant;

    private ContingencyElementType getContingencyType(String id) {
        return ContingencyElementFactory.create(network.getIdentifiable(id)).getType();
    }

    @BeforeEach
    public void setUp() {
        network = Network.read("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));

        crac = CracFactory.findDefault().create("crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        crac.newContingency().withId("co-1").withContingencyElement("BBE1AA1  BBE2AA1  1", getContingencyType("BBE1AA1  BBE2AA1  1")).add();
        crac.newContingency().withId("co-2").withContingencyElement("BBE1AA1  BBE3AA1  1", getContingencyType("BBE1AA1  BBE3AA1  1")).add();

        crac.newFlowCnec()
            .withId("auto-cnec-1")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withContingency("co-1").withInstant(AUTO_INSTANT_ID)
            .withNominalVoltage(400., TwoSides.ONE)
            .withNominalVoltage(200., TwoSides.TWO)
            .withIMax(2000., TwoSides.ONE)
            .withIMax(4000., TwoSides.TWO)
            .withReliabilityMargin(15.)
            .withOptimized()
            .newThreshold().withMin(-100.).withMax(100.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
            .newThreshold().withMin(-100.).withMax(100.).withUnit(Unit.MEGAWATT).withSide(TwoSides.TWO).add()
            .newThreshold().withMin(-1.).withMax(1.).withUnit(Unit.PERCENT_IMAX).withSide(TwoSides.ONE).add()
            .add();

        crac.newFlowCnec()
            .withId("auto-cnec-2")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withContingency("co-1").withInstant(AUTO_INSTANT_ID)
            .withNominalVoltage(300., TwoSides.ONE)
            .withNominalVoltage(900., TwoSides.TWO)
            .withIMax(40, TwoSides.ONE)
            .withIMax(40., TwoSides.TWO)
            .withReliabilityMargin(0.)
            .newThreshold().withMax(1000.).withUnit(Unit.AMPERE).withSide(TwoSides.ONE).add()
            .add();

        crac.newFlowCnec()
            .withId("auto-cnec-3")
            .withNetworkElement("BBE1AA1  BBE3AA1  1")
            .withContingency("co-2").withInstant(AUTO_INSTANT_ID)
            .withNominalVoltage(500., TwoSides.ONE)
            .withNominalVoltage(700., TwoSides.TWO)
            .withIMax(200., TwoSides.ONE)
            .withIMax(400., TwoSides.TWO)
            .withReliabilityMargin(1.)
            .withMonitored()
            .newThreshold().withMin(-1.).withUnit(Unit.PERCENT_IMAX).withSide(TwoSides.TWO).add()
            .add();
    }

    private void assertCnecHasOutageDuplicate(String flowCnecId) {
        FlowCnec flowCnec = crac.getFlowCnec(flowCnecId);
        assertNotNull(flowCnec);
        FlowCnec duplicate = crac.getFlowCnec(flowCnec.getId() + " - OUTAGE DUPLICATE");
        assertNotNull(duplicate);
        assertEquals(flowCnec.getNetworkElement().getId(), duplicate.getNetworkElement().getId());
        assertEquals(flowCnec.getState().getContingency(), duplicate.getState().getContingency());
        assertEquals(outageInstant, duplicate.getState().getInstant());
        assertEquals(flowCnec.isOptimized(), duplicate.isOptimized());
        assertEquals(flowCnec.isMonitored(), duplicate.isMonitored());
        assertEquals(flowCnec.getReliabilityMargin(), duplicate.getReliabilityMargin(), 1e-6);
        assertEquals(flowCnec.getIMax(TwoSides.ONE).get(), duplicate.getIMax(TwoSides.ONE).get(), 1e-6);
        assertEquals(flowCnec.getIMax(TwoSides.TWO).get(), duplicate.getIMax(TwoSides.TWO).get(), 1e-6);
        assertEquals(flowCnec.getNominalVoltage(TwoSides.ONE), duplicate.getNominalVoltage(TwoSides.ONE), 1e-6);
        assertEquals(flowCnec.getNominalVoltage(TwoSides.TWO), duplicate.getNominalVoltage(TwoSides.TWO), 1e-6);
        assertEquals(flowCnec.getThresholds(), duplicate.getThresholds());
    }

    @Test
    void testDuplicateAutoCnecs0() {
        // No auto RA in CRAC => no auto perimeter => no need to duplicate CNECs
        CracValidator.validateCrac(crac, network);

        assertEquals(3, crac.getFlowCnecs().size());
    }

    @Test
    void testDuplicateAutoCnecs1() {
        // Auto RAs in CRAC but useless for 3 CNECs => duplicate all 3 auto CNECs
        crac.newNetworkAction()
            .withId("network-action-1")
            .newSwitchAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();
        List<String> report = CracValidator.validateCrac(crac, network);

        assertEquals(6, crac.getFlowCnecs().size());

        assertCnecHasOutageDuplicate("auto-cnec-1");
        assertCnecHasOutageDuplicate("auto-cnec-2");
        assertCnecHasOutageDuplicate("auto-cnec-3");

        assertEquals(3, report.size());
        assertTrue(report.contains("CNEC \"auto-cnec-1\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
        assertTrue(report.contains("CNEC \"auto-cnec-2\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
        assertTrue(report.contains("CNEC \"auto-cnec-3\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }

    @Test
    void testDuplicateAutoCnecs2() {
        // 1 auto RA in CRAC for auto-cnec-1 => duplicate other 2 CNECs
        crac.newNetworkAction()
            .withId("network-action-1")
            .newSwitchAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withCnec("auto-cnec-1").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();

        List<String> report = CracValidator.validateCrac(crac, network);

        assertEquals(5, crac.getFlowCnecs().size());

        assertCnecHasOutageDuplicate("auto-cnec-2");
        assertCnecHasOutageDuplicate("auto-cnec-3");

        assertEquals(2, report.size());
        assertTrue(report.contains("CNEC \"auto-cnec-2\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
        assertTrue(report.contains("CNEC \"auto-cnec-3\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }

    @Test
    void testDuplicateAutoCnecs3() {
        // 2 auto RA in CRAC for auto-cnec-1 & auto-cnec-2 => duplicate other auto-cnec-3
        crac.newNetworkAction()
            .withId("network-action-1")
            .newSwitchAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withCnec("auto-cnec-1").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();
        crac.newNetworkAction()
            .withId("network-action-2")
            .newSwitchAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.DE).withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();

        List<String> report = CracValidator.validateCrac(crac, network);

        assertEquals(4, crac.getFlowCnecs().size());

        assertCnecHasOutageDuplicate("auto-cnec-3");

        assertEquals(1, report.size());
        assertTrue(report.contains("CNEC \"auto-cnec-3\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }

    @Test
    void testDuplicateAutoCnecs4() {
        // 2 auto RA in CRAC for contingency of auto-cnec-1 & auto-cnec-2 => duplicate other auto-cnec-3
        crac.newNetworkAction()
            .withId("network-action-1")
            .newSwitchAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();
        crac.newNetworkAction()
            .withId("network-action-2")
            .newSwitchAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnContingencyStateUsageRule().withContingency("co-1").withUsageMethod(UsageMethod.FORCED).withInstant(AUTO_INSTANT_ID).add()
            .add();

        List<String> report = CracValidator.validateCrac(crac, network);

        assertEquals(4, crac.getFlowCnecs().size());

        assertCnecHasOutageDuplicate("auto-cnec-3");

        assertEquals(1, report.size());
        assertTrue(report.contains("CNEC \"auto-cnec-3\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }

    @Test
    void testDuplicateAutoCnecs5() {
        // 1 auto RA in CRAC forced after all contingencies => no duplicate
        crac.newNetworkAction()
            .withId("network-action-1")
            .newSwitchAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnContingencyStateUsageRule().withContingency("co-1").withUsageMethod(UsageMethod.FORCED).withInstant(AUTO_INSTANT_ID).add()
            .newOnContingencyStateUsageRule().withContingency("co-2").withUsageMethod(UsageMethod.FORCED).withInstant(AUTO_INSTANT_ID).add()
            .add();

        CracValidator.validateCrac(crac, network);
        assertEquals(3, crac.getFlowCnecs().size());
    }

    @Test
    void testDuplicateCnecsWithOnFlowConstraints() {
        crac.newNetworkAction()
            .withId("network-action-1")
            .newTerminalsConnectionAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withCnec("auto-cnec-1").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();

        CracValidator.validateCrac(crac, network);
        assertEquals(4, crac.getFlowCnecs().size());
        assertNull(crac.getFlowCnec("auto-cnec-1 - OUTAGE DUPLICATE"));
        assertNotNull(crac.getFlowCnec("auto-cnec-2 - OUTAGE DUPLICATE"));
    }

    @Test
    void testDuplicateCnecsWithOnFlowConstraintInCountries() {
        crac.newNetworkAction()
            .withId("network-action-1")
            .newTerminalsConnectionAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.BE).withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();

        CracValidator.validateCrac(crac, network);
        assertEquals(4, crac.getFlowCnecs().size());
        assertNull(crac.getFlowCnec("auto-cnec-1 - OUTAGE DUPLICATE"));
        assertNotNull(crac.getFlowCnec("auto-cnec-2 - OUTAGE DUPLICATE"));
        assertNull(crac.getFlowCnec("auto-cnec-3 - OUTAGE DUPLICATE"));
    }
}
