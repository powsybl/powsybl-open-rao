/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.results.FlowResult;

import java.util.*;

/**
 * It enables to evaluate the absolute or relative minimal margin as a cost
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MarginEvaluatorWithUnoptimizedCnecs implements MarginEvaluator {
    private final MarginEvaluator marginEvaluator;
    private final Set<String> countriesNotToOptimize;
    private final FlowResult prePerimeterFlowResult;

    public MarginEvaluatorWithUnoptimizedCnecs(MarginEvaluator marginEvaluator,
                                               Set<String> countriesNotToOptimize,
                                               FlowResult prePerimeterFlowResult) {
        this.marginEvaluator = marginEvaluator;
        this.countriesNotToOptimize = countriesNotToOptimize;
        this.prePerimeterFlowResult = prePerimeterFlowResult;
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, Unit unit) {
        double newMargin = marginEvaluator.getMargin(flowResult, flowCnec, unit);
        if (countriesNotToOptimize.contains(flowCnec.getOperator())) {
            double prePerimeterMargin = marginEvaluator.getMargin(prePerimeterFlowResult, flowCnec, unit);
            if (newMargin > prePerimeterMargin - .0001 * Math.abs(prePerimeterMargin)) {
                return Double.MAX_VALUE;
            }
        }
        return newMargin;
    }
}
