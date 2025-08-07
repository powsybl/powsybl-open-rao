/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult extends RangeActionActivationResultImpl {
    private static final double ACTIVATION_THRESHOLD = 1e-6;

    public LinearProblemResult(LinearProblem linearProblem, RangeActionSetpointResult prePerimeterSetPoints, Map<State, Set<RangeAction<?>>> availableRangeActionsPerState) {
        super(prePerimeterSetPoints);
        availableRangeActionsPerState.forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                if (wasRangeActionActivated(linearProblem, rangeAction, state)) {
                    double setPoint = linearProblem.getRangeActionSetpointVariable(rangeAction, state).solutionValue();
                    putResult(rangeAction, state, setPoint);
                }
            })
        );
    }

    private static boolean wasRangeActionActivated(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        return linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD).solutionValue()
            + linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD).solutionValue() > ACTIVATION_THRESHOLD;
    }
}
