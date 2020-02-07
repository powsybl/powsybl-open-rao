/*
 * Copyright (c) 20, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.*;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.AbstractElementaryNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import org.junit.Test;

import java.util.*;

import static com.farao_community.farao.data.crac_io_json.RoundTripUtil.roundTrip;

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

        Cnec preventiveCnec1 = simpleCrac.addCnec("cnec1prev", "neId1", new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.IN, 500), preventiveState.getId());
        simpleCrac.addCnec("cnec2prev", "neId2", new RelativeFlowThreshold(Side.LEFT, Direction.IN, 30), preventiveState.getId());
        simpleCrac.addCnec("cnec1cur", "neId1", new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.IN, 800), postContingencyState.getId());

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
            simpleCrac.getNetworkElement("neId"),
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

        SimpleCrac simpleCrac1 = roundTrip(simpleCrac, SimpleCrac.class);
        int i = 1;
    }
}
