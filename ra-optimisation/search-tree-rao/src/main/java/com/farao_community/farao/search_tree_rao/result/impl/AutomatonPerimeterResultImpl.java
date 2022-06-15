/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.PerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

/**
 * Represents the optimization result of automatons
 * Since optimizing automatons is only a simulation of RAs, we only need lists of activated RAs and a sensitivity result
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AutomatonPerimeterResultImpl implements PerimeterResult {

    private final PrePerimeterResult postAutomatonSensitivityAnalysisOutput;
    private final Set<NetworkAction> activatedNetworkActions;
    private final Set<RangeAction<?>> activatedRangeActions;
    private final Map<RangeAction<?>, Double> rangeActionsWithSetpoint;
    private final State optimizedState;

    public AutomatonPerimeterResultImpl(PrePerimeterResult postAutomatonSensitivityAnalysisOutput, Set<NetworkAction> activatedNetworkActions, Set<RangeAction<?>> activatedRangeActions, Map<RangeAction<?>, Double> rangeActionsWithSetpoint, State optimizedState) {
        this.postAutomatonSensitivityAnalysisOutput = postAutomatonSensitivityAnalysisOutput;
        this.activatedNetworkActions = activatedNetworkActions;
        this.activatedRangeActions = activatedRangeActions;
        this.rangeActionsWithSetpoint =  rangeActionsWithSetpoint;
        this.optimizedState = optimizedState;
    }

    public PrePerimeterResult getPostAutomatonSensitivityAnalysisOutput() {
        return postAutomatonSensitivityAnalysisOutput;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getFlow(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getCommercialFlow(flowCnec, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec) {
        return postAutomatonSensitivityAnalysisOutput.getPtdfZonalSum(flowCnec);
    }

    @Override
    public Map<FlowCnec, Double> getPtdfZonalSums() {
        return postAutomatonSensitivityAnalysisOutput.getPtdfZonalSums();
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return activatedNetworkActions.contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return new HashSet<>(activatedNetworkActions);
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
    public double getSensitivityValue(FlowCnec flowCnec, RangeAction<?> rangeAction, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getSensitivityValue(flowCnec, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, SensitivityVariableSet linearGlsk, Unit unit) {
        return postAutomatonSensitivityAnalysisOutput.getSensitivityValue(flowCnec, linearGlsk, unit);
    }

    private void checkState(State state) {
        if (state != optimizedState) {
            throw new FaraoException("State should be " + optimizedState.getId() + " but was " + state.getId());
        }
    }
}
