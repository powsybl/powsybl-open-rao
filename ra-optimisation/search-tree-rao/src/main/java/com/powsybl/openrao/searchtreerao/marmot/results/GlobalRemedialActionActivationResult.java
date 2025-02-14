/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class GlobalRemedialActionActivationResult extends GlobalRangeActionActivationResult implements RemedialActionActivationResult {
    public GlobalRemedialActionActivationResult(TemporalData<RangeActionActivationResult> rangeActionActivationPerTimestamp) {
        super(rangeActionActivationPerTimestamp);
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return Set.of();
    }
}
