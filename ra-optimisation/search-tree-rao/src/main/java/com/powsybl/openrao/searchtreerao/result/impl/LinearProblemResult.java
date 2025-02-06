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

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult extends RangeActionActivationResultImpl {

    public LinearProblemResult(LinearProblem linearProblem, RangeActionSetpointResult prePerimeterSetpoints, OptimizationPerimeter optimizationContext) {
        super(prePerimeterSetpoints);
        // TODO: handle multiple timestamps
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                // TODO: use state's timestamp when this new feature is available
                if (linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state, Optional.empty()).solutionValue() > 1e-6) {
                    double setPoint = linearProblem.getRangeActionSetpointVariable(rangeAction, state, Optional.empty()).solutionValue();
                    putResult(rangeAction, state, setPoint);
                }
            })
        );
    }
}
