/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AlreadySynchronizedException;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoInputTest {

    private Network network;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
    }

    @Test
    public void testSynchronizeOk() {
        Crac crac = CommonCracCreation.create();
        RaoInput.synchronize(crac, network);
        assertTrue(crac.isSynchronized());
    }

    @Test
    public void testSynchronizeNotFailSecondTime() {
        Crac crac = CommonCracCreation.create();
        RaoInput.synchronize(crac, network);
        try {
            RaoInput.synchronize(crac, network);
        } catch (AlreadySynchronizedException e) {
            fail("AlreadySynchronizedException should not be thrown");
        }
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

        simpleCrac.addCnec("cnec1prev", "FFR1AA1  FFR2AA1  1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), preventiveState.getId());
        simpleCrac.addCnec("cnec2prev", "neId2", Collections.singleton(new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30)), preventiveState.getId());
        simpleCrac.addCnec("cnec1cur", "neId1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 800)), postContingencyState.getId());
        simpleCrac.addCnec("cnec3cur", "BBE1AA1  BBE2AA1  1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), stateThatShouldBeRemoved.getId());

        Topology topology1 = new Topology(
            "topologyId1",
            "topologyName",
            "RTE",
            new ArrayList<>(),
            simpleCrac.getNetworkElement("neId1"),
            ActionType.CLOSE
        );
        Topology topology2 = new Topology(
            "topologyId2",
            "topologyName",
            "RTE",
            new ArrayList<>(),
            simpleCrac.getNetworkElement("FFR1AA1  FFR2AA1  1"),
            ActionType.CLOSE
        );
        ComplexNetworkAction complexNetworkAction = new ComplexNetworkAction("complexNextworkActionId", "RTE");
        PstWithRange pstWithRange = new PstWithRange(
            "pstRangeId",
            "pstRangeName",
            "RTE",
            Collections.singletonList(new FreeToUse(UsageMethod.AVAILABLE, preventiveState)),
            Arrays.asList(new Range(0, 16, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE)),
            simpleCrac.getNetworkElement("pst")
        );

        simpleCrac.addNetworkAction(topology1);
        simpleCrac.addNetworkAction(topology2);
        simpleCrac.addNetworkAction(complexNetworkAction);
        simpleCrac.addRangeAction(pstWithRange);
        assertEquals(4, simpleCrac.getCnecs().size());
        assertEquals(3, simpleCrac.getNetworkActions().size());
        assertEquals(1, simpleCrac.getRangeActions().size());
        assertEquals(3, simpleCrac.getContingencies().size());
        assertEquals(3, simpleCrac.getStates().size());

        List<String> qualityReport = RaoInput.cleanCrac(simpleCrac, network);

        assertEquals(1, simpleCrac.getCnecs().size());
        assertEquals(1, simpleCrac.getNetworkActions().size());
        assertEquals(0, simpleCrac.getRangeActions().size());
        assertEquals(2, simpleCrac.getContingencies().size());
        assertEquals(2, simpleCrac.getStates().size());

        assertEquals(6, qualityReport.size());
        int removedCount = 0;
        for (String line: qualityReport) {
            if (line.contains("[REMOVED]")) {
                removedCount++;
            }
        }
        assertEquals(6, removedCount);
    }
}
