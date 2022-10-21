/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.Map;

/**
 * It enables to evaluate the absolute or relative minimal margin as a cost
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MarginEvaluatorWithPstLimitationUnoptimizedCnecs implements MarginEvaluator {
    private final MarginEvaluator marginEvaluator;
    private final Map<FlowCnec, PstRangeAction> flowCnecPstRangeActionMap;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpointResult;

    public MarginEvaluatorWithPstLimitationUnoptimizedCnecs(MarginEvaluator marginEvaluator,
                                                            Map<FlowCnec, PstRangeAction> flowCnecPstRangeActionMap,
                                                            RangeActionSetpointResult prePerimeterRangeActionSetpointResult) {
        this.marginEvaluator = marginEvaluator;
        this.flowCnecPstRangeActionMap = flowCnecPstRangeActionMap;
        this.prePerimeterRangeActionSetpointResult = prePerimeterRangeActionSetpointResult;
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit) {
        return flowCnec.getMonitoredSides().stream()
                .map(side -> getMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit))
                .min(Double::compareTo).orElseThrow();
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, Side side, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit) {
        double newMargin = marginEvaluator.getMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit);
        return computeMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit, newMargin);
    }

    private double computeMargin(FlowResult flowResult, FlowCnec flowCnec, Side side, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit, double newMargin) {
        if (flowCnecPstRangeActionMap.containsKey(flowCnec)) {
            PstRangeAction pstRangeAction = flowCnecPstRangeActionMap.get(flowCnec);
            double sensitivity = sensitivityResult.getSensitivityValue(flowCnec, side, pstRangeAction, Unit.MEGAWATT);
            double minSetpoint = pstRangeAction.getMinAdmissibleSetpoint(prePerimeterRangeActionSetpointResult.getSetpoint(pstRangeAction));
            double maxSetpoint = pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterRangeActionSetpointResult.getSetpoint(pstRangeAction));
            // GetOptimizedSetpoint retrieves the latest activated range action's setpoint
            double currentSetpoint = rangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, flowCnec.getState());
            double aboveThresholdMargin = flowCnec.getUpperBound(Side.LEFT, unit).orElse(Double.POSITIVE_INFINITY) - flowResult.getFlow(flowCnec, side, unit);
            double belowThresholdMargin = flowResult.getFlow(flowCnec, side, unit) - flowCnec.getLowerBound(Side.LEFT, unit).orElse(Double.NEGATIVE_INFINITY);

            double aboveThresholdConstraint;
            double belowThresholdConsraint;
            if (sensitivity >= 0) {
                aboveThresholdConstraint = sensitivity * (currentSetpoint - minSetpoint) + aboveThresholdMargin;
                belowThresholdConsraint = sensitivity * (maxSetpoint - currentSetpoint) + belowThresholdMargin;
            } else {
                aboveThresholdConstraint = sensitivity * (currentSetpoint - maxSetpoint) + aboveThresholdMargin;
                belowThresholdConsraint = sensitivity * (minSetpoint - currentSetpoint) + belowThresholdMargin;
            }

            if (aboveThresholdConstraint > 0 && belowThresholdConsraint > 0) {
                return Double.MAX_VALUE;
            }
        }
        return newMargin;
    }
}
