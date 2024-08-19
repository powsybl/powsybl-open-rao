/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CurativeWithSecondPraoResult implements OptimizationResult {

    private final State state; // the optimized state of the curative RAO
    private final OptimizationResult firstCraoResult; // contains information about the perimeter and activated network actions
    private final OptimizationResult secondPraoResult; // contains information about activated range actions
    private final Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive; // information about whether CRAs were re-optimized in 2nd PRAO
    private final FlowResult postCraSensitivityFlowResult; // contains final flows
    private final ObjectiveFunctionResult postCraSensitivityObjectiveResult; // contains final flows
    private final SensitivityResult postCraSensitivitySensitivityResult; // contains final flows

    private CurativeWithSecondPraoResult(State state, OptimizationResult firstCraoResult, OptimizationResult secondPraoResult, Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive, FlowResult postCraSensitivityFlowResult, ObjectiveFunctionResult postCraSensitivityObjectiveResult, SensitivityResult postCraSensitivitySensitivityResult) {
        this.state = state;
        this.firstCraoResult = firstCraoResult;
        this.secondPraoResult = secondPraoResult;
        this.remedialActionsExcludedFromSecondPreventive = remedialActionsExcludedFromSecondPreventive;
        this.postCraSensitivityFlowResult = postCraSensitivityFlowResult;
        this.postCraSensitivityObjectiveResult = postCraSensitivityObjectiveResult;
        this.postCraSensitivitySensitivityResult = postCraSensitivitySensitivityResult;
    }

    public CurativeWithSecondPraoResult(State state, OptimizationResult firstCraoResult, OptimizationResult secondPraoResult, Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive, PrePerimeterResult postCraPrePerimeterResult) {
        this(state, firstCraoResult, secondPraoResult, remedialActionsExcludedFromSecondPreventive, postCraPrePerimeterResult, postCraPrePerimeterResult, postCraPrePerimeterResult);
    }

    private void checkState(State stateToCheck) {
        if (!state.equals(stateToCheck)) {
            throw new OpenRaoException(String.format("State %s is not the same as this result's state (%s)", stateToCheck, state.getId()));
        }
    }

    private void checkCnec(Cnec<?> cnec) {
        if (!cnec.getState().getContingency().equals(state.getContingency())) {
            throw new OpenRaoException(String.format("Cnec %s has a different contingency than this result's state (%s)", cnec.getId(), state.getId()));
        }
    }

    private boolean isCraIncludedInSecondPreventiveRao(RemedialAction<?> remedialAction) {
        return !remedialActionsExcludedFromSecondPreventive.contains(remedialAction);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        checkCnec(flowCnec);
        return postCraSensitivityFlowResult.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant instant) {
        checkCnec(flowCnec);
        return postCraSensitivityFlowResult.getFlow(flowCnec, side, unit, instant);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        checkCnec(flowCnec);
        return postCraSensitivityFlowResult.getMargin(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        checkCnec(flowCnec);
        return postCraSensitivityFlowResult.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        checkCnec(flowCnec);
        return postCraSensitivityFlowResult.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return postCraSensitivityFlowResult.getPtdfZonalSums();
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        if (isCraIncludedInSecondPreventiveRao(networkAction)) {
            return secondPraoResult.isActivated(networkAction);
        } else {
            return firstCraoResult.isActivated(networkAction);
        }
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        // Hard to check which were included in 2nd preventive RAO. We'll suppose none was.
        return firstCraoResult.getActivatedNetworkActions();
    }

    @Override
    public double getFunctionalCost() {
        // Careful : this returns functional cost over all curative perimeters, but it should be enough for normal use
        // since we never really need functional cost per perimeter at the end of the RAO
        return postCraSensitivityObjectiveResult.getFunctionalCost();
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        // Careful : this returns most limiting elements over all curative perimeters, but it should be enough for normal use
        return postCraSensitivityObjectiveResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        // Careful : this returns virtual cost over all curative perimeters, but it should be enough for normal use
        return postCraSensitivityObjectiveResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return postCraSensitivityObjectiveResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        // Careful : this returns virtual cost over all curative perimeters, but it should be enough for normal use
        return postCraSensitivityObjectiveResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        // Careful : this returns costly elements over all curative perimeters, but it should be enough for normal use
        return postCraSensitivityObjectiveResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        return postCraSensitivityObjectiveResult.getObjectiveFunction();
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        firstCraoResult.excludeContingencies(contingenciesToExclude);
        secondPraoResult.excludeContingencies(contingenciesToExclude);
        postCraSensitivityObjectiveResult.excludeContingencies(contingenciesToExclude);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        // Some range actions can be excluded from first CRAO (for example if they are only available after a constraint)
        // but re-optimised in second PRAO
        Set<RangeAction<?>> rangeActions = new HashSet<>(firstCraoResult.getRangeActions());
        rangeActions.addAll(secondPraoResult.getRangeActions());
        return rangeActions;
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        checkState(state);
        Set<RangeAction<?>> activated = firstCraoResult.getActivatedRangeActions(state).stream().filter(ra -> !isCraIncludedInSecondPreventiveRao(ra)).collect(Collectors.toSet());
        activated.addAll(secondPraoResult.getActivatedRangeActions(state));
        return activated;
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        checkState(state);
        if (isCraIncludedInSecondPreventiveRao(rangeAction)) {
            return secondPraoResult.getOptimizedSetpoint(rangeAction, state);
        } else {
            return firstCraoResult.getOptimizedSetpoint(rangeAction, state);
        }
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        checkState(state);
        return firstCraoResult.getRangeActions().stream().collect(Collectors.toMap(ra -> ra, ra -> getOptimizedSetpoint(ra, state)));
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        checkState(state);
        if (isCraIncludedInSecondPreventiveRao(pstRangeAction)) {
            return secondPraoResult.getOptimizedTap(pstRangeAction, state);
        } else {
            return firstCraoResult.getOptimizedTap(pstRangeAction, state);
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        checkState(state);
        return firstCraoResult.getRangeActions().stream()
            .filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast)
            .collect(Collectors.toMap(pst -> pst, pst -> getOptimizedTap(pst, state)));
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return postCraSensitivitySensitivityResult.getSensitivityStatus();
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return postCraSensitivitySensitivityResult.getSensitivityStatus(state);
    }

    @Override
    public Set<String> getContingencies() {
        Set<String> allContingencies = firstCraoResult.getContingencies();
        allContingencies.addAll(secondPraoResult.getContingencies());
        return allContingencies;
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        checkCnec(flowCnec);
        if (!firstCraoResult.getRangeActions().contains(rangeAction)) {
            throw new OpenRaoException(String.format("RangeAction %s does not belong to this result's state (%s)", rangeAction.getId(), state));
        }
        return postCraSensitivitySensitivityResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        checkCnec(flowCnec);
        return postCraSensitivitySensitivityResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }
}
