/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

/**
 * Represents the optimization result of automatons
 * Since optimizing automatons is only a simulation of RAs, we only need lists of activated RAs and a sensitivity result
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AutomatonPerimeterResultImpl implements OptimizationResult {

    private final PrePerimeterResult postAutomatonSensitivityAnalysisOutput;
    private final Set<NetworkAction> forcedNetworkActions;
    private final Set<NetworkAction> selectedNetworkActions;
    private final Set<RangeAction<?>> activatedRangeActions;
    private final Map<RangeAction<?>, Double> rangeActionsWithSetpoint;
    private final State optimizedState;

    public AutomatonPerimeterResultImpl(PrePerimeterResult postAutomatonSensitivityAnalysisOutput, Set<NetworkAction> forcedNetworkActions, Set<NetworkAction> selectedNetworkActions, Set<RangeAction<?>> activatedRangeActions, Map<RangeAction<?>, Double> rangeActionsWithSetpoint, State optimizedState) {
        this.postAutomatonSensitivityAnalysisOutput = postAutomatonSensitivityAnalysisOutput;
        this.forcedNetworkActions = forcedNetworkActions;
        this.selectedNetworkActions = selectedNetworkActions;
        this.activatedRangeActions = activatedRangeActions;
        this.rangeActionsWithSetpoint = rangeActionsWithSetpoint;
        this.optimizedState = optimizedState;
    }

    public PrePerimeterResult getPostAutomatonSensitivityAnalysisOutput() {
        return postAutomatonSensitivityAnalysisOutput;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant instant) {
        return postAutomatonSensitivityAnalysisOutput.getFlow(flowCnec, side, unit, instant);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getMargin(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return postAutomatonSensitivityAnalysisOutput.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return postAutomatonSensitivityAnalysisOutput.getPtdfZonalSums();
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return forcedNetworkActions.contains(networkAction) || selectedNetworkActions.contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        Set<NetworkAction> networkActions = new HashSet<>();
        networkActions.addAll(forcedNetworkActions);
        networkActions.addAll(selectedNetworkActions);
        return networkActions;
    }

    public Set<NetworkAction> getForcedNetworkActions() {
        return new HashSet<>(forcedNetworkActions);
    }

    public Set<NetworkAction> getSelectedNetworkActions() {
        return new HashSet<>(selectedNetworkActions);
    }

    @Override
    public double getFunctionalCost() {
        return postAutomatonSensitivityAnalysisOutput.getFunctionalCost();
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return postAutomatonSensitivityAnalysisOutput.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return postAutomatonSensitivityAnalysisOutput.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return postAutomatonSensitivityAnalysisOutput.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return postAutomatonSensitivityAnalysisOutput.getVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return postAutomatonSensitivityAnalysisOutput.getCostlyElements(virtualCostName, number);
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        return postAutomatonSensitivityAnalysisOutput.getObjectiveFunction();
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        postAutomatonSensitivityAnalysisOutput.excludeContingencies(contingenciesToExclude);

    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return rangeActionsWithSetpoint.keySet();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        checkState(state);
        return activatedRangeActions;
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        checkState(state);
        return rangeActionsWithSetpoint.get(rangeAction);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        checkState(state);
        return rangeActionsWithSetpoint;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        checkState(state);
        return pstRangeAction.convertAngleToTap(rangeActionsWithSetpoint.get(pstRangeAction));
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        checkState(state);
        Map<PstRangeAction, Integer> pstRangeActionOptimizedTaps = new HashMap<>();
        activatedRangeActions.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast)
                .forEach(pstRangeAction -> pstRangeActionOptimizedTaps.put(pstRangeAction, getOptimizedTap(pstRangeAction, state)));
        return pstRangeActionOptimizedTaps;
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return postAutomatonSensitivityAnalysisOutput.getSensitivityStatus();
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return postAutomatonSensitivityAnalysisOutput.getSensitivityStatus(state);
    }

    @Override
    public Set<String> getContingencies() {
        return postAutomatonSensitivityAnalysisOutput.getContingencies();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }

    private void checkState(State state) {
        if (!state.equals(optimizedState)) {
            throw new OpenRaoException("State should be " + optimizedState.getId() + " but was " + state.getId());
        }
    }
}
