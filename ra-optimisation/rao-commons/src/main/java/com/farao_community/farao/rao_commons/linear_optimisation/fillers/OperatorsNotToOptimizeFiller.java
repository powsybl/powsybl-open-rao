/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * This filler adds variables and constraints allowing the RAO to ignore some
 * operators, if they should not be optimized.
 * These operators' CNECs' margins will not be taken into account in the objective function,
 * unless they are worse than their pre-perimeter margins.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OperatorsNotToOptimizeFiller implements ProblemFiller {

    private Set<String> operatorsNotToOptimize;
    private LinearProblem linearProblem;
    private LinearOptimizerInput linearOptimizerInput;
    private double largestCnecThreshold;

    public OperatorsNotToOptimizeFiller(LinearProblem linearProblem, LinearOptimizerInput linearOptimizerInput, LinearOptimizerParameters linearOptimizerParameters) {
        this.linearProblem = linearProblem;
        this.linearOptimizerInput = linearOptimizerInput;
        if (!Objects.isNull(linearOptimizerParameters.getOperatorsNotToOptimize())) {
            this.operatorsNotToOptimize = linearOptimizerParameters.getOperatorsNotToOptimize();
        } else {
            this.operatorsNotToOptimize = new HashSet<>();
        }
        largestCnecThreshold = getLargestCnecThreshold();
    }

    @Override
    public void update(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        // nothing to do
    }

    @Override
    public void fill(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        // build variables
        buildMarginDecreaseVariables();

        // build constraints
        buildMarginDecreaseConstraints();

        // update minimum margin objective function constraints
        updateMinimumMarginConstraints();
    }

    /**
     * This method defines, for each CNEC belonging to a TSO that does not share RAs in the given perimeter, a binary variable
     * The binary variable should detect the decrease of the margin on the given CNEC compared to the preperimeter margin
     * The variable should be equal to 1 if there is a decrease
     */
    private void buildMarginDecreaseVariables() {
        getCnecsForOperatorsNotToOptimize().forEach(linearProblem::addMarginDecreaseBinaryVariable);
    }

    Stream<BranchCnec> getCnecsForOperatorsNotToOptimize() {
        return linearOptimizerInput.getCnecs().stream()
                .filter(BranchCnec::isOptimized)
                .filter(cnec -> operatorsNotToOptimize.contains(cnec.getOperator()));
    }

    /**
     * This method defines, for each CNEC belonging to a TSO that does not share RAs in the given perimeter, a constraint
     * The constraint defines the behaviour of the binary variable "margin decrease"
     * margin >= margin_preperimeter - margin_decrease * bigM
     * => (1) -flow + margin_decrease * bigM >= margin_preperimeter - maxFlow
     * and (2) flow + margin_decrease * bigM >= margin_preperimeter + minFlow
     * bigM is computed to be equal to the maximum margin decrease possible, which is the amount that decreases the cnec's margin to the initial worst margin
     */
    private void buildMarginDecreaseConstraints() {
        if (operatorsNotToOptimize.isEmpty()) {
            return;
        }

        double worstMarginDecrease = 20 * largestCnecThreshold;
        // No margin should be smaller than the worst margin computed above, otherwise it means the linear optimizer or the search tree rao is degrading the situation
        // So we can use this to estimate the worst decrease possible of the margins on cnecs
        getCnecsForOperatorsNotToOptimize().forEach(cnec -> {
            double initialMargin = linearOptimizerInput.getPrePerimeterMarginsInAbsoluteMW(cnec);

            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }
            MPVariable marginDecreaseBinaryVariable = linearProblem.getMarginDecreaseBinaryVariable(cnec);
            if (marginDecreaseBinaryVariable == null) {
                throw new FaraoException(String.format("Margin decrease binary variable has not yet been created for Cnec %s", cnec.getId()));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);

            if (minFlow.isPresent()) {
                MPConstraint decreaseMinmumThresholdMargin = linearProblem.addMarginDecreaseConstraint(initialMargin + minFlow.get(), linearProblem.infinity(), cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, 1);
                decreaseMinmumThresholdMargin.setCoefficient(marginDecreaseBinaryVariable, worstMarginDecrease);
            }

            if (maxFlow.isPresent()) {
                MPConstraint decreaseMinmumThresholdMargin = linearProblem.addMarginDecreaseConstraint(initialMargin - maxFlow.get(), linearProblem.infinity(), cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, -1);
                decreaseMinmumThresholdMargin.setCoefficient(marginDecreaseBinaryVariable, worstMarginDecrease);
            }
        });
    }

    /**
     * For CNECs of operators not sharing RAs, deactivate their participation in the definition of the minimum margin
     * if their margin is not decreased (ie margin_decrease = 0)
     * Do this by adding (1 - margin_decrease) * bigM to the right side of the inequality
     * bigM is computed as 2 times the largest absolute threshold between all CNECs
     * Of course this can be restrictive as CNECs can have hypothetically infinite margins if they are monitored in one direction only
     * But we'll suppose for now that the minimum margin can never be greater than 1 * the largest threshold
     */
    private void updateMinimumMarginConstraints() {
        MPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }

        double bigM = 2 * largestCnecThreshold;
        getCnecsForOperatorsNotToOptimize().forEach(cnec -> {
            MPVariable marginDecreaseBinaryVariable = linearProblem.getMarginDecreaseBinaryVariable(cnec);
            if (marginDecreaseBinaryVariable == null) {
                throw new FaraoException(String.format("Margin decrease binary variable has not yet been created for Cnec %s", cnec.getId()));
            }
            updateMinimumMarginConstraint(linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD), marginDecreaseBinaryVariable, bigM);
            updateMinimumMarginConstraint(linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD), marginDecreaseBinaryVariable, bigM);
            updateMinimumMarginConstraint(linearProblem.getMinimumRelativeMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD), marginDecreaseBinaryVariable, bigM);
            updateMinimumMarginConstraint(linearProblem.getMinimumRelativeMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD), marginDecreaseBinaryVariable, bigM);
        });
    }

    /**
     * Add a big coefficient to the minimum margin definition constraint, allowing it to be relaxed if the
     * binary variable is equal to 1
     */
    private void updateMinimumMarginConstraint(MPConstraint constraint, MPVariable marginDecreaseBinaryVariable, double bigM) {
        if (constraint != null) {
            constraint.setCoefficient(marginDecreaseBinaryVariable, bigM);
            constraint.setUb(constraint.ub() + bigM);
        }
    }

    double getLargestCnecThreshold() {
        double max = 0;
        for (BranchCnec cnec : linearOptimizerInput.getCnecs()) {
            if (cnec.isOptimized()) {
                Optional<Double> minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
                if (minFlow.isPresent() && Math.abs(minFlow.get()) > max) {
                    max = Math.abs(minFlow.get());
                }
                Optional<Double> maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
                if (maxFlow.isPresent() && Math.abs(maxFlow.get()) > max) {
                    max = Math.abs(maxFlow.get());
                }
            }
        }
        return max;
    }
}
