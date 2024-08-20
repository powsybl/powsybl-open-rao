/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;

import java.util.Collections;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class EmptyFlowResultImpl implements FlowResult {

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return Double.NaN;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant instant) {
        return Double.NaN;
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return Double.NaN;
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return Double.NaN;
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return Collections.emptyMap();
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return Double.NaN;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return ComputationStatus.DEFAULT;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return ComputationStatus.DEFAULT;
    }
}
