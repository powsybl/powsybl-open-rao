/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.impl.NetworkActionImpl;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** This class aggregates RemedialActionActivationResult stored in TemporalData<RemedialActionActivationResult> in one big RemedialActionActivationResult.
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class GlobalRemedialActionActivationResult extends GlobalRangeActionActivationResult implements RemedialActionActivationResult {
    private final Map<State, Set<NetworkAction>> globalNetworkActionsResultPerState;

    public GlobalRemedialActionActivationResult(TemporalData<RangeActionActivationResult> rangeActionActivationPerTimestamp, TemporalData<NetworkActionsResult> preventiveTopologicalActions) {
        super(rangeActionActivationPerTimestamp);
        // TODO remove network action copy and renaming
        Map<State, Set<NetworkAction>> globalNetworkActionsResultPerState = new HashMap<>();
        preventiveTopologicalActions.getDataPerTimestamp()
            .forEach((timestamp, networkActionsResult) ->
                networkActionsResult.getActivatedNetworkActionsPerState()
                .forEach((state, networkActions) -> {
                    Set<NetworkAction> networkActionsSetWithTimestamp = new HashSet<>();
                    networkActions.forEach(networkAction -> {
                        networkActionsSetWithTimestamp.add(NetworkActionImpl.copyWithNewId(networkAction, networkAction.getId() + " - " + timestamp.format(DateTimeFormatter.ISO_DATE_TIME)));
                    });
                    globalNetworkActionsResultPerState.put(state, networkActions);
                })
            );

        this.globalNetworkActionsResultPerState = globalNetworkActionsResultPerState;
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return this.globalNetworkActionsResultPerState.values().stream()
            .anyMatch(set -> set.contains(networkAction));
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        // TODO remove this ?
        return this.globalNetworkActionsResultPerState.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    @Override
    public Map<State, Set<NetworkAction>> getActivatedNetworkActionsPerState() {
        return globalNetworkActionsResultPerState;
    }

}
