/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.searchtreerao.result.api.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class OneStateOnlyRaoResultImpl extends AbstractFlowRaoResult {
    public static final String WRONG_STATE = "Trying to access perimeter result for the wrong state.";
    private final State optimizedState;
    private final PrePerimeterResult initialResult;
    private final OptimizationResult postOptimizationResult;
    private final Set<FlowCnec> optimizedFlowCnecs;
    private OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;

    public OneStateOnlyRaoResultImpl(State optimizedState, PrePerimeterResult initialResult, OptimizationResult postOptimizationResult, Set<FlowCnec> optimizedFlowCnecs) {
        this.optimizedState = optimizedState;
        this.initialResult = initialResult;
        this.postOptimizationResult = postOptimizationResult;
        this.optimizedFlowCnecs = optimizedFlowCnecs;
    }

    private FlowResult getAppropriateResult(Instant optimizedInstant, FlowCnec flowCnec) {
        if (!optimizedFlowCnecs.contains(flowCnec)) {
            throw new OpenRaoException("Cnec not optimized in this perimeter.");
        }
        State state = flowCnec.getState();
        if (optimizedInstant == null) {
            return initialResult;
        }
        if (optimizedState.isPreventive()) {
            return postOptimizationResult;
        }
        if (state.isPreventive()) {
            return initialResult;
        }
        if (!optimizedState.isPreventive()) {
            Contingency optimizedContingency = optimizedState.getContingency().orElseThrow(() -> new OpenRaoException("Should not happen"));
            Contingency contingency = state.getContingency().orElseThrow(() -> new OpenRaoException("Should not happen"));
            if (optimizedContingency.equals(contingency)
                && state.compareTo(optimizedState) >= 0) {
                return postOptimizationResult;
            }
        }
        return initialResult;
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
        return getAppropriateResult(optimizedInstant, flowCnec).getMargin(flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getRelativeMargin(flowCnec, unit);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getFlow(flowCnec, side, unit);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getLoopFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        return getAppropriateResult(optimizedInstant, flowCnec).getPtdfZonalSum(flowCnec, side);
    }

    public OptimizationResult getOptimizationResult(State state) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult;
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
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return false;
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult.getActivatedNetworkActions();
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedSetpoint(rangeAction, state) != initialResult.getSetpoint(rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return initialResult.getTap(pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return initialResult.getSetpoint(rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult.getRangeActions().stream().filter(rangeAction -> isActivatedDuringState(state, rangeAction)).collect(Collectors.toSet());
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedTapsOnState(state);

    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedSetpointsOnState(state);
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        if (this.optimizationStepsExecuted.isOverwritePossible(optimizationStepsExecuted)) {
            this.optimizationStepsExecuted = optimizationStepsExecuted;
        } else {
            throw new OpenRaoException("The RaoResult object should not be modified outside of its usual routine");
        }
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return isSecure(optimizedState.getInstant(), u);
    }

    @Override
    public OptimizationStepsExecuted getOptimizationStepsExecuted() {
        return optimizationStepsExecuted;
    }
}
