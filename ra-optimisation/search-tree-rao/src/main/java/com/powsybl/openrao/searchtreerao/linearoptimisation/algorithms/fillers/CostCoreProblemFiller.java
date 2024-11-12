/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
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
    protected void buildRangeActionVariables(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                linearProblem.addRangeActionSetpointVariable(-linearProblem.infinity(), linearProblem.infinity(), rangeAction, state);
                // TODO: define only if activation cost is not null
                linearProblem.addRangeActionVariationBinary(rangeAction, state);
                linearProblem.addRangeActionVariationVariable(linearProblem.infinity(), rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
                linearProblem.addRangeActionVariationVariable(linearProblem.infinity(), rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
            })
        );
    }

    /**
     * Links the set-point and the activation status of a range action r to its upward and downward variation variables.
     * S[r] = initialSetPoint[r] + variationUpward[r] - variationDownward[r]
     * (maxSetPoint[r] - minSetPoint[r]) * isRaActivated[r] >= variationUpward[r] + variationDownward[r]
     */
    @Override
    protected void buildSingleRangeActionConstraints(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state);
        OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable activationVariationVariable = linearProblem.getRangeActionVariationBinary(rangeAction, state);

        OpenRaoMPConstraint setPointVariationConstraint = linearProblem.addRangeActionSetPointVariationConstraint(rangeAction, state);
        setPointVariationConstraint.setCoefficient(setPointVariable, 1.0);
        setPointVariationConstraint.setCoefficient(upwardVariationVariable, -1.0);
        setPointVariationConstraint.setCoefficient(downwardVariationVariable, 1.0);

        OpenRaoMPConstraint activationConstraint = linearProblem.addRangeActionActivationConstraint(rangeAction, state);
        activationConstraint.setCoefficient(upwardVariationVariable, -1.0);
        activationConstraint.setCoefficient(downwardVariationVariable, -1.0);

        Pair<RangeAction<?>, State> lastAvailableRangeAction = RaoUtil.getLastAvailableRangeActionOnSameNetworkElement(optimizationContext, rangeAction, state);

        double minSetPoint;
        double maxSetPoint;

        if (lastAvailableRangeAction == null) {
            // if state is equal to masterState,
            // or if rangeAction is not available for a previous state
            // then, rangeAction could not have been activated in a previous instant

            double prePerimeterSetPoint = prePerimeterRangeActionSetpoints.getSetpoint(rangeAction);
            minSetPoint = rangeAction.getMinAdmissibleSetpoint(prePerimeterSetPoint);
            maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(prePerimeterSetPoint);

            setPointVariable.setLb(minSetPoint - RANGE_ACTION_SETPOINT_EPSILON);
            setPointVariable.setUb(maxSetPoint + RANGE_ACTION_SETPOINT_EPSILON);

            setPointVariationConstraint.setLb(prePerimeterSetPoint);
            setPointVariationConstraint.setUb(prePerimeterSetPoint);
        } else {
            // range action have been activated in a previous instant
            // getRangeActionSetpointVariable from previous instant
            OpenRaoMPVariable previousSetpointVariable = linearProblem.getRangeActionSetpointVariable(lastAvailableRangeAction.getLeft(), lastAvailableRangeAction.getValue());
            setPointVariationConstraint.setCoefficient(previousSetpointVariable, -1.0);

            List<Double> minAndMaxAbsoluteAndRelativeSetpoints = getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction, linearProblem.infinity());
            setRelativeAndAbsoluteBounds(rangeAction, state, linearProblem, minAndMaxAbsoluteAndRelativeSetpoints, setPointVariable, previousSetpointVariable);

            minSetPoint = minAndMaxAbsoluteAndRelativeSetpoints.get(0);
            maxSetPoint = minAndMaxAbsoluteAndRelativeSetpoints.get(1);
        }

        activationConstraint.setCoefficient(activationVariationVariable, maxSetPoint - minSetPoint);
    }

    /**
     * Adds in the objective function the activation and variation costs associated to the RangeActions.
     */
    @Override
    protected void fillObjective(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
                OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.UPWARD);
                OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
                OpenRaoMPVariable activationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

                if (ra.getActivationCost().isPresent()) {
                    linearProblem.getObjective().setCoefficient(activationVariable, ra.getActivationCost().get());
                }

                // If the range action has been filtered out, then absoluteVariationVariable is null
                if (ra instanceof PstRangeAction) {
                    linearProblem.getObjective().setCoefficient(upwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.UP).orElse(rangeActionParameters.getPstPenaltyCost()));
                    linearProblem.getObjective().setCoefficient(downwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.DOWN).orElse(rangeActionParameters.getPstPenaltyCost()));
                } else if (ra instanceof HvdcRangeAction) {
                    linearProblem.getObjective().setCoefficient(upwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.UP).orElse(rangeActionParameters.getHvdcPenaltyCost()));
                    linearProblem.getObjective().setCoefficient(downwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.DOWN).orElse(rangeActionParameters.getHvdcPenaltyCost()));
                } else if (ra instanceof InjectionRangeAction) {
                    linearProblem.getObjective().setCoefficient(upwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.UP).orElse(rangeActionParameters.getInjectionRaPenaltyCost()));
                    linearProblem.getObjective().setCoefficient(downwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.DOWN).orElse(rangeActionParameters.getInjectionRaPenaltyCost()));
                } else {
                    // TODO: add penalty for HVDC range actions
                    if (ra.getVariationCost(RangeAction.VariationDirection.UP).isPresent()) {
                        linearProblem.getObjective().setCoefficient(upwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.UP).get());
                    }
                    if (ra.getVariationCost(RangeAction.VariationDirection.DOWN).isPresent()) {
                        linearProblem.getObjective().setCoefficient(downwardVariationVariable, ra.getVariationCost(RangeAction.VariationDirection.DOWN).get());
                    }
                }
            }
        ));
    }
}
