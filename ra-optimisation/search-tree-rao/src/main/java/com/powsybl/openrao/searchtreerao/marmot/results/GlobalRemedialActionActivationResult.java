/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.impl.NetworkActionImpl;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class GlobalRemedialActionActivationResult extends GlobalRangeActionActivationResult implements RemedialActionActivationResult {
    private final NetworkActionsResult globalNetworkActionsResult;

    public GlobalRemedialActionActivationResult(TemporalData<RangeActionActivationResult> rangeActionActivationPerTimestamp, TemporalData<NetworkActionsResult> preventiveTopologicalActions) {
        super(rangeActionActivationPerTimestamp);
        Set<NetworkAction> allPreventiveTopologicalActions = new HashSet<>();
        preventiveTopologicalActions.getDataPerTimestamp().forEach(
            (timestamp, networkActionsResult) -> networkActionsResult.getActivatedNetworkActions().forEach(
                // need to duplicate the network action with the timestamp in the id in case the same network action is applied at different timestamps
                networkAction -> allPreventiveTopologicalActions.add(NetworkActionImpl.copyWithNewId(networkAction, networkAction.getId() + " - " + timestamp.format(DateTimeFormatter.ISO_DATE_TIME)))));
        this.globalNetworkActionsResult = new NetworkActionsResultImpl(allPreventiveTopologicalActions);
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return globalNetworkActionsResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return globalNetworkActionsResult.getActivatedNetworkActions();
    }
}
