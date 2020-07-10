/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.threshold.Direction;
import com.farao_community.farao.data.crac_api.threshold.Side;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.AbstractElementaryNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AlignedRangeAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.farao_community.farao.data.crac_impl.json.RoundTripUtil.roundTrip;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImportExportTest {

    @Test
    public void cracTest() {
        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        Instant initialInstant = simpleCrac.addInstant("N", 0);
        State preventiveState = simpleCrac.addState(null, initialInstant);
        Contingency contingency = simpleCrac.addContingency("contingencyId", "neId");
        simpleCrac.addContingency("contingency2Id", "neId1", "neId2");
        Instant outageInstant = simpleCrac.addInstant("postContingencyId", 5);
        State postContingencyState = simpleCrac.addState(contingency, outageInstant);
        simpleCrac.addState("contingency2Id", "postContingencyId");

        Cnec preventiveCnec1 = simpleCrac.addCnec("cnec1prev", "neId1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), preventiveState.getId());

        Set<Threshold> thresholds = new HashSet<>();
        thresholds.add(new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30));
        thresholds.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 800));

        simpleCrac.addCnec("cnec2prev", "neId2", thresholds, preventiveState.getId());
        simpleCrac.addCnec("cnec1cur", "neId1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 800)), postContingencyState.getId());

        double positiveFrmMw = 20.0;
        AbsoluteFlowThreshold absoluteFlowThreshold = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 500.0);
        Set<Threshold> thresholdSet = new HashSet<>();
        thresholdSet.add(absoluteFlowThreshold);
        simpleCrac.addCnec("cnec3prevId", "cnec3prevName", "neId2", thresholdSet, preventiveState.getId(), positiveFrmMw, false, true);
        simpleCrac.addCnec("cnec4prevId", "cnec4prevName", "neId2", thresholdSet, preventiveState.getId(), 0.0, true, true);

        List<UsageRule> usageRules = new ArrayList<>();
        usageRules.add(new FreeToUse(UsageMethod.AVAILABLE, preventiveState));
        usageRules.add(new OnConstraint(UsageMethod.UNAVAILABLE, preventiveState, preventiveCnec1));
        usageRules.add(new OnContingency(UsageMethod.FORCED, postContingencyState, contingency));

        simpleCrac.addNetworkElement(new NetworkElement("pst"));
        simpleCrac.addNetworkAction(new PstSetpoint("pstSetpointId", "pstSetpointName", "RTE", usageRules, simpleCrac.getNetworkElement("pst"), 15));

        Set<AbstractElementaryNetworkAction> elementaryNetworkActions = new HashSet<>();
        PstSetpoint pstSetpoint = new PstSetpoint(
            "pstSetpointId",
            "pstSetpointName",
            "RTE",
            new ArrayList<>(),
            simpleCrac.getNetworkElement("pst"),
            5
        );
        Topology topology = new Topology(
            "topologyId",
            "topologyName",
            "RTE",
            new ArrayList<>(),
            simpleCrac.getNetworkElement("neId"),
            ActionType.CLOSE
        );
        elementaryNetworkActions.add(pstSetpoint);
        elementaryNetworkActions.add(topology);
        ComplexNetworkAction complexNetworkAction = new ComplexNetworkAction(
            "complexNetworkActionId",
            "complexNetworkActionName",
            "RTE",
            new ArrayList<>(),
            elementaryNetworkActions
        );
        simpleCrac.addNetworkAction(complexNetworkAction);

        simpleCrac.addRangeAction(new PstWithRange(
            "pstRangeId",
            "pstRangeName",
            "RTE",
            Collections.singletonList(new FreeToUse(UsageMethod.AVAILABLE, preventiveState)),
            Arrays.asList(new Range(0, 16, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE),
                new Range(-3, 3, RangeType.RELATIVE_FIXED, RangeDefinition.CENTERED_ON_ZERO)),
            simpleCrac.getNetworkElement("pst")
        ));

        simpleCrac.addRangeAction(new AlignedRangeAction(
            "alignedRangeId",
            "alignedRangeName",
            "RTE",
            Collections.singletonList(new OnConstraint(UsageMethod.AVAILABLE, preventiveState, preventiveCnec1)),
            Collections.singletonList(new Range(-3, 3, RangeType.RELATIVE_DYNAMIC, RangeDefinition.CENTERED_ON_ZERO)),
            Stream.of(simpleCrac.getNetworkElement("pst"), simpleCrac.addNetworkElement("pst2")).collect(Collectors.toSet())
        ));

        simpleCrac.setNetworkDate(new DateTime(2020, 5, 14, 11, 35));

        Crac crac = roundTrip(simpleCrac, SimpleCrac.class);
        assertEquals(5, crac.getNetworkElements().size());
        assertEquals(2, crac.getInstants().size());
        assertEquals(2, crac.getContingencies().size());
        assertEquals(5, crac.getCnecs().size());
        assertEquals(2, crac.getRangeActions().size());
        assertEquals(2, crac.getNetworkActions().size());
        assertTrue(crac.getCnec("cnec4prevId").getMaxThreshold(Unit.MEGAWATT).get()
                > crac.getCnec("cnec3prevId").getMaxThreshold(Unit.MEGAWATT).get());
        assertFalse(crac.getCnec("cnec3prevId").isOptimized());
        assertTrue(crac.getCnec("cnec4prevId").isMonitored());
    }
}
