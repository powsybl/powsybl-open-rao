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
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.XnodeContingency;
import com.farao_community.farao.data.crac_impl.range_domain.PstRangeImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.NetworkActionImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.TopologicalActionImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUseImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
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
        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        Contingency contingency = simpleCrac.addContingency("contingencyId", "FFR1AA1  FFR2AA1  1");
        simpleCrac.addContingency("contingency2Id", "BBE1AA1  BBE2AA1  1", "BBE1AA1  BBE3AA1  1");
        simpleCrac.addContingency("contThatShouldBeRemoved", "element that does not exist");

        Instant initialInstant = simpleCrac.addInstant("N", 0);
        Instant outageInstant = simpleCrac.addInstant("postContingencyId", 5);

        State preventiveState = simpleCrac.addState(null, initialInstant);
        State postContingencyState = simpleCrac.addState(contingency, outageInstant);
        State stateThatShouldBeRemoved = simpleCrac.addState("contThatShouldBeRemoved", "postContingencyId");

        simpleCrac.addNetworkElement("neId1");
        simpleCrac.addNetworkElement("neId2");
        simpleCrac.addNetworkElement(new NetworkElement("pst"));
        simpleCrac.addNetworkElement(new NetworkElement("BBE2AA1  BBE3AA1  1"));

        simpleCrac.newBranchCnec()
                .setId("cnec1prev").optimized().monitored()
                .newNetworkElement().setId("FFR1AA1  FFR2AA1  1").add()
                .newThreshold().setUnit(Unit.AMPERE).setMin(-500.0).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .setInstant(preventiveState.getInstant())
                .add();
        simpleCrac.newBranchCnec()
                .setId("cnec2prev").optimized().monitored()
                .newNetworkElement().setId("neId2").add()
                .newThreshold().setUnit(Unit.PERCENT_IMAX).setMin(-0.3).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .setInstant(preventiveState.getInstant())
                .add();
        simpleCrac.newBranchCnec()
                .setId("cnec1cur").optimized().monitored()
                .newNetworkElement().setId("neId1").add()
                .newThreshold().setUnit(Unit.AMPERE).setMin(-800.).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .setInstant(postContingencyState.getInstant())
                .setContingency(postContingencyState.getContingency().orElseThrow())
                .add();
        simpleCrac.newBranchCnec()
                .setId("cnec3cur").optimized().monitored()
                .newNetworkElement().setId("BBE1AA1  BBE2AA1  1").add()
                .newThreshold().setUnit(Unit.AMPERE).setMin(-500.).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .setInstant(stateThatShouldBeRemoved.getInstant())
                .setContingency(stateThatShouldBeRemoved.getContingency().orElseThrow())
                .add();

        TopologicalActionImpl topology1 = new TopologicalActionImpl(
            "topologyId1",
            "topologyName",
            simpleCrac.getNetworkElement("neId1"),
            ActionType.CLOSE);

        TopologicalActionImpl topology2 = new TopologicalActionImpl(
            "topologyId2",
            "topologyName",
            simpleCrac.getNetworkElement("FFR1AA1  FFR2AA1  1"),
            ActionType.CLOSE);

        NetworkActionImpl topoRa1 = new NetworkActionImpl(
            "topoRaId1",
            "topoRaName1",
            "RTE",
            new ArrayList<>(),
            Collections.singleton(topology1));

        NetworkActionImpl topoRa2 = new NetworkActionImpl(
            "topoRaId2",
            "topoRaName2",
            "RTE",
            new ArrayList<>(),
            Collections.singleton(topology2));

        NetworkActionImpl complexNetworkAction = new NetworkActionImpl(
            "complexNextworkActionId",
            "complexNextworkActionName",
            "RTE",
            new ArrayList<>(),
            new HashSet<>(Arrays.asList(topology1, topology2)));

        PstRangeActionImpl pstRangeAction1 = new PstRangeActionImpl(
            "pstRangeId",
            "pstRangeName",
            "RTE",
            Collections.singletonList(new FreeToUseImpl(UsageMethod.AVAILABLE, preventiveState.getInstant())),
            Collections.singletonList(new PstRangeImpl(0, 16, RangeType.ABSOLUTE, RangeDefinition.STARTS_AT_ONE)),
            simpleCrac.getNetworkElement("pst"));

        PstRangeActionImpl pstRangeAction2 = new PstRangeActionImpl(
            "pstRangeId2",
            "pstRangeName2",
            "RTE",
            Collections.singletonList(new FreeToUseImpl(UsageMethod.AVAILABLE, preventiveState.getInstant())),
            Collections.singletonList(new PstRangeImpl(0, 16, RangeType.RELATIVE_TO_PREVIOUS_INSTANT, RangeDefinition.STARTS_AT_ONE)),
            simpleCrac.getNetworkElement("BBE2AA1  BBE3AA1  1"));

        simpleCrac.addNetworkAction(topoRa1);
        simpleCrac.addNetworkAction(topoRa2);
        simpleCrac.addNetworkAction(complexNetworkAction);
        simpleCrac.addRangeAction(pstRangeAction1);
        simpleCrac.addRangeAction(pstRangeAction2);
        assertEquals(4, simpleCrac.getBranchCnecs().size());
        assertEquals(3, simpleCrac.getNetworkActions().size());
        assertEquals(2, simpleCrac.getRangeActions().size());
        assertEquals(3, simpleCrac.getContingencies().size());
        assertEquals(3, simpleCrac.getStates().size());

        CracCleaner cracCleaner = new CracCleaner();
        List<String> qualityReport = cracCleaner.cleanCrac(simpleCrac, network);

        assertEquals(1, simpleCrac.getBranchCnecs().size());
        assertEquals(1, simpleCrac.getNetworkActions().size());
        assertEquals(0, simpleCrac.getRangeActions().size());
        assertEquals(2, simpleCrac.getContingencies().size());
        assertEquals(2, simpleCrac.getStates().size());

        assertEquals(10, qualityReport.size());
        int removedCount = 0;
        for (String line : qualityReport) {
            if (line.contains("[REMOVED]")) {
                removedCount++;
            }
        }
        assertEquals(10, removedCount);
    }

    private Crac createTestCrac() {
        CracFactory factory = CracFactory.find("SimpleCracFactory");
        Crac crac = factory.create("test-crac");
        Instant inst = crac.newInstant().setId("inst1").setSeconds(10).add();
        crac.newBranchCnec().setId("BBE1AA1  BBE2AA1  1").optimized().monitored()
                .newNetworkElement().setId("BBE1AA1  BBE2AA1  1").add()
                .newThreshold().setUnit(Unit.MEGAWATT).setMin(0.0).setMax(0.).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .setInstant(inst)
                .add();
        crac.newBranchCnec().setId("BBE1AA1  BBE3AA1  1").optimized()
                .newNetworkElement().setId("BBE1AA1  BBE3AA1  1").add()
                .newThreshold().setUnit(Unit.MEGAWATT).setMin(0.0).setMax(0.).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .setInstant(inst)
                .add();
        crac.newBranchCnec().setId("FFR1AA1  FFR2AA1  1").monitored()
                .newNetworkElement().setId("FFR1AA1  FFR2AA1  1").add()
                .newThreshold().setUnit(Unit.MEGAWATT).setMin(0.0).setMax(0.).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .setInstant(inst)
                .add();
        crac.newBranchCnec().setId("FFR1AA1  FFR3AA1  1")
                .newNetworkElement().setId("FFR1AA1  FFR3AA1  1").add()
                .newThreshold().setUnit(Unit.MEGAWATT).setMin(0.0).setMax(0.).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .setInstant(inst)
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
        assertEquals(4, crac.getBranchCnecs().size());
    }

    @Test
    public void testRemoveUnmonitoredCnecs() {
        Crac crac = createTestCrac();
        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.enableFeature(CracCleaningFeature.CHECK_CNEC_MNEC);
        List<String> qualityReport = cracCleaner.cleanCrac(crac, network);
        assertEquals(1, qualityReport.size());
        assertEquals(3, crac.getBranchCnecs().size());
        assertNull(crac.getBranchCnec("FFR1AA1  FFR3AA1  1"));
    }

    @Test
    public void testRemoveOnStateUsageRule() {
        SimpleCrac crac = new SimpleCrac("id");

        crac.newInstant().setId("N").setSeconds(1).add();
        crac.newInstant().setId("Outage").setSeconds(60).add();
        crac.newContingency().setId("cont_exists").newNetworkElement().setId("BBE1AA1  BBE2AA1  1").add().add();
        crac.newContingency().setId("cont_unknown").newNetworkElement().setId("unknown").add().add();

        State n = new SimpleState(Optional.empty(), crac.getInstant("N"));
        State outageOk = new SimpleState(Optional.of(crac.getContingency("cont_exists")), crac.getInstant("Outage"));
        State outageNok = new SimpleState(Optional.of(crac.getContingency("cont_unknown")), crac.getInstant("Outage"));

        crac.addState(n);
        crac.addState(outageOk);
        crac.addState(outageNok);

        List<UsageRule> usageRules = new ArrayList<>();
        usageRules.add(new OnStateImpl(UsageMethod.AVAILABLE, outageOk));
        usageRules.add(new OnStateImpl(UsageMethod.AVAILABLE, outageNok));


        TopologicalActionImpl topologicalAction = new TopologicalActionImpl(
            "topologyId1",
            "topologyName",
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            ActionType.OPEN
        );

        NetworkAction topologicalRa = new NetworkActionImpl(
            "topoRaId",
            "topoRaName",
            "RTE",
            usageRules,
            Collections.singleton(topologicalAction)
        );

        PstRangeActionImpl pstRangeAction = new PstRangeActionImpl(
            "pstRangeId",
            "pstRangeName",
            "RTE",
            usageRules,
            Collections.singletonList(new PstRangeImpl(0, 16, RangeType.ABSOLUTE, RangeDefinition.STARTS_AT_ONE)),
            new NetworkElement("BBE1AA1  BBE2AA1  1")
        );

        crac.addNetworkAction(topologicalRa);
        crac.addRangeAction(pstRangeAction);

        CracCleaner cracCleaner = new CracCleaner();
        List<String> qualityReport = cracCleaner.cleanCrac(crac, network);

        assertEquals(4, qualityReport.size());
        assertEquals(1, crac.getNetworkAction("topoRaId").getUsageRules().size());
        assertEquals(1, crac.getRangeAction("pstRangeId").getUsageRules().size());
    }

    @Test
    public void testIgnoreUnsyncedContingencies() {
        Crac crac = createTestCrac();
        crac.newContingency().setId("cont_unknown").newNetworkElement().setId("unknown").add().add();
        crac.addContingency(new XnodeContingency("xnodecont_unknown", Set.of("unknown")));

        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.cleanCrac(crac, network);

        assertNull(crac.getContingency("cont_unknown"));
        assertNotNull(crac.getContingency("xnodecont_unknown"));
    }
}
