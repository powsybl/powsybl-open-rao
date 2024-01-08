/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.result.impl;

import com.powsybl.open_rao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionSetpointResult;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult extends RangeActionActivationResultImpl {

    public LinearProblemResult(LinearProblem linearProblem, RangeActionSetpointResult prePerimeterSetpoints, OptimizationPerimeter optimizationContext) {
        super(prePerimeterSetpoints);

        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                if (linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state).solutionValue() > 1e-6) {
                    double setpoint = linearProblem.getRangeActionSetpointVariable(rangeAction, state).solutionValue();
                    activate(rangeAction, state, setpoint);
                }
            })
        );
    }
}
