/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface SearchTreeRaoResult extends RaoResult {

    /**
     * It enables to access to a {@link PerimeterResult} which is a sub-representation of the {@link RaoResult}. Be
     * careful because some combinations of {@code optimizationState} and {@code state} can be quite tricky to
     * analyze.
     *
     * @param optimizationState: The state of optimization to be studied.
     * @param state: The state of the state tree to be studied.
     * @return The full perimeter result to be studied with comprehensive data.
     */
    PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state);

    /**
     * It enables to access to the preventive {@link PerimeterResult} after PRA which is a sub-representation of the
     * {@link RaoResult}.
     *
     * @return The full preventive perimeter result to be studied with comprehensive data.
     */
    PerimeterResult getPostPreventivePerimeterResult();

    /**
     * It enables to access to the initial {@link PerimeterResult} which is a sub-representation of the {@link RaoResult}.
     *
     * @return The full initial perimeter result to be studied with comprehensive data.
     */
    PrePerimeterResult getInitialResult();

    //todo: javadoc
    Set<PerimeterResult> getAllPerimeterResults();

    @Override
    default Set<FlowCnec> getFlowCnecs() {
        Set<FlowCnec> flowCnecs = getInitialResult().getFlowCnecs();
        getAllPerimeterResults().forEach(perimeterResult -> flowCnecs.addAll(perimeterResult.getFlowCnecs()));
        return flowCnecs;
    }

    @Override
    default double getFlow(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        if (optimizationState.equals(OptimizationState.INITIAL)) {
            return getInitialResult().getFlow(flowCnec, unit);
        } else if (optimizationState.equals(OptimizationState.AFTER_PRA)
            || Objects.isNull(getPerimeterResult(optimizationState, flowCnec.getState()))) {
            return getPostPreventivePerimeterResult().getFlow(flowCnec, unit);
        } else {
            return getPerimeterResult(optimizationState, flowCnec.getState()).getFlow(flowCnec, unit);
        }
    }

    @Override
    default double getMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        if (optimizationState.equals(OptimizationState.INITIAL)) {
            return getInitialResult().getMargin(flowCnec, unit);
        } else if (optimizationState.equals(OptimizationState.AFTER_PRA)
            || Objects.isNull(getPerimeterResult(optimizationState, flowCnec.getState()))) {
            return getPostPreventivePerimeterResult().getMargin(flowCnec, unit);
        } else {
            return getPerimeterResult(optimizationState, flowCnec.getState()).getMargin(flowCnec, unit);
        }
    }

    @Override
    default double getRelativeMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        if (optimizationState.equals(OptimizationState.INITIAL)) {
            return getInitialResult().getRelativeMargin(flowCnec, unit);
        } else if (optimizationState.equals(OptimizationState.AFTER_PRA)
            || Objects.isNull(getPerimeterResult(optimizationState, flowCnec.getState()))) {
            return getPostPreventivePerimeterResult().getRelativeMargin(flowCnec, unit);
        } else {
            return getPerimeterResult(optimizationState, flowCnec.getState()).getRelativeMargin(flowCnec, unit);
        }
    }

    @Override
    default double getCommercialFlow(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        if (optimizationState.equals(OptimizationState.INITIAL)) {
            return getInitialResult().getCommercialFlow(flowCnec, unit);
        } else if (optimizationState.equals(OptimizationState.AFTER_PRA)
            || Objects.isNull(getPerimeterResult(optimizationState, flowCnec.getState()))) {
            return getPostPreventivePerimeterResult().getCommercialFlow(flowCnec, unit);
        } else {
            return getPerimeterResult(optimizationState, flowCnec.getState()).getCommercialFlow(flowCnec, unit);
        }
    }

    @Override
    default double getLoopFlow(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        if (optimizationState.equals(OptimizationState.INITIAL)) {
            return getInitialResult().getLoopFlow(flowCnec, unit);
        } else if (optimizationState.equals(OptimizationState.AFTER_PRA)
            || Objects.isNull(getPerimeterResult(optimizationState, flowCnec.getState()))) {
            return getPostPreventivePerimeterResult().getLoopFlow(flowCnec, unit);
        } else {
            return getPerimeterResult(optimizationState, flowCnec.getState()).getLoopFlow(flowCnec, unit);
        }
    }

    @Override
    default double getPtdfZonalSum(OptimizationState optimizationState, FlowCnec flowCnec) {
        if (optimizationState.equals(OptimizationState.INITIAL)) {
            return getInitialResult().getPtdfZonalSum(flowCnec);
        } else if (optimizationState.equals(OptimizationState.AFTER_PRA)
            || Objects.isNull(getPerimeterResult(optimizationState, flowCnec.getState()))) {
            return getPostPreventivePerimeterResult().getPtdfZonalSum(flowCnec);
        } else {
            return getPerimeterResult(optimizationState, flowCnec.getState()).getPtdfZonalSum(flowCnec);
        }
    }

    @Override
    default double getCost(OptimizationState optimizationState) {
        return getFunctionalCost(optimizationState) + getVirtualCost(optimizationState);
    }

    @Override
    default Set<NetworkAction> getNetworkActions() {
        Set<NetworkAction> networkActions = new HashSet<>();
        getAllPerimeterResults().forEach(perimeterResult -> networkActions.addAll(perimeterResult.getNetworkActions()));
        return networkActions;
    }
}
