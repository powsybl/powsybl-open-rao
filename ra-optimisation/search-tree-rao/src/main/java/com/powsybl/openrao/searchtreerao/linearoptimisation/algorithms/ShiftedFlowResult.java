/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.Map;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ShiftedFlowResult implements FlowResult {
    private FlowResult initFlowResult;
    private Map<String, Double> shiftedInjections;
    private SensitivityResult sensitivityResult;

    public ShiftedFlowResult(FlowResult initFlowResult, Map<String, Double> shiftedInjections, SensitivityResult sensitivityResult) {
        this.initFlowResult = initFlowResult;
        this.shiftedInjections = shiftedInjections;
        this.sensitivityResult = sensitivityResult;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        double flow = initFlowResult.getFlow(flowCnec, side, unit);
        for (String shiftedInjection : this.shiftedInjections.keySet()) {
            double sensi = sensitivityResult.getSensitivityValue(flowCnec, side, shiftedInjection, unit);
            flow += sensi * shiftedInjections.get(shiftedInjection);
        }
        return flow;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
        throw new OpenRaoException("Not implemented");
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        throw new OpenRaoException("Not implemented");
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        // throw new OpenRaoException("Not implemented");
        return initFlowResult.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return initFlowResult.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return initFlowResult.getPtdfZonalSums();
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return initFlowResult.getComputationStatus();
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return initFlowResult.getComputationStatus(state);
    }
}
