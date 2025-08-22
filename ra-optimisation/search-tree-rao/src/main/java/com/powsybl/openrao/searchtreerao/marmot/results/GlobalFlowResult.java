/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * This class aggregates FlowResults stored in TemporalData&lt;FlowResult&gt; in one big FlowResult.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class GlobalFlowResult extends AbstractGlobalResult<FlowResult> implements FlowResult {

    public GlobalFlowResult(TemporalData<FlowResult> flowResultPerTimestamp) {
        super(flowResultPerTimestamp);
    }

    @Override
    public FlowResult getIndividualResult(OffsetDateTime timestamp) {
        return resultPerTimestamp.getData(timestamp).orElseThrow(() -> new OpenRaoException("No individual result"));
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, flowCnec.getState()).getFlow(flowCnec, side, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, flowCnec.getState()).getFlow(flowCnec, side, unit, optimizedInstant);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, flowCnec.getState()).getMargin(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, flowCnec.getState()).getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, flowCnec.getState()).getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums = new HashMap<>();
        resultPerTimestamp.getDataPerTimestamp().values().forEach(flowResult -> ptdfZonalSums.putAll(flowResult.getPtdfZonalSums()));
        return ptdfZonalSums;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return MarmotUtils.getGlobalComputationStatus(resultPerTimestamp, FlowResult::getComputationStatus);
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getComputationStatus(state);
    }
}
