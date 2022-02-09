/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ObjectiveFunctionHelper {

    private ObjectiveFunctionHelper() {
        // Should not be used
    }

    public static void addMinMarginObjectiveFunction(
        Set<FlowCnec> cnecs,
        FlowResult prePerimeterFlowResult,
        ObjectiveFunction.ObjectiveFunctionBuilder builder,
        boolean hasRelativeMargins,
        UnoptimizedCnecParameters unoptimizedCnecParameters,
        Unit unit
    ) {
        MarginEvaluator marginEvaluator;
        if (hasRelativeMargins) {
            marginEvaluator = FlowResult::getRelativeMargin;
        } else {
            marginEvaluator = FlowResult::getMargin;
        }
        if (unoptimizedCnecParameters != null) {
            builder.withFunctionalCostEvaluator(new MinMarginEvaluator(
                cnecs,
                unit,
                new MarginEvaluatorWithUnoptimizedCnecs(
                    marginEvaluator,
                    unoptimizedCnecParameters.getOperatorsNotToOptimize(),
                    prePerimeterFlowResult
                )
            ));
        } else {
            builder.withFunctionalCostEvaluator(new MinMarginEvaluator(
                cnecs,
                unit,
                marginEvaluator
            ));
        }
    }

}
