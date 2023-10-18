/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.InstantImpl;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CracValidatorTest {
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private Crac crac;
    private Network network;

    @BeforeEach
    public void setUp() {
        network = Network.read("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));

        crac = CracFactory.findDefault().create("crac");
        crac.newContingency().withId("co-1").withNetworkElement("BBE1AA1  BBE2AA1  1").add();
        crac.newContingency().withId("co-2").withNetworkElement("BBE1AA1  BBE3AA1  1").add();
        crac.addInstant(INSTANT_PREV);
        crac.addInstant(INSTANT_OUTAGE);
        crac.addInstant(INSTANT_AUTO);

        crac.newFlowCnec()
            .withId("auto-cnec-1")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withContingency("co-1")
            .withInstantId(INSTANT_AUTO.getId())
            .withNominalVoltage(400., Side.LEFT)
            .withNominalVoltage(200., Side.RIGHT)
            .withIMax(2000., Side.LEFT)
            .withIMax(4000., Side.RIGHT)
            .withReliabilityMargin(15.)
            .withOptimized()
            .newThreshold().withMin(-100.).withMax(100.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
            .newThreshold().withMin(-100.).withMax(100.).withUnit(Unit.MEGAWATT).withSide(Side.RIGHT).add()
            .newThreshold().withMin(-1.).withMax(1.).withUnit(Unit.PERCENT_IMAX).withSide(Side.LEFT).add()
            .add();

        crac.newFlowCnec()
            .withId("auto-cnec-2")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withContingency("co-1")
            .withInstantId(INSTANT_AUTO.getId())
            .withNominalVoltage(300., Side.LEFT)
            .withNominalVoltage(900., Side.RIGHT)
            .withIMax(40, Side.LEFT)
            .withIMax(40., Side.RIGHT)
            .withReliabilityMargin(0.)
            .newThreshold().withMax(1000.).withUnit(Unit.AMPERE).withSide(Side.LEFT).add()
            .add();

        crac.newFlowCnec()
            .withId("auto-cnec-3")
            .withNetworkElement("BBE1AA1  BBE3AA1  1")
            .withContingency("co-2")
            .withInstantId(INSTANT_AUTO.getId())
            .withNominalVoltage(500., Side.LEFT)
            .withNominalVoltage(700., Side.RIGHT)
            .withIMax(200., Side.LEFT)
            .withIMax(400., Side.RIGHT)
            .withReliabilityMargin(1.)
            .withMonitored()
            .newThreshold().withMin(-1.).withUnit(Unit.PERCENT_IMAX).withSide(Side.RIGHT).add()
            .add();
    }

    private void assertCnecHasOutageDuplicate(String flowCnecId) {
        FlowCnec flowCnec = crac.getFlowCnec(flowCnecId);
        assertNotNull(flowCnec);
        FlowCnec duplicate = crac.getFlowCnec(flowCnec.getId() + " - OUTAGE DUPLICATE");
        assertNotNull(duplicate);
        assertEquals(flowCnec.getNetworkElement().getId(), duplicate.getNetworkElement().getId());
        assertEquals(flowCnec.getState().getContingency(), duplicate.getState().getContingency());
        assertEquals(INSTANT_OUTAGE, duplicate.getState().getInstant());
        assertEquals(flowCnec.isOptimized(), duplicate.isOptimized());
        assertEquals(flowCnec.isMonitored(), duplicate.isMonitored());
        assertEquals(flowCnec.getReliabilityMargin(), duplicate.getReliabilityMargin(), 1e-6);
        assertEquals(flowCnec.getIMax(Side.LEFT), duplicate.getIMax(Side.LEFT), 1e-6);
        assertEquals(flowCnec.getIMax(Side.RIGHT), duplicate.getIMax(Side.RIGHT), 1e-6);
        assertEquals(flowCnec.getNominalVoltage(Side.LEFT), duplicate.getNominalVoltage(Side.LEFT), 1e-6);
        assertEquals(flowCnec.getNominalVoltage(Side.RIGHT), duplicate.getNominalVoltage(Side.RIGHT), 1e-6);
        assertEquals(flowCnec.getThresholds(), duplicate.getThresholds());
    }

    @Test
    void testDuplicateAutoCnecsWithInvalidInstant() {
        FaraoException exception = assertThrows(FaraoException.class, () -> CracValidator.validateCrac(crac, network, INSTANT_OUTAGE));
        assertEquals("Instant should be an auto instant", exception.getMessage());
    }

    @Test
    void testDuplicateAutoCnecs0() {
        // No auto RA in CRAC => no auto perimeter => no need to duplicate CNECs
        List<String> report = CracValidator.validateCrac(crac, network, INSTANT_AUTO);

        assertEquals(3, crac.getFlowCnecs().size());
    }

    @Test
    void testDuplicateAutoCnecs1() {
        // Auto RAs in CRAC but useless for 3 CNECs => duplicate all 3 auto CNECs
        crac.newNetworkAction()
            .withId("network-action-1")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstantId(INSTANT_AUTO.getId()).add()
            .add();
        List<String> report = CracValidator.validateCrac(crac, network, INSTANT_AUTO);

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
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintUsageRule().withFlowCnec("auto-cnec-1").withInstantId(INSTANT_AUTO.getId()).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstantId(INSTANT_AUTO.getId()).add()
            .add();

        List<String> report = CracValidator.validateCrac(crac, network, INSTANT_AUTO);

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
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintUsageRule().withFlowCnec("auto-cnec-1").withInstantId(INSTANT_AUTO.getId()).add()
            .add();
        crac.newNetworkAction()
            .withId("network-action-2")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.DE).withInstantId(INSTANT_AUTO.getId()).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstantId(INSTANT_AUTO.getId()).add()
            .add();

        List<String> report = CracValidator.validateCrac(crac, network, INSTANT_AUTO);

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
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstantId(INSTANT_AUTO.getId()).add()
            .add();
        crac.newNetworkAction()
            .withId("network-action-2")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnContingencyStateUsageRule().withContingency("co-1").withUsageMethod(UsageMethod.FORCED).withInstantId(INSTANT_AUTO.getId()).add()
            .add();

        List<String> report = CracValidator.validateCrac(crac, network, INSTANT_AUTO);

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
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnContingencyStateUsageRule().withContingency("co-1").withUsageMethod(UsageMethod.FORCED).withInstantId(INSTANT_AUTO.getId()).add()
            .newOnContingencyStateUsageRule().withContingency("co-2").withUsageMethod(UsageMethod.FORCED).withInstantId(INSTANT_AUTO.getId()).add()
            .add();

        CracValidator.validateCrac(crac, network, INSTANT_AUTO);
        assertEquals(3, crac.getFlowCnecs().size());
    }
}
