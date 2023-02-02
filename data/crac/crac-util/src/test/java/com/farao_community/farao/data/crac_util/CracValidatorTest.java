/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CracValidatorTest {
    private Crac crac;
    private Network network;

    @Before
    public void setUp() {
        network = Network.read("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));

        crac = CracFactory.findDefault().create("crac");
        crac.newContingency().withId("co-1").withNetworkElement("BBE1AA1  BBE2AA1  1").add();
        crac.newContingency().withId("co-2").withNetworkElement("BBE1AA1  BBE3AA1  1").add();

        crac.newFlowCnec()
            .withId("auto-cnec-1")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withContingency("co-1").withInstant(Instant.AUTO)
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
            .withContingency("co-1").withInstant(Instant.AUTO)
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
            .withContingency("co-2").withInstant(Instant.AUTO)
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
        assertEquals(Instant.OUTAGE, duplicate.getState().getInstant());
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
    public void testDuplicateAutoCnecs0() {
        // No auto RA in CRAC => no auto perimeter => no need to duplicate CNECs
        List<String> report = CracValidator.validateCrac(crac, network);

        assertEquals(3, crac.getFlowCnecs().size());
    }

    @Test
    public void testDuplicateAutoCnecs1() {
        // Auto RAs in CRAC but useless for 3 CNECs => duplicate all 3 auto CNECs
        crac.newNetworkAction()
            .withId("network-action-1")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstant(Instant.AUTO).add()
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
    public void testDuplicateAutoCnecs2() {
        // 1 auto RA in CRAC for auto-cnec-1 => duplicate other 2 CNECs
        crac.newNetworkAction()
            .withId("network-action-1")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintUsageRule().withFlowCnec("auto-cnec-1").withInstant(Instant.AUTO).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstant(Instant.AUTO).add()
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
    public void testDuplicateAutoCnecs3() {
        // 2 auto RA in CRAC for auto-cnec-1 & auto-cnec-2 => duplicate other auto-cnec-3
        crac.newNetworkAction()
            .withId("network-action-1")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintUsageRule().withFlowCnec("auto-cnec-1").withInstant(Instant.AUTO).add()
            .add();
        crac.newNetworkAction()
            .withId("network-action-2")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.DE).withInstant(Instant.AUTO).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstant(Instant.AUTO).add()
            .add();

        List<String> report = CracValidator.validateCrac(crac, network);

        assertEquals(4, crac.getFlowCnecs().size());

        assertCnecHasOutageDuplicate("auto-cnec-3");

        assertEquals(1, report.size());
        assertTrue(report.contains("CNEC \"auto-cnec-3\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }

    @Test
    public void testDuplicateAutoCnecs4() {
        // 2 auto RA in CRAC for contingency of auto-cnec-1 & auto-cnec-2 => duplicate other auto-cnec-3
        crac.newNetworkAction()
            .withId("network-action-1")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withCountry(Country.NL).withInstant(Instant.AUTO).add()
            .add();
        crac.newNetworkAction()
            .withId("network-action-2")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnStateUsageRule().withContingency("co-1").withUsageMethod(UsageMethod.FORCED).withInstant(Instant.AUTO).add()
            .add();

        List<String> report = CracValidator.validateCrac(crac, network);

        assertEquals(4, crac.getFlowCnecs().size());

        assertCnecHasOutageDuplicate("auto-cnec-3");

        assertEquals(1, report.size());
        assertTrue(report.contains("CNEC \"auto-cnec-3\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }

    @Test
    public void testDuplicateAutoCnecs5() {
        // 1 auto RA in CRAC forced after all contingencies => no duplicate
        crac.newNetworkAction()
            .withId("network-action-1")
            .newTopologicalAction().withNetworkElement("FFR2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newOnStateUsageRule().withContingency("co-1").withUsageMethod(UsageMethod.FORCED).withInstant(Instant.AUTO).add()
            .newOnStateUsageRule().withContingency("co-2").withUsageMethod(UsageMethod.FORCED).withInstant(Instant.AUTO).add()
            .add();

        CracValidator.validateCrac(crac, network);
        assertEquals(3, crac.getFlowCnecs().size());
    }
}
