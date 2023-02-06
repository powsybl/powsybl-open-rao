/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A RaoResult implementation that contains the initial situation before optimization
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UnoptimizedRaoResultImpl implements RaoResult {
    private final PrePerimeterResult initialResult;
    private OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;

    public UnoptimizedRaoResultImpl(PrePerimeterResult initialResult) {
        this.initialResult = initialResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return initialResult.getSensitivityStatus();
    }

    @Override
    public double getFlow(OptimizationState optimizationState, FlowCnec flowCnec, Side side, Unit unit) {
        return initialResult.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getAngle(OptimizationState optimizationState, AngleCnec angleCnec, Unit unit) {
        throw new FaraoException("Angle cnecs are not computed in the rao");
    }

    @Override
    public double getVoltage(OptimizationState optimizationState, VoltageCnec voltageCnec, Unit unit) {
        throw new FaraoException("Voltage cnecs are not computed in the rao");
    }

    @Override
    public double getMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return initialResult.getMargin(flowCnec, unit);
    }

    @Override
    public double getMargin(OptimizationState optimizationState, AngleCnec angleCnec, Unit unit) {
        throw new FaraoException("Angle cnecs are not computed in the rao");
    }

    @Override
    public double getMargin(OptimizationState optimizationState, VoltageCnec voltageCnec, Unit unit) {
        throw new FaraoException("Voltage cnecs are not computed in the rao");
    }

    @Override
    public double getRelativeMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return initialResult.getRelativeMargin(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(OptimizationState optimizationState, FlowCnec flowCnec, Side side, Unit unit) {
        return initialResult.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getLoopFlow(OptimizationState optimizationState, FlowCnec flowCnec, Side side, Unit unit) {
        return initialResult.getLoopFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(OptimizationState optimizationState, FlowCnec flowCnec, Side side) {
        return initialResult.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public double getCost(OptimizationState optimizationState) {
        return initialResult.getCost();
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        return initialResult.getFunctionalCost();
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        return initialResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return initialResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        return initialResult.getVirtualCost(virtualCostName);
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        if (remedialAction instanceof NetworkAction) {
            return isActivatedDuringState(state, (NetworkAction) remedialAction);
        } else if (remedialAction instanceof RangeAction<?>) {
            return isActivatedDuringState(state, (RangeAction<?>) remedialAction);
        } else {
            throw new FaraoException("Unrecognized remedial action type");
        }
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return new HashSet<>();
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return false;
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return initialResult.getTap(pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return getPreOptimizationTapOnState(state, pstRangeAction);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return initialResult.getSetpoint(rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return getPreOptimizationSetPointOnState(state, rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return new HashSet<>();
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        Map<PstRangeAction, Integer> tapPerPst = new HashMap<>();
        initialResult.getRangeActions().forEach(ra -> {
            if (ra instanceof PstRangeAction) {
                tapPerPst.put((PstRangeAction) ra, initialResult.getTap((PstRangeAction) ra));
            }
        });
        return tapPerPst;
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        Map<RangeAction<?>, Double> setpointPerRa = new HashMap<>();
        initialResult.getRangeActions().forEach(ra ->
            setpointPerRa.put(ra, initialResult.getSetpoint(ra))
        );
        return setpointPerRa;
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        if (this.optimizationStepsExecuted.isOverwritePossible(optimizationStepsExecuted)) {
            this.optimizationStepsExecuted = optimizationStepsExecuted;
        } else {
            throw new FaraoException("The RaoResult object should not be modified outside of its usual routine");
        }
    }

    @Override
    public OptimizationStepsExecuted getOptimizationStepsExecuted() {
        return optimizationStepsExecuted;
    }
}
