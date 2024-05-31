/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;

import java.util.Collections;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class EmptyFlowResultImpl implements FlowResult {

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit, Instant instant) {
        return Double.NaN;
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        return Double.NaN;
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        return Double.NaN;
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        return Collections.emptyMap();
    }
}
