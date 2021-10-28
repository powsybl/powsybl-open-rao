/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_api.parameters.LoopFlowParameters;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Objects;
import java.util.Set;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class MaxLoopFlowFiller implements ProblemFiller {
    private final Set<FlowCnec> loopFlowCnecs;
    private final FlowResult initialFlowResult;
    private final RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel;
    private final double loopFlowAcceptableAugmentation;
    private final double loopFlowViolationCost;
    private final double loopFlowConstraintAdjustmentCoefficient;

    public MaxLoopFlowFiller(Set<FlowCnec> loopFlowCnecs, FlowResult initialFlowResult, LoopFlowParameters loopFlowParameters) {
        this.loopFlowCnecs = loopFlowCnecs;
        this.initialFlowResult = initialFlowResult;
        this.loopFlowApproximationLevel = loopFlowParameters.getLoopFlowApproximationLevel();
        this.loopFlowAcceptableAugmentation = loopFlowParameters.getLoopFlowAcceptableAugmentation();
        this.loopFlowViolationCost = loopFlowParameters.getLoopFlowViolationCost();
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowParameters.getLoopFlowConstraintAdjustmentCoefficient();
    }

    private Set<FlowCnec> getLoopFlowCnecs() {
        return loopFlowCnecs;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        buildLoopFlowConstraintsAndUpdateObjectiveFunction(linearProblem, flowResult);
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        updateLoopFlowConstraints(linearProblem, flowResult);
    }

    /**
     * currentLoopflow = flowVariable - PTDF * NetPosition, where flowVariable a MPVariable
     * Constraint for loopflow in optimization is: - MaxLoopFlow <= currentLoopFlow <= MaxLoopFlow,
     * where MaxLoopFlow is calculated as
     * max(Input TSO loopflow limit, Loopflow value computed from initial network)
     * <p>
     * let CommercialFlow = PTDF * NetPosition, then
     * - MaxLoopFlow + CommercialFlow <= flowVariable <= MaxLoopFlow + CommercialFlow
     * <p>
     * Loopflow limit may be tuned by a "Loopflow adjustment coefficient":
     * MaxLoopFlow = Loopflow constraint - Loopflow adjustment coefficient
     * <p>
     * An additional MPVariable "loopflowViolationVariable" may be added when "Loopflow violation cost" is not zero:
     * - (MaxLoopFlow + loopflowViolationVariable) <= currentLoopFlow <= MaxLoopFlow + loopflowViolationVariable
     * equivalent to 2 constraints:
     * - MaxLoopFlow <= currentLoopFlow + loopflowViolationVariable
     * currentLoopFlow - loopflowViolationVariable <= MaxLoopFlow
     * or:
     * - MaxLoopFlow + CommercialFlow <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
     * NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + CommercialFlow
     * and a "virtual cost" is added to objective function as "loopflowViolationVariable * Loopflow violation cost"
     */
    private void buildLoopFlowConstraintsAndUpdateObjectiveFunction(LinearProblem linearProblem, FlowResult flowResult) {

        for (FlowCnec cnec : getLoopFlowCnecs()) {

            // build loopFlow upper bound, with inputThreshold, initial loop-flows, and configuration parameters
            double loopFlowUpperBound = getLoopFlowUpperBound(cnec);
            if (loopFlowUpperBound == Double.POSITIVE_INFINITY) {
                continue;
            }

            // get loop-flow variable
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (Objects.isNull(flowVariable)) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            MPVariable loopflowViolationVariable = linearProblem.addLoopflowViolationVariable(
                    0,
                    LinearProblem.infinity(),
                    cnec
            );

            // build constraint which defines the loopFlow :
            // - MaxLoopFlow + commercialFlow <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
            // NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + commercialFlow

            MPConstraint positiveLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -loopFlowUpperBound + flowResult.getCommercialFlow(cnec, Unit.MEGAWATT),
                    LinearProblem.infinity(),
                    cnec,
                    LinearProblem.BoundExtension.LOWER_BOUND
            );
            positiveLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
            positiveLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, 1);

            MPConstraint negativeLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -LinearProblem.infinity(),
                    loopFlowUpperBound + flowResult.getCommercialFlow(cnec, Unit.MEGAWATT),
                    cnec,
                    LinearProblem.BoundExtension.UPPER_BOUND
            );
            negativeLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
            negativeLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, -1);

            //update objective function with loopflowViolationCost
            linearProblem.getObjective().setCoefficient(loopflowViolationVariable, loopFlowViolationCost);
        }
    }

    /**
     * Update LoopFlow constraints' bounds when commercial flows have changed
     */
    private void updateLoopFlowConstraints(LinearProblem linearProblem, FlowResult flowResult) {

        if (!loopFlowApproximationLevel.shouldUpdatePtdfWithPstChange()) {
            return;
        }

        for (FlowCnec loopFlowCnec : getLoopFlowCnecs()) {

            double loopFlowUpperBound = getLoopFlowUpperBound(loopFlowCnec);
            if (loopFlowUpperBound == Double.POSITIVE_INFINITY) {
                continue;
            }
            double commercialFlow = flowResult.getCommercialFlow(loopFlowCnec, Unit.MEGAWATT);

            MPConstraint positiveLoopflowViolationConstraint = linearProblem.getMaxLoopFlowConstraint(loopFlowCnec, LinearProblem.BoundExtension.LOWER_BOUND);
            if (positiveLoopflowViolationConstraint == null) {
                throw new FaraoException(String.format("Positive LoopFlow violation constraint on %s has not been defined yet.", loopFlowCnec.getId()));
            }
            positiveLoopflowViolationConstraint.setLb(-loopFlowUpperBound + commercialFlow);

            MPConstraint negativeLoopflowViolationConstraint = linearProblem.getMaxLoopFlowConstraint(loopFlowCnec, LinearProblem.BoundExtension.UPPER_BOUND);
            if (negativeLoopflowViolationConstraint == null) {
                throw new FaraoException(String.format("Negative LoopFlow violation constraint on %s has not been defined yet.", loopFlowCnec.getId()));
            }
            negativeLoopflowViolationConstraint.setUb(loopFlowUpperBound + commercialFlow);
        }
    }

    private double getLoopFlowUpperBound(FlowCnec loopFlowCnec) {
        double loopFlowThreshold = loopFlowCnec.getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialFlowResult.getLoopFlow(loopFlowCnec, Unit.MEGAWATT);
        return Math.max(Math.abs(initialLoopFlow),
            Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation) - loopFlowConstraintAdjustmentCoefficient);
    }
}
