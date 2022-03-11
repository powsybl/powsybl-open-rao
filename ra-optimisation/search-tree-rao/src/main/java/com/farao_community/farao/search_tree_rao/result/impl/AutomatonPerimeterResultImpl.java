/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.PerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the optimization result of automatons
 * Since optimizing automatons is only a simulation of RAs, we only need lists of activated RAs and a sensitivity result
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AutomatonPerimeterResultImpl implements PerimeterResult {

    private final PrePerimeterResult postAutomatonSensitivityAnalysisOutput;
    private final Set<NetworkAction> activatedNetworkActions;

    public AutomatonPerimeterResultImpl(PrePerimeterResult postAutomatonSensitivityAnalysisOutput, Set<NetworkAction> activatedNetworkActions) {
        this.postAutomatonSensitivityAnalysisOutput = postAutomatonSensitivityAnalysisOutput;
        this.activatedNetworkActions = activatedNetworkActions;
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
    public Set<RangeAction<?>> getActivatedRangeActions() {
        return Set.of();
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
        return postAutomatonSensitivityAnalysisOutput.getRangeActions();
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return postAutomatonSensitivityAnalysisOutput.getOptimizedTap(pstRangeAction);
    }

    @Override
    public double getOptimizedSetPoint(RangeAction<?> rangeAction) {
        return postAutomatonSensitivityAnalysisOutput.getOptimizedSetPoint(rangeAction);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return postAutomatonSensitivityAnalysisOutput.getOptimizedTaps();
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPoints() {
        return postAutomatonSensitivityAnalysisOutput.getOptimizedSetPoints();
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
}
