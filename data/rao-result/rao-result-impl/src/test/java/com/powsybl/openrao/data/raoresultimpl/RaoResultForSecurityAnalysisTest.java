/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresultimpl;

import com.powsybl.action.Action;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.security.condition.TrueCondition;
import com.powsybl.security.strategy.ConditionalActions;
import com.powsybl.security.strategy.OperatorStrategy;
import org.junit.jupiter.api.Test;

import java.util.*;


/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoResultForSecurityAnalysisTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private RaoResultImpl raoResult;
    private Crac crac;
    private Network network;

    private void initializeCracAndItsRaoResult() {
        crac = CommonCracCreation.createWithPreventiveAndCurativePstRange();
        crac.newNetworkAction().withId("na-id")
            .newSwitchAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR3").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR2").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        raoResult = new RaoResultImpl(crac);
    }

    @Test
    void testCreateSecurityAnalysisFromRaoResult() {
        initializeCracAndItsRaoResult();

        //SecurityAnalysisRunParameters.getDefault();
        List<Action> preventiveActions = raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).stream().flatMap(networkAction -> networkAction.getElementaryActions().stream()).toList();
        List<String> preventiveActionIds = preventiveActions.stream().map(Action::getId).toList();
        OperatorStrategy preventiveOp = new OperatorStrategy("id", ContingencyContext.none(), new TrueCondition(), preventiveActionIds);
        // add limits:
        for (FlowCnec cnec : crac.getFlowCnecs(crac.getPreventiveState())) {
            cnec.getThresholds();
        }
        // I can not add this preventive action to the other op because they are after contingency...
        // do the rao have scenarios with multiple contingencies?
        for (Contingency contingency : crac.getContingencies()) {
            List<ConditionalActions> conditionalActions = new ArrayList<>();
            SortedSet<State> stateChronologicallyOrderedByInstant = crac.getStates(contingency);
            for (State state : stateChronologicallyOrderedByInstant) {
                if (state.isPreventive()) {
                    // I can not add this preventive action to the other op because they are after contingency... but i need the network in a correct state...
                    continue;
                }
                if (state.getInstant().isOutage()) {
                    // outage: operator strategy with contingency context but without actions
                    conditionalActions.add(new ConditionalActions("id", new TrueCondition(), Collections.emptyList()));
                } else {
                    List<Action> actions = raoResult.getActivatedNetworkActionsDuringState(state).stream().flatMap(networkAction -> networkAction.getElementaryActions().stream()).toList();
                    conditionalActions.add(new ConditionalActions("id", new TrueCondition(), actions.stream().map(Action::getId).toList()));
                }
            }
            OperatorStrategy op = new OperatorStrategy("id", ContingencyContext.specificContingency(contingency.getId()), conditionalActions);
        }
    }
}