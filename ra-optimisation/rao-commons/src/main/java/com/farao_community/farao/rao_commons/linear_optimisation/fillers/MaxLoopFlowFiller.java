/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.LoopFlowParameters;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Objects;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class MaxLoopFlowFiller implements ProblemFiller {
    LinearOptimizerInput linearOptimizerInput;
    private double loopFlowConstraintAdjustmentCoefficient;
    private double loopFlowAcceptableAugmentation;
    private double loopFlowViolationCost;
    private RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel;
    private LinearProblem linearProblem;

    public MaxLoopFlowFiller(LinearProblem linearProblem,
                             LinearOptimizerInput linearOptimizerInput,
                             LinearOptimizerParameters linearOptimizerParameters) {
        this.linearProblem = linearProblem;
        this.linearOptimizerInput = linearOptimizerInput;
        LoopFlowParameters loopFlowParameters = linearOptimizerParameters.getLoopFlowParameters();
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowParameters.getLoopFlowConstraintAdjustmentCoefficient();
        this.loopFlowViolationCost = loopFlowParameters.getLoopFlowViolationCost();
        this.loopFlowApproximationLevel = loopFlowParameters.getLoopFlowApproximationLevel();
        this.loopFlowAcceptableAugmentation = loopFlowParameters.getLoopFlowAcceptableAugmentation();
    }

    @Override
    public void fill(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        buildLoopFlowConstraintsAndUpdateObjectiveFunction(sensitivityAndLoopflowResults);
    }

    @Override
    public void update(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        updateLoopFlowConstraints(sensitivityAndLoopflowResults);
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
    private void buildLoopFlowConstraintsAndUpdateObjectiveFunction(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {

        for (BranchCnec cnec : linearOptimizerInput.getLoopflowCnecs()) {

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
                    linearProblem.infinity(),
                    cnec
            );

            // build constraint which defines the loopFlow :
            // - MaxLoopFlow + commercialFlow <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
            // NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + commercialFlow

            MPConstraint positiveLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -loopFlowUpperBound + sensitivityAndLoopflowResults.getCommercialFlow(cnec),
                    linearProblem.infinity(),
                    cnec,
                    LinearProblem.BoundExtension.LOWER_BOUND
            );
            positiveLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
            positiveLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, 1);

            MPConstraint negativeLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -linearProblem.infinity(),
                    loopFlowUpperBound + sensitivityAndLoopflowResults.getCommercialFlow(cnec),
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
    private void updateLoopFlowConstraints(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {

        if (!loopFlowApproximationLevel.shouldUpdatePtdfWithPstChange()) {
            return;
        }

        for (BranchCnec cnec : linearOptimizerInput.getLoopflowCnecs()) {

            double loopFlowUpperBound = getLoopFlowUpperBound(cnec);
            if (loopFlowUpperBound == Double.POSITIVE_INFINITY) {
                continue;
            }
            double commercialFlow = sensitivityAndLoopflowResults.getCommercialFlow(cnec);

            MPConstraint positiveLoopflowViolationConstraint = linearProblem.getMaxLoopFlowConstraint(cnec, LinearProblem.BoundExtension.LOWER_BOUND);
            if (positiveLoopflowViolationConstraint == null) {
                throw new FaraoException(String.format("Positive LoopFlow violation constraint on %s has not been defined yet.", cnec.getId()));
            }
            positiveLoopflowViolationConstraint.setLb(-loopFlowUpperBound + commercialFlow);

            MPConstraint negativeLoopflowViolationConstraint = linearProblem.getMaxLoopFlowConstraint(cnec, LinearProblem.BoundExtension.UPPER_BOUND);
            if (negativeLoopflowViolationConstraint == null) {
                throw new FaraoException(String.format("Negative LoopFlow violation constraint on %s has not been defined yet.", cnec.getId()));
            }
            negativeLoopflowViolationConstraint.setUb(loopFlowUpperBound + commercialFlow);
        }
    }

    private double getLoopFlowUpperBound(BranchCnec cnec) {
        double loopFlowThreshold = cnec.getExtension(CnecLoopFlowExtension.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        //TODO : move loopflow threshold
        double initialLoopFlow = linearOptimizerInput.getInitialLoopflowInMW(cnec);
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation) - loopFlowConstraintAdjustmentCoefficient);
    }
}
