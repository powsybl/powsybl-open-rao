/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.*;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult extends RangeActionActivationResultImpl {
    private static final double PST_ACTIVATION_THRESHOLD = 1e-6;
    private static final double INJECTION_HVDC_ACTIVATION_THRESHOLD = 1;

    public LinearProblemResult(LinearProblem linearProblem, RangeActionSetpointResult prePerimeterSetPoints, OptimizationPerimeter optimizationContext) {
        super(prePerimeterSetPoints);
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                if (wasRangeActionActivated(linearProblem, rangeAction, state)) {
                    double setPoint = linearProblem.getRangeActionSetpointVariable(rangeAction, state).solutionValue();
                    putResult(rangeAction, state, setPoint);
                }
            })
        );
    }

    private static boolean wasRangeActionActivated(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {

        // For these range actions a variation <= 1 MW is not significant enough to be considered
        if (rangeAction instanceof InjectionRangeAction || rangeAction instanceof HvdcRangeAction || rangeAction instanceof CounterTradeRangeAction) {
            return linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD).solutionValue()
                + linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD).solutionValue() > INJECTION_HVDC_ACTIVATION_THRESHOLD;
        }

        return linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD).solutionValue()
            + linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD).solutionValue() > PST_ACTIVATION_THRESHOLD;
    }
}
