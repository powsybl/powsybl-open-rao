/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultImpl implements RaoResult {

    private static final FlowCnecResult DEFAULT_FLOWCNEC_RESULT = new FlowCnecResult();
    private static final AngleCnecResult DEFAULT_ANGLECNEC_RESULT = new AngleCnecResult();
    private static final VoltageCnecResult DEFAULT_VOLTAGECNEC_RESULT = new VoltageCnecResult();
    private static final NetworkActionResult DEFAULT_NETWORKACTION_RESULT = new NetworkActionResult();
    private static final RangeActionResult DEFAULT_RANGEACTION_RESULT = new RangeActionResult();
    private static final CostResult DEFAULT_COST_RESULT = new CostResult();

    private final Crac crac;

    private ComputationStatus sensitivityStatus;
    private final Map<State, ComputationStatus> sensitivityStatusPerState = new HashMap<>();
    private final Map<FlowCnec, FlowCnecResult> flowCnecResults = new HashMap<>();
    private final Map<AngleCnec, AngleCnecResult> angleCnecResults = new HashMap<>();
    private final Map<VoltageCnec, VoltageCnecResult> voltageCnecResults = new HashMap<>();
    private final Map<NetworkAction, NetworkActionResult> networkActionResults = new HashMap<>();
    private final Map<RangeAction<?>, RangeActionResult> rangeActionResults = new HashMap<>();
    private final Map<String, CostResult> costResults = new HashMap<>();

    private OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;

    public RaoResultImpl(Crac crac) {
        this.crac = crac;
    }

    public void setComputationStatus(ComputationStatus computationStatus) {
        this.sensitivityStatus = computationStatus;
    }

    public void setComputationStatus(State state, ComputationStatus computationStatus) {
        this.sensitivityStatusPerState.put(state, computationStatus);
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return sensitivityStatus;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return sensitivityStatusPerState.getOrDefault(state, ComputationStatus.DEFAULT);
    }

    private Instant checkOptimizedInstant(Instant optimizedInstant, FlowCnec flowCnec) {
        if (optimizedInstant == null) {
            return null;
        }
        Instant instant = optimizedInstant;
        if (flowCnec.getState().getInstant().comesBefore(instant)) {
            instant = flowCnec.getState().getInstant();
        }
        if (instant.isOutage()) {
            instant = crac.getPreventiveInstant();
        }
        return instant;
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(checkOptimizedInstant(optimizedInstant, flowCnec)).getFlow(side, unit);
    }

    @Override
    public double getAngle(Instant optimizedInstant, AngleCnec angleCnec, Unit unit) {
        return angleCnecResults.getOrDefault(angleCnec, DEFAULT_ANGLECNEC_RESULT).getResult(optimizedInstant).getAngle(unit);
    }

    @Override
    public double getVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        return voltageCnecResults.getOrDefault(voltageCnec, DEFAULT_VOLTAGECNEC_RESULT).getResult(optimizedInstant).getVoltage(unit);
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(checkOptimizedInstant(optimizedInstant, flowCnec)).getMargin(unit);
    }

    @Override
    public double getMargin(Instant optimizedInstant, AngleCnec angleCnec, Unit unit) {
        return angleCnecResults.getOrDefault(angleCnec, DEFAULT_ANGLECNEC_RESULT).getResult(optimizedInstant).getMargin(unit);
    }

    @Override
    public double getMargin(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        return voltageCnecResults.getOrDefault(voltageCnec, DEFAULT_VOLTAGECNEC_RESULT).getResult(optimizedInstant).getMargin(unit);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(checkOptimizedInstant(optimizedInstant, flowCnec)).getRelativeMargin(unit);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(checkOptimizedInstant(optimizedInstant, flowCnec)).getLoopFlow(side, unit);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(checkOptimizedInstant(optimizedInstant, flowCnec)).getCommercialFlow(side, unit);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, Side side) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(checkOptimizedInstant(optimizedInstant, flowCnec)).getPtdfZonalSum(side);
    }

    public FlowCnecResult getAndCreateIfAbsentFlowCnecResult(FlowCnec flowCnec) {
        flowCnecResults.putIfAbsent(flowCnec, new FlowCnecResult());
        return flowCnecResults.get(flowCnec);
    }

    public AngleCnecResult getAndCreateIfAbsentAngleCnecResult(AngleCnec angleCnec) {
        angleCnecResults.putIfAbsent(angleCnec, new AngleCnecResult());
        return angleCnecResults.get(angleCnec);
    }

    public VoltageCnecResult getAndCreateIfAbsentVoltageCnecResult(VoltageCnec voltageCnec) {
        voltageCnecResults.putIfAbsent(voltageCnec, new VoltageCnecResult());
        return voltageCnecResults.get(voltageCnec);
    }

    public CostResult getAndCreateIfAbsentCostResult(String optimizedInstantId) {
        costResults.putIfAbsent(optimizedInstantId, new CostResult());
        return costResults.get(optimizedInstantId);
    }

    @Override
    public double getCost(Instant optimizedInstant) {
        String id = getIdFromNullableInstant(optimizedInstant);
        return costResults.getOrDefault(id, DEFAULT_COST_RESULT).getCost();
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        String id = getIdFromNullableInstant(optimizedInstant);
        return costResults.getOrDefault(id, DEFAULT_COST_RESULT).getFunctionalCost();
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        String id = getIdFromNullableInstant(optimizedInstant);
        return costResults.getOrDefault(id, DEFAULT_COST_RESULT).getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return costResults.values().stream().flatMap(c -> c.getVirtualCostNames().stream()).collect(Collectors.toSet());
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        String id = getIdFromNullableInstant(optimizedInstant);
        return costResults.getOrDefault(id, DEFAULT_COST_RESULT).getVirtualCost(virtualCostName);
    }

    private static String getIdFromNullableInstant(Instant optimizedInstant) {
        return optimizedInstant == null ? INITIAL_INSTANT_ID : optimizedInstant.getId();
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

    public NetworkActionResult getAndCreateIfAbsentNetworkActionResult(NetworkAction networkAction) {
        networkActionResults.putIfAbsent(networkAction, new NetworkActionResult());
        return networkActionResults.get(networkAction);
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        if (state.isPreventive() || state.getContingency().isEmpty()) {
            return false;
        }

        // if it is activated in the preventive state, return true
        if (networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT)
                .getStatesWithActivation().stream()
                .anyMatch(State::isPreventive)) {
            return true;
        }

        return networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT)
                .getStatesWithActivation().stream()
                .filter(st -> st.getContingency().isPresent())
                .filter(st -> st.getInstant().getOrder() < state.getInstant().getOrder())
                .anyMatch(st -> st.getContingency().get().getId().equals(state.getContingency().get().getId()));
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT).getStatesWithActivation().contains(state);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return networkActionResults.entrySet().stream()
                .filter(e -> e.getValue().getStatesWithActivation().contains(state))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public RangeActionResult getAndCreateIfAbsentRangeActionResult(RangeAction<?> rangeAction) {
        rangeActionResults.putIfAbsent(rangeAction, new RangeActionResult());
        return rangeActionResults.get(rangeAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return rangeActionResults.getOrDefault(rangeAction, DEFAULT_RANGEACTION_RESULT).isActivatedDuringState(state);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return pstRangeAction.convertAngleToTap(getPreOptimizationSetPointOnState(state, pstRangeAction));
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return pstRangeAction.convertAngleToTap(getOptimizedSetPointOnState(state, pstRangeAction));
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (state.isPreventive()) {
            return rangeActionResults.getOrDefault(rangeAction, DEFAULT_RANGEACTION_RESULT).getInitialSetpoint();
        } else {
            return getOptimizedSetPointOnState(stateBefore(state), rangeAction);
        }
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        State stateBefore = state;
        // Search for any RA with same network element that has been activated before given state
        while (Objects.nonNull(stateBefore)) {
            final State finalStateBefore = stateBefore;
            Optional<Map.Entry<RangeAction<?>, RangeActionResult>> activatedRangeAction =
                    rangeActionResults.entrySet().stream().filter(entry ->
                    entry.getKey().getNetworkElements().equals(rangeAction.getNetworkElements())
                            && entry.getValue().isActivatedDuringState(finalStateBefore)).findAny();
            if (activatedRangeAction.isPresent()) {
                return activatedRangeAction.get().getValue().getOptimizedSetpointOnState(stateBefore);
            }
            stateBefore = stateBefore(stateBefore);
        }
        // If no activated RA was found, return initial setpoint
        return getPreOptimizationSetPointOnState(crac.getPreventiveState(), rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return rangeActionResults.entrySet().stream()
                .filter(e -> e.getValue().isActivatedDuringState(state))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return crac.getPstRangeActions().stream().collect(Collectors.toMap(Function.identity(), pst -> getOptimizedTapOnState(state, pst)));
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return crac.getRangeActions().stream().collect(Collectors.toMap(Function.identity(), ra -> getOptimizedSetPointOnState(state, ra)));
    }

    private State stateBefore(State state) {
        if (state.getContingency().isPresent()) {
            return stateBefore(state.getContingency().orElseThrow().getId(), state.getInstant());
        } else {
            return null;
        }
    }

    private State stateBefore(String contingencyId, Instant instant) {
        if (instant.isOutage()) {
            return crac.getPreventiveState();
        }
        State stateBefore = lookupState(contingencyId, crac.getInstantBefore(instant));
        if (Objects.nonNull(stateBefore)) {
            return stateBefore;
        } else {
            return stateBefore(contingencyId, crac.getInstantBefore(instant));
        }
    }

    private State lookupState(String contingencyId, Instant instant) {
        return crac.getStates(instant).stream()
                .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingencyId))
                .findAny()
                .orElse(null);
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        if (this.optimizationStepsExecuted.equals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY)) {
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
