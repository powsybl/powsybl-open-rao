/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AlignedRangeAction;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import org.junit.Test;

import java.util.*;

import static com.farao_community.farao.data.crac_io_json.RoundTripUtil.roundTrip;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RemedialActionImportExportTest {

    @Test
    public void abstractRemedialActionJsonCreator() {
        PstSetpoint pstSetpoint = new PstSetpoint(
            "pstSetpointId",
            "pstSetpointName",
            "RTE",
            new ArrayList<>(),
            new NetworkElement("neId"),
            5
        );
        Topology topology = new Topology(
            "topologyId",
            "topologyName",
            "RTE",
            new ArrayList<>(),
            new NetworkElement("neId"),
            ActionType.CLOSE
        );
        Set<AbstractElementaryNetworkAction> elementaryNetworkActions = new HashSet<>();
        elementaryNetworkActions.add(pstSetpoint);
        elementaryNetworkActions.add(topology);
        AbstractRemedialAction complexNetworkAction = new ComplexNetworkAction(
            "complexNetworkActionId",
            "complexNetworkActionName",
            "RTE",
            new ArrayList<>(),
            elementaryNetworkActions
        );
        AbstractRemedialAction transformedComplexNetworkAction = roundTrip(complexNetworkAction, AbstractRemedialAction.class);
        assertEquals(transformedComplexNetworkAction, complexNetworkAction);
    }

    @Test
    public void abstractRemedialActionWithAlignedRangeActionWithRangesWithUsageRulesJsonCreator() {
        State state = new SimpleState(Optional.empty(), new Instant("N", 0));
        List<UsageRule> usageRules = new ArrayList<>();
        usageRules.add(new FreeToUse(UsageMethod.AVAILABLE, new SimpleState(Optional.empty(), new Instant("N", 0))));
        usageRules.add(new OnConstraint(
            UsageMethod.UNAVAILABLE,
            state,
            new SimpleCnec("cnecId", new NetworkElement("neId"), new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.IN, 500), state)
        ));
        usageRules.add(new OnContingency(
            UsageMethod.FORCED,
            state,
            new ComplexContingency("contingencyId")
        ));

        List<Range> ranges = new ArrayList<>();
        ranges.add(new Range(0, 12, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE));
        ranges.add(new Range(-5, 5, RangeType.RELATIVE_FIXED, RangeDefinition.STARTS_AT_ONE));
        ranges.add(new Range(4, 12, RangeType.RELATIVE_DYNAMIC, RangeDefinition.STARTS_AT_ONE));
        ranges.add(new Range(-12, 12, RangeType.ABSOLUTE_FIXED, RangeDefinition.CENTERED_ON_ZERO));

        Set<NetworkElement> networkElements = new HashSet<>();
        networkElements.add(new NetworkElement("neId1"));
        networkElements.add(new NetworkElement("neId2"));

        AbstractRemedialAction abstractRemedialAction = new AlignedRangeAction(
            "alignedRangeActionId",
            "alignedRangeActionName",
            "RTE",
            usageRules,
            ranges,
            networkElements
        );
        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }
}
