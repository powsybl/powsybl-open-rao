/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.MnecParameters;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Objects;
import java.util.Optional;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecFiller implements ProblemFiller {
    private Unit unit;
    private double mnecAcceptableMarginDiminution;
    private double mnecViolationCost;
    private double mnecConstraintAdjustmentCoefficient;
    private LinearProblem linearProblem;
    private LinearOptimizerInput linearOptimizerInput;

    public MnecFiller(LinearProblem linearProblem, LinearOptimizerInput linearOptimizerInput, LinearOptimizerParameters linearOptimizerParameters) {
        this.linearProblem = linearProblem;
        this.linearOptimizerInput = linearOptimizerInput;
        this.unit = linearOptimizerParameters.getObjectiveFunction().getUnit();
        MnecParameters mnecParameters = linearOptimizerParameters.getMnecParameters();
        this.mnecAcceptableMarginDiminution = mnecParameters.getMnecAcceptableMarginDiminution();
        this.mnecViolationCost = mnecParameters.getMnecViolationCost();
        this.mnecConstraintAdjustmentCoefficient = mnecParameters.getMnecConstraintAdjustmentCoefficient();
    }

    @Override
    public void fill(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        buildMarginViolationVariable();
        buildMnecMarginConstraints();
        fillObjectiveWithMnecPenaltyCost();
    }

    @Override
    public void update(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        // nothing to do
    }

    private void buildMarginViolationVariable() {
        linearOptimizerInput.getCnecs().stream().filter(BranchCnec::isMonitored).forEach(mnec ->
            linearProblem.addMnecViolationVariable(0, linearProblem.infinity(), mnec)
        );
    }

    private void buildMnecMarginConstraints() {
        linearOptimizerInput.getCnecs().stream().filter(BranchCnec::isMonitored).forEach(mnec -> {
                if (Objects.isNull(mnec.getExtension(CnecResultExtension.class))) {
                    return;
                }
                double mnecInitialFlow = linearOptimizerInput.getInitialFlowOnCnec(mnec, MEGAWATT);

                MPVariable flowVariable = linearProblem.getFlowVariable(mnec);

                if (flowVariable == null) {
                    throw new FaraoException(String.format("Flow variable has not yet been created for Mnec %s", mnec.getId()));
                }

                MPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(mnec);

                if (mnecViolationVariable == null) {
                    throw new FaraoException(String.format("Mnec violation variable has not yet been created for Mnec %s", mnec.getId()));
                }

                Optional<Double> maxFlow = mnec.getUpperBound(Side.LEFT, MEGAWATT);
                if (maxFlow.isPresent()) {
                    double ub = Math.max(maxFlow.get(), mnecInitialFlow + mnecAcceptableMarginDiminution) - mnecConstraintAdjustmentCoefficient;
                    MPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(-linearProblem.infinity(), ub, mnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, -1);
                }

                Optional<Double> minFlow = mnec.getLowerBound(Side.LEFT, MEGAWATT);
                if (minFlow.isPresent()) {
                    double lb = Math.min(minFlow.get(), mnecInitialFlow - mnecAcceptableMarginDiminution) + mnecConstraintAdjustmentCoefficient;
                    MPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(lb, linearProblem.infinity(), mnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, 1);
                }
            }
        );
    }

    public void fillObjectiveWithMnecPenaltyCost() {
        linearOptimizerInput.getCnecs().stream().filter(BranchCnec::isMonitored).forEach(mnec ->
            linearProblem.getObjective().setCoefficient(linearProblem.getMnecViolationVariable(mnec),
                    RaoUtil.getBranchFlowUnitMultiplier(mnec, Side.LEFT, MEGAWATT, unit) * mnecViolationCost)
        );
    }
}
