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
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.Map;

/**
 * It enables to evaluate the absolute margin of a FlowCnec
 * For cnecs parameterized as in series with a pst, margin is considered infinite
 * if the PST has enough setpoints left to absorb the cnec's margin deficit
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MarginEvaluatorWithPstLimitationUnoptimizedCnecs implements MarginEvaluator {
    private final MarginEvaluator marginEvaluator;
    private final Map<FlowCnec, RangeAction<?>> flowCnecRangeActionMap;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpointResult;

    public MarginEvaluatorWithPstLimitationUnoptimizedCnecs(MarginEvaluator marginEvaluator,
                                                            Map<FlowCnec, RangeAction<?>> flowCnecRangeActionMap,
                                                            RangeActionSetpointResult rangeActionActivationResult) {
        this.marginEvaluator = marginEvaluator;
        this.flowCnecRangeActionMap = flowCnecRangeActionMap;
        this.prePerimeterRangeActionSetpointResult = rangeActionActivationResult;
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit) {
        return flowCnec.getMonitoredSides().stream()
                .map(side -> getMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit))
                .min(Double::compareTo).orElseThrow();
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, Side side, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit) {
        if (RaoUtil.cnecShouldBeOptimized(flowCnecRangeActionMap, flowResult, flowCnec, side, rangeActionActivationResult, prePerimeterRangeActionSetpointResult, sensitivityResult, unit)) {
            return marginEvaluator.getMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit);
        } else {
            return Double.MAX_VALUE;
        }
    }
}
