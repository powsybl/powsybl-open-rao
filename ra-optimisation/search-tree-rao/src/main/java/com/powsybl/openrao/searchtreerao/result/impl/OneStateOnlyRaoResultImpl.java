/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getDuplicateCnecs;

/**
 * Implementation of {@link com.powsybl.openrao.data.raoresult.api.RaoResult} used when only one state
 * was optimized by the RAO. The methods are not expected to be called with other states.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class OneStateOnlyRaoResultImpl extends AbstractFlowRaoResult {
    private final State optimizedState;
    private final PrePerimeterResult initialResult;
    private final OptimizationResult postOptimizationResult;
    private final Set<FlowCnec> optimizedFlowCnecs;
    private String executionDetails = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;

    public OneStateOnlyRaoResultImpl(State optimizedState, PrePerimeterResult initialResult, OptimizationResult postOptimizationResult, Set<FlowCnec> optimizedFlowCnecs) {
        this.optimizedState = optimizedState;
        this.initialResult = initialResult;
        this.postOptimizationResult = postOptimizationResult;
        this.optimizedFlowCnecs = optimizedFlowCnecs;
        excludeDuplicateCnec();
    }

    private Optional<FlowResult> getAppropriateResult(Instant optimizedInstant, FlowCnec flowCnec) {
        if (!optimizedFlowCnecs.contains(flowCnec)) {
            return Optional.empty();
        }
        State state = flowCnec.getState();
        if (optimizedInstant == null) {
            return Optional.of(initialResult);
        }
        if (optimizedState.isPreventive()) {
            return Optional.of(postOptimizationResult);
        }
        if (state.isPreventive()) {
            return Optional.of(initialResult);
        }
        if (!optimizedState.isPreventive()) {
            Contingency optimizedContingency = optimizedState.getContingency().orElseThrow(() -> new OpenRaoException("Should not happen"));
            Contingency contingency = state.getContingency().orElseThrow(() -> new OpenRaoException("Should not happen"));
            if (optimizedContingency.equals(contingency)
                && state.compareTo(optimizedState) >= 0) {
                return Optional.of(postOptimizationResult);
            }
        }
        return Optional.of(initialResult);
    }

    public OptimizationResult getPostOptimizationResult() {
        return postOptimizationResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (initialResult.getSensitivityStatus() == ComputationStatus.FAILURE || postOptimizationResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
            return ComputationStatus.FAILURE;
        }
        if (initialResult.getSensitivityStatus() == postOptimizationResult.getSensitivityStatus()) {
            return initialResult.getSensitivityStatus();
        }
        return ComputationStatus.DEFAULT;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return postOptimizationResult.getSensitivityStatus(state);
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).map(flowResult -> flowResult.getMargin(flowCnec, unit)).orElse(Double.NaN);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).map(flowResult -> flowResult.getRelativeMargin(flowCnec, unit)).orElse(Double.NaN);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).map(flowResult -> flowResult.getFlow(flowCnec, side, unit)).orElse(Double.NaN);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).map(flowResult -> flowResult.getCommercialFlow(flowCnec, side, unit)).orElse(Double.NaN);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).map(flowResult -> flowResult.getLoopFlow(flowCnec, side, unit)).orElse(Double.NaN);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        return getAppropriateResult(optimizedInstant, flowCnec).map(flowResult -> flowResult.getPtdfZonalSum(flowCnec, side)).orElse(Double.NaN);
    }

    public PrePerimeterResult getInitialResult() {
        return initialResult;
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getFunctionalCost();
        } else {
            return postOptimizationResult.getFunctionalCost();
        }
    }

    public List<FlowCnec> getMostLimitingElements(Instant optimizedInstant, int number) {
        if (optimizedInstant == null) {
            return initialResult.getMostLimitingElements(number);
        } else {
            return postOptimizationResult.getMostLimitingElements(number);
        }
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost();
        } else {
            return postOptimizationResult.getVirtualCost();
        }
    }

    @Override
    public Set<String> getVirtualCostNames() {
        Set<String> virtualCostNames = new HashSet<>();
        if (initialResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(initialResult.getVirtualCostNames());
        }
        if (postOptimizationResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(postOptimizationResult.getVirtualCostNames());
        }
        return virtualCostNames;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost(virtualCostName);
        } else {
            return postOptimizationResult.getVirtualCost(virtualCostName);
        }
    }

    public List<FlowCnec> getCostlyElements(Instant optimizedInstant, String virtualCostName, int number) {
        if (optimizedInstant == null) {
            return initialResult.getCostlyElements(virtualCostName, number);
        } else {
            return postOptimizationResult.getCostlyElements(virtualCostName, number);
        }
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return state.equals(optimizedState) && postOptimizationResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return state.equals(optimizedState) ? postOptimizationResult.getActivatedNetworkActions() : Set.of();
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return state.equals(optimizedState) && postOptimizationResult.getOptimizedSetpoint(rangeAction, state) != initialResult.getSetpoint(rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return initialResult.getTap(pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return state.equals(optimizedState) ? postOptimizationResult.getOptimizedTap(pstRangeAction, state) : initialResult.getTap(pstRangeAction);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return initialResult.getSetpoint(rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return state.equals(optimizedState) ? postOptimizationResult.getOptimizedSetpoint(rangeAction, state) : initialResult.getSetpoint(rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return postOptimizationResult.getRangeActions().stream().filter(rangeAction -> isActivatedDuringState(state, rangeAction)).collect(Collectors.toSet());
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return state.equals(optimizedState) ? postOptimizationResult.getOptimizedTapsOnState(state) : Map.of();
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return state.equals(optimizedState) ? postOptimizationResult.getOptimizedSetpointsOnState(state) : Map.of();
    }

    @Override
    public void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return isSecure(optimizedState.getInstant(), u);
    }

    @Override
    public String getExecutionDetails() {
        return executionDetails;
    }

    private void excludeDuplicateCnec() {
        if (optimizedState != null) {
            Set<String> cnecsToExclude = getDuplicateCnecs(optimizedFlowCnecs);
            postOptimizationResult.excludeCnecs(cnecsToExclude);
        }

    }
}
