/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class InterTemporalPrePerimeterResult implements SensitivityResult, FlowResult {
    private final TemporalData<PrePerimeterResult> systematicSensitivityResults;

    public InterTemporalPrePerimeterResult(TemporalData<PrePerimeterResult> systematicSensitivityResults) {
        this.systematicSensitivityResults = systematicSensitivityResults;
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        AtomicBoolean hasPartialFailure = new AtomicBoolean(false);
        systematicSensitivityResults.getDataPerTimestamp().values().forEach(
            prePerimeterResult -> {
                switch (prePerimeterResult.getSensitivityStatus()) {
                    case PARTIAL_FAILURE -> hasPartialFailure.set(true);
                    case FAILURE -> hasFailure.set(true);
                }
            }
        );
        if (hasFailure.get()) {
            return ComputationStatus.FAILURE;
        } else if (hasPartialFailure.get()) {
            return ComputationStatus.PARTIAL_FAILURE;
        } else {
            return ComputationStatus.DEFAULT;
        }
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return getPrePerimeterFromState(state).getSensitivityStatus(state);
    }

    @Override
    public Set<String> getContingencies() {
        Set<String> contingencies = new HashSet<>();
        systematicSensitivityResults.getDataPerTimestamp().values().forEach(prePerimeterResult -> contingencies.addAll(prePerimeterResult.getContingencies()));
        return contingencies;
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return getPrePerimeterFromState(flowCnec.getState()).getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return getPrePerimeterFromState(flowCnec.getState()).getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getPrePerimeterFromState(flowCnec.getState()).getFlow(flowCnec, side, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
        return getPrePerimeterFromState(flowCnec.getState()).getFlow(flowCnec, side, unit, optimizedInstant);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return getPrePerimeterFromState(flowCnec.getState()).getMargin(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getPrePerimeterFromState(flowCnec.getState()).getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return getPrePerimeterFromState(flowCnec.getState()).getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums = new HashMap<>();
        systematicSensitivityResults.getDataPerTimestamp().values().forEach(
            prePerimeterResult -> ptdfZonalSums.putAll(prePerimeterResult.getPtdfZonalSums())
        );
        return ptdfZonalSums;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        AtomicBoolean hasPartialFailure = new AtomicBoolean(false);
        systematicSensitivityResults.getDataPerTimestamp().values().forEach(
            prePerimeterResult -> {
                switch (prePerimeterResult.getComputationStatus()) {
                    case PARTIAL_FAILURE -> hasPartialFailure.set(true);
                    case FAILURE -> hasFailure.set(true);
                }
            }
        );
        if (hasFailure.get()) {
            return ComputationStatus.FAILURE;
        } else if (hasPartialFailure.get()) {
            return ComputationStatus.PARTIAL_FAILURE;
        } else {
            return ComputationStatus.DEFAULT;
        }
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return getPrePerimeterFromState(state).getComputationStatus(state);
    }

    private PrePerimeterResult getPrePerimeterFromState(State state) {
        // TODO: use state's timestamp when dedicated PR is merged
        return systematicSensitivityResults.getDataPerTimestamp().values().iterator().next();
    }
}
