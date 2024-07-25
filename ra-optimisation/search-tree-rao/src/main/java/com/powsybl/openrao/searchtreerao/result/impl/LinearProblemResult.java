/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;

import java.util.List;

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

    public LinearProblemResult(LinearProblem linearProblem, RangeActionSetpointResult prePerimeterSetpoints, List<OptimizationPerimeter> optimizationContexts) {
        super(prePerimeterSetpoints);

        optimizationContexts.forEach(optimizationPerimeter -> optimizationPerimeter.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                double setpoint = linearProblem.getRangeActionSetpointVariable(rangeAction, state).solutionValue();
                activate(rangeAction, state, setpoint);
            })
        ));
    }
}
