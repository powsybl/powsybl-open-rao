/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;

import java.time.OffsetDateTime;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MarginCoreProblemFiller extends AbstractCoreProblemFiller {

    public MarginCoreProblemFiller(OptimizationPerimeter optimizationContext,
                                   RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                                   RangeActionsOptimizationParameters rangeActionParameters,
                                   SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension,
                                   Unit unit,
                                   boolean raRangeShrinking,
                                   SearchTreeRaoRangeActionsOptimizationParameters.PstModel pstModel,
                                   OffsetDateTime timestamp) {
        super(optimizationContext, prePerimeterRangeActionSetpoints, rangeActionParameters, rangeActionParametersExtension, unit, raRangeShrinking, pstModel, timestamp);
    }

    /**
     * Build range action constraints for each RangeAction r.
     * These constraints link the set-point variable of the RangeAction with its
     * variation variables, and bounds the set-point in an admissible range.
     * S[r] = initialSetPoint[r] + upwardVariation[r] - downwardVariation[r]
     */
    @Override
    protected void buildConstraintsForRangeActionAndState(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        addSetPointConstraints(linearProblem, rangeAction, state);
    }

    /**
     * Add in the objective function a penalty cost associated to the RangeAction
     * activations. This penalty cost prioritizes the solutions which change as little
     * as possible the set points of the RangeActions.
     * <p>
     * min( sum{r in RangeAction} penaltyCost[r]  - (upwardVariation[r] + downwardVariation[r]) )
     */
    @Override
    protected void fillObjective(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
            OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.UPWARD);
            if (upwardVariationVariable != null) {
                linearProblem.getObjective().setCoefficient(upwardVariationVariable, getRangeActionPenaltyCost(ra, rangeActionParameters));
            }
            OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
            if (downwardVariationVariable != null) {
                linearProblem.getObjective().setCoefficient(downwardVariationVariable, getRangeActionPenaltyCost(ra, rangeActionParameters));
            }
        }));
    }
}
