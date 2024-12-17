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
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CostCoreProblemFiller extends AbstractCoreProblemFiller {

    public CostCoreProblemFiller(OptimizationPerimeter optimizationContext,
                                 RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                                 RangeActionsOptimizationParameters rangeActionParameters,
                                 Unit unit,
                                 boolean raRangeShrinking,
                                 RangeActionsOptimizationParameters.PstModel pstModel) {
        super(optimizationContext, prePerimeterRangeActionSetpoints, rangeActionParameters, unit, raRangeShrinking, pstModel);
    }

    @Override
    protected void addAllRangeActionVariables(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        super.addAllRangeActionVariables(linearProblem, rangeAction, state);
        if (rangeAction.getActivationCost().isPresent() && rangeAction.getActivationCost().get() > 0) {
            linearProblem.addRangeActionVariationBinary(rangeAction, state);
        }
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
        addIsVariationConstraint(linearProblem, rangeAction, state);
    }

    private void addIsVariationConstraint(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        Optional<Double> activationCost = rangeAction.getActivationCost();
        if (activationCost.isPresent() && activationCost.get() > 0) {
            OpenRaoMPConstraint activationConstraint = linearProblem.addIsVariationConstraint(0, linearProblem.infinity(), rangeAction, state);
            OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
            OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
            OpenRaoMPVariable variationBinaryVariable = linearProblem.getRangeActionVariationBinary(rangeAction, state);

            double minSetPoint;
            double maxSetPoint;

            Pair<RangeAction<?>, State> lastAvailableRangeAction = RaoUtil.getLastAvailableRangeActionOnSameNetworkElement(optimizationContext, rangeAction, state);

            if (lastAvailableRangeAction == null) {
                // if state is equal to masterState,
                // or if rangeAction is not available for a previous state
                // then, rangeAction could not have been activated in a previous instant

                double prePerimeterSetPoint = prePerimeterRangeActionSetpoints.getSetpoint(rangeAction);
                minSetPoint = rangeAction.getMinAdmissibleSetpoint(prePerimeterSetPoint);
                maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(prePerimeterSetPoint);
            } else {
                // range action have been activated in a previous instant
                // getRangeActionSetpointVariable from previous instant
                List<Double> minAndMaxAbsoluteAndRelativeSetpoints = getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction, linearProblem.infinity());
                minSetPoint = minAndMaxAbsoluteAndRelativeSetpoints.get(0);
                maxSetPoint = minAndMaxAbsoluteAndRelativeSetpoints.get(1);
            }

            activationConstraint.setCoefficient(variationBinaryVariable, maxSetPoint - minSetPoint + RANGE_ACTION_SETPOINT_EPSILON);
            activationConstraint.setCoefficient(upwardVariationVariable, -1.0);
            activationConstraint.setCoefficient(downwardVariationVariable, -1.0);
        }
    }

    /**
     * Add in the objective function the costs associated to the range actions that
     * is the sum of two factors:
     * <ul>
     *     <li>An activation cost if the range action is used;</li>
     *     <li>Variation costs that depend on how much the set-point was shifted.</li>
     * </ul>
     */
    @Override
    protected void fillObjective(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
                OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.UPWARD);
                OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

                double defaultVariationCost = getRangeActionPenaltyCost(ra, rangeActionParameters);
                linearProblem.getObjective().setCoefficient(upwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.UP).orElse(defaultVariationCost));
                linearProblem.getObjective().setCoefficient(downwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.DOWN).orElse(defaultVariationCost));

                if (ra.getActivationCost().isPresent() && ra.getActivationCost().get() > 0) {
                    OpenRaoMPVariable activationVariable = linearProblem.getRangeActionVariationBinary(ra, state);
                    linearProblem.getObjective().setCoefficient(activationVariable, ra.getActivationCost().get());
                }
            }
        ));
    }
}