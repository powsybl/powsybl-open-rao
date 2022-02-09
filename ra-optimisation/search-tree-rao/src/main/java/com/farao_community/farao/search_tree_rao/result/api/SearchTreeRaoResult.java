/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.result.api;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;

import java.util.List;
import java.util.Objects;

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

    /**
     * It gives an ordered list of the most constraining elements at a given {@link OptimizationState} according to the
     * objective function defined in the RAO. They are evaluated and ordered from the functional
     * costs of the different CNECs.
     *
     * @param optimizationState: The state of optimization to be studied.
     * @param number: The size of the list to be studied, so the number of limiting elements to be retrieved.
     * @return The ordered list of the n first limiting elements.
     */
    List<FlowCnec> getMostLimitingElements(OptimizationState optimizationState, int number);

    /**
     * It gives an ordered list of the costly {@link FlowCnec} according to the specified virtual cost at a given
     * {@link OptimizationState}. If the virtual is null the list would be empty. If the specified virtual cost does
     * not imply any branch in its computation the list would be empty.
     *
     * @param optimizationState: The state of optimization to be studied.
     * @param virtualCostName: The name of the virtual cost.
     * @param number: The size of the list to be studied, so the number of costly elements to be retrieved.
     * @return The ordered list of the n first costly elements according to the given virtual cost.
     */
    List<FlowCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number);

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
}
