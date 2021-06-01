/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class CracCleanerTest {

    private Network network;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
    }

    @Test
    public void testCleanCrac() {
        Crac crac = CracFactory.findDefault().create("cracId");

        // contingencies
        crac.newContingency()
            .withId("contingendy1Id")
            .withNetworkElement("FFR1AA1  FFR2AA1  1")
            .add();

        crac.newContingency()
            .withId("contingency2Id")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withNetworkElement("BBE1AA1  BBE3AA1  1")
            .add();

        crac.newContingency()
            .withId("contThatShouldBeRemoved")
            .withNetworkElement("element that does not exist")
            .add();

        // cnecs
        crac.newFlowCnec()
            .withId("cnec1prev")
            .withNetworkElement("FFR1AA1  FFR2AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized(true)
            .withMonitored(true)
            .newThreshold()
                .withUnit(Unit.AMPERE)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-500.0)
                .add()
            .withIMax(5000.)
            .withNominalVoltage(380.)
            .add();

        crac.newFlowCnec()
            .withId("cnec2prev")
            .withNetworkElement("element that does not exist")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized(true)
            .withMonitored(true)
            .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-0.3)
                .add()
            .withIMax(5000.)
            .withNominalVoltage(380.)
            .add();

        crac.newFlowCnec()
            .withId("cnec1cur")
            .withNetworkElement("element that does not exist")
            .withInstant(Instant.OUTAGE)
            .withContingency("contingendy1Id")
            .withOptimized(true)
            .withMonitored(true)
            .newThreshold()
                .withUnit(Unit.AMPERE)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-800.)
                .add()
            .withIMax(5000.)
            .withNominalVoltage(380.)
            .add();

        crac.newFlowCnec()
            .withId("cnec3cur")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withInstant(Instant.OUTAGE)
            .withContingency("contThatShouldBeRemoved")
            .withOptimized(true)
            .withMonitored(true)
            .newThreshold()
                .withUnit(Unit.AMPERE)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-500.)
                .add()
            .withIMax(5000.)
            .withNominalVoltage(380.)
            .add();

        // remedial actions
        crac.newNetworkAction()
            .withId("topoRaId1")
            .withName("topoRaName1")
            .withOperator("operator")
            .newTopologicalAction()
                .withNetworkElement("element that does not exist")
                .withActionType(ActionType.OPEN)
                .add()
            .add();

        crac.newNetworkAction()
            .withId("topoRaId2")
            .withName("topoRaName2")
            .withOperator("operator")
            .newTopologicalAction()
                .withNetworkElement("FFR1AA1  FFR2AA1  1")
                .withActionType(ActionType.CLOSE)
                .add()
            .add();

        crac.newNetworkAction()
            .withId("topoRaId3")
            .withName("topoRaName2")
            .withOperator("operator")
            .newTopologicalAction()
                .withNetworkElement("FFR1AA1  FFR2AA1  1")
                .withActionType(ActionType.CLOSE)
                .add()
            .newTopologicalAction()
                .withNetworkElement("element that does not exist")
                .withActionType(ActionType.OPEN)
                .add()
            .add();

        crac.newPstRangeAction()
            .withId("pstRange1Id")
            .withName("pstRange1Name")
            .withOperator("operator")
            .withNetworkElement("element that does not exist")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, -20., 2, 20.))
            .newTapRange()
                .withMinTap(1)
                .withMaxTap(16)
                .withRangeType(RangeType.ABSOLUTE)
                .add()
            .newFreeToUseUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.PREVENTIVE)
                .add()
            .add();

        crac.newPstRangeAction()
            .withId("pstRange2Id")
            .withName("pstRange2Name")
            .withOperator("operator")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, -20., 2, 20.))
            .newTapRange()
                .withMinTap(1)
                .withMaxTap(16)
                .withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT)
                .add()
            .newFreeToUseUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.PREVENTIVE)
                .add()
            .add();

        // check crac before clean
        assertEquals(4, crac.getFlowCnecs().size());
        assertEquals(3, crac.getNetworkActions().size());
        assertEquals(2, crac.getRangeActions().size());
        assertEquals(3, crac.getContingencies().size());
        assertEquals(3, crac.getStates().size());

        CracCleaner cracCleaner = new CracCleaner();
        List<String> qualityReport = cracCleaner.cleanCrac(crac, network);

        assertEquals(1, crac.getFlowCnecs().size());
        assertEquals(1, crac.getNetworkActions().size());
        assertEquals(1, crac.getRangeActions().size());
        assertEquals(2, crac.getContingencies().size());
        assertEquals(1, crac.getStates().size());

        assertEquals(8, qualityReport.size());
        int removedCount = 0;
        for (String line : qualityReport) {
            if (line.contains("[REMOVED]")) {
                removedCount++;
            }
        }
        assertEquals(8, removedCount);
    }

    private Crac createTestCrac() {
        CracFactory factory = CracFactory.findDefault();
        Crac crac = factory.create("test-crac");

        crac.newFlowCnec()
            .withId("BBE1AA1  BBE2AA1  1")
            .withOptimized(true)
            .withMonitored(true)
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withMin(0.0)
                .withMax(0.0)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        crac.newFlowCnec()
            .withId("BBE1AA1  BBE3AA1  1")
            .withOptimized(true)
            .withMonitored(false)
            .withNetworkElement("BBE1AA1  BBE3AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withMin(0.0)
                .withMax(0.0)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        crac.newFlowCnec()
            .withId("FFR1AA1  FFR2AA1  1")
            .withOptimized(false)
            .withMonitored(true)
            .withNetworkElement("FFR1AA1  FFR2AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withMin(0.0)
                .withMax(0.0)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        crac.newFlowCnec()
            .withId("FFR1AA1  FFR3AA1  1")
            .withOptimized(false)
            .withMonitored(false)
            .withNetworkElement("FFR1AA1  FFR3AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withMin(0.0)
                .withMax(0.0)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        return crac;
    }

    @Test
    public void testIgnoreRemoveUnmonitoredCnecs() {
        Crac crac = createTestCrac();
        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.disableFeature(CracCleaningFeature.CHECK_CNEC_MNEC);
        List<String> qualityReport = cracCleaner.cleanCrac(crac, network);
        assertEquals(0, qualityReport.size());
        assertEquals(4, crac.getFlowCnecs().size());
    }

    @Test
    public void testRemoveUnmonitoredCnecs() {
        Crac crac = createTestCrac();
        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.enableFeature(CracCleaningFeature.CHECK_CNEC_MNEC);
        List<String> qualityReport = cracCleaner.cleanCrac(crac, network);
        assertEquals(1, qualityReport.size());
        assertEquals(3, crac.getFlowCnecs().size());
        assertNull(crac.getFlowCnec("FFR1AA1  FFR3AA1  1"));
    }

    @Test
    public void testRemoveOnStateUsageRule() {
        Crac crac = CracFactory.findDefault().create("cracId");

        crac.newContingency()
            .withId("cont_exists")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .add();

        crac.newContingency()
            .withId("cont_unknown")
            .withNetworkElement("unknown")
            .add();

        crac.newNetworkAction()
            .withId("topoRaId")
            .withName("topoRaName")
            .withOperator("operator")
            .newTopologicalAction()
                .withNetworkElement("FFR1AA1  FFR3AA1  1")
                .withActionType(ActionType.OPEN)
                .add()
            .newOnStateUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.CURATIVE)
                .withContingency("cont_exists")
                .add()
            .newOnStateUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.CURATIVE)
                .withContingency("cont_unknown")
                .add()
            .add();

        crac.newPstRangeAction()
            .withId("pstRangeId")
            .withName("pstRangeName")
            .withOperator("operator")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(1, -20., 2, 20.))
            .newOnStateUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.CURATIVE)
                .withContingency("cont_exists")
                .add()
            .newOnStateUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.CURATIVE)
                .withContingency("cont_unknown")
                .add()
            .add();

        CracCleaner cracCleaner = new CracCleaner();
        List<String> qualityReport = cracCleaner.cleanCrac(crac, network);

        assertEquals(4, qualityReport.size());
        assertEquals(1, crac.getStates().size());
        assertEquals(1, crac.getNetworkAction("topoRaId").getUsageRules().size());
        assertEquals(1, crac.getRangeAction("pstRangeId").getUsageRules().size());
    }

}
