/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult extends RangeActionActivationResultImpl {

    public LinearProblemResult(LinearProblem linearProblem, RangeActionSetpointResult prePerimeterSetpoints, OptimizationPerimeter optimizationContext) {
        super(prePerimeterSetpoints);

        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                double setpoint = linearProblem.getRangeActionAbsoluteSetpointVariable(rangeAction, state).solutionValue();
                activate(rangeAction, state, setpoint);
            })
        );
    }
}
