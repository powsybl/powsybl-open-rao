/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MarginCoreProblemFiller extends AbstractCoreProblemFiller {

    public MarginCoreProblemFiller(OptimizationPerimeter optimizationContext,
                                   RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                                   RangeActionsOptimizationParameters rangeActionParameters,
                                   Unit unit,
                                   boolean raRangeShrinking,
                                   RangeActionsOptimizationParameters.PstModel pstModel) {
        super(optimizationContext, prePerimeterRangeActionSetpoints, rangeActionParameters, unit, raRangeShrinking, pstModel);
    }

    /**
     * Build one set point variable S[r] for each RangeAction r
     * This variable describes the setpoint of the given RangeAction r, given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     *     <li>in MEGAWATT for HVDC range actions</li>
     *     <li>in MEGAWATT for Injection range actions</li>
     * </ul>
     * <p>
     * Build one absolute variation variable AV[r] for each RangeAction r
     * This variable describes the absolute difference between the range action setpoint
     * and its initial value. It is given in the same unit as S[r].
     */
    @Override
    protected void buildRangeActionVariables(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                linearProblem.addRangeActionSetpointVariable(-linearProblem.infinity(), linearProblem.infinity(), rangeAction, state);
                linearProblem.addAbsoluteRangeActionVariationVariable(0, linearProblem.infinity(), rangeAction, state);
                linearProblem.addRangeActionVariationVariable(linearProblem.infinity(), rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
                linearProblem.addRangeActionVariationVariable(linearProblem.infinity(), rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
            })
        );
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
        addAbsoluteVariationConstraint(linearProblem, rangeAction, state);
    }

    private void addAbsoluteVariationConstraint(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state);
        OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

        OpenRaoMPConstraint absoluteVariationConstraint = linearProblem.addRangeActionAbsoluteVariationConstraint(rangeAction, state);
        absoluteVariationConstraint.setCoefficient(absoluteVariationVariable, 1.0);
        absoluteVariationConstraint.setCoefficient(upwardVariationVariable, -1.0);
        absoluteVariationConstraint.setCoefficient(downwardVariationVariable, -1.0);
    }

    /**
     * Add in the objective function a penalty cost associated to the RangeAction
     * activations. This penalty cost prioritizes the solutions which change as little
     * as possible the set points of the RangeActions.
     * <p>
     * min( sum{r in RangeAction} penaltyCost[r] - AV[r] )
     */
    @Override
    protected void fillObjective(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
            OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(ra, state);
            if (absoluteVariationVariable != null) {
                linearProblem.getObjective().setCoefficient(absoluteVariationVariable, getRangeActionPenaltyCost(ra, rangeActionParameters));
            }
        }));
    }
}
