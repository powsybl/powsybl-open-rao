/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CostCoreProblemFiller extends AbstractCoreProblemFiller {

    public CostCoreProblemFiller(OptimizationPerimeter optimizationContext,
                                 RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                                 RangeActionsOptimizationParameters rangeActionParameters,
                                 SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension,
                                 Unit unit,
                                 boolean raRangeShrinking,
                                 SearchTreeRaoRangeActionsOptimizationParameters.PstModel pstModel,
                                 OffsetDateTime timestamp) {
        super(optimizationContext, prePerimeterRangeActionSetpoints, rangeActionParameters, rangeActionParametersExtension, unit, raRangeShrinking, pstModel, timestamp);
        if (SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS.equals(pstModel)) {
            throw new OpenRaoException("Costly remedial action optimization is only available for the APPROXIMATED_INTEGERS mode of PST range actions.");
        }
    }

    @Override
    protected void addAllRangeActionVariables(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        super.addAllRangeActionVariables(linearProblem, rangeAction, state);
        Optional<Double> activationCost = rangeAction.getActivationCost();
        if (activationCost.isPresent() && activationCost.get() > 0) {
            linearProblem.addRangeActionVariationBinary(rangeAction, state, Optional.ofNullable(timestamp));
        }
    }

    @Override
    protected void buildConstraintsForRangeActionAndState(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        addSetPointConstraints(linearProblem, rangeAction, state);
        addIsVariationConstraint(linearProblem, rangeAction, state);
    }

    /**
     * Link the activation binary variable of a RangeAction r to its variation variables.
     * If one of the variation variables is non-null, the total variation is necessarily
     * lower than maxReachableSetPoint[r] - minReachableSetPoint[r] and r is activated.
     * isVariation[r] * (maxReachableSetPoint[r] - minReachableSetPoint[r]) >= upwardVariation[r] + downwardVariation[r]
     */
    private void addIsVariationConstraint(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        Optional<Double> activationCost = rangeAction.getActivationCost();
        if (activationCost.isPresent() && activationCost.get() > 0) {
            OpenRaoMPConstraint activationConstraint = linearProblem.addIsVariationConstraint(0, linearProblem.infinity(), rangeAction, state, Optional.ofNullable(timestamp));
            OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD, Optional.ofNullable(timestamp));
            OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD, Optional.ofNullable(timestamp));
            OpenRaoMPVariable variationBinaryVariable = linearProblem.getRangeActionVariationBinary(rangeAction, state, Optional.ofNullable(timestamp));

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
                OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.UPWARD, Optional.ofNullable(timestamp));
                OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.DOWNWARD, Optional.ofNullable(timestamp));

                double defaultVariationCost = getRangeActionPenaltyCost(ra, rangeActionParameters);
                // pst costs are considered in the discreteTapFiller
                if (!(ra instanceof PstRangeAction)) {
                    linearProblem.getObjective().setCoefficient(upwardVariationVariable, ra.getVariationCost(VariationDirection.UP).orElse(defaultVariationCost));
                    linearProblem.getObjective().setCoefficient(downwardVariationVariable, ra.getVariationCost(VariationDirection.DOWN).orElse(defaultVariationCost));
                }

                if (ra.getActivationCost().isPresent() && ra.getActivationCost().get() > 0) {
                    OpenRaoMPVariable activationVariable = linearProblem.getRangeActionVariationBinary(ra, state, Optional.ofNullable(timestamp));
                    linearProblem.getObjective().setCoefficient(activationVariable, ra.getActivationCost().get());
                }
            }
        ));
    }
}
