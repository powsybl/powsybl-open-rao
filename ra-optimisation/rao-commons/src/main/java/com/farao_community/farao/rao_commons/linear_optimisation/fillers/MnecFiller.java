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
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_api.parameters.MnecParameters;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecFiller implements ProblemFiller {
    private final LinearProblem linearProblem;
    private final Map<BranchCnec, Double> initialFlowInMWPerMnec;
    private final Unit unit;
    private final double mnecViolationCost;
    private final double mnecAcceptableMarginDiminution;
    private final double mnecConstraintAdjustmentCoefficient;

    public MnecFiller(LinearProblem linearProblem, Map<BranchCnec, Double> initialFlowInMWPerMnec, Unit unit, MnecParameters mnecParameters) {
        this.linearProblem = linearProblem;
        this.initialFlowInMWPerMnec = initialFlowInMWPerMnec;
        this.unit = unit;
        this.mnecViolationCost = mnecParameters.getMnecViolationCost();
        this.mnecAcceptableMarginDiminution = mnecParameters.getMnecAcceptableMarginDiminution();
        this.mnecConstraintAdjustmentCoefficient = mnecParameters.getMnecConstraintAdjustmentCoefficient();
    }

    final Map<BranchCnec, Double> getInitialFlowInMWPerMnec() {
        return initialFlowInMWPerMnec;
    }

    final Unit getUnit() {
        return unit;
    }

    final double getMnecViolationCost() {
        return mnecViolationCost;
    }

    final double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    final double getMnecConstraintAdjustmentCoefficient() {
        return mnecConstraintAdjustmentCoefficient;
    }

    private Set<BranchCnec> getMnecs() {
        return initialFlowInMWPerMnec.keySet();
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
        getMnecs().forEach(mnec ->
            linearProblem.addMnecViolationVariable(0, linearProblem.infinity(), mnec)
        );
    }

    private void buildMnecMarginConstraints() {
        getMnecs().forEach(mnec -> {
                double mnecInitialFlowInMW = initialFlowInMWPerMnec.get(mnec);

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
                    double ub = Math.max(maxFlow.get(),  mnecInitialFlowInMW + mnecAcceptableMarginDiminution) - mnecConstraintAdjustmentCoefficient;
                    MPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(-linearProblem.infinity(), ub, mnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, -1);
                }

                Optional<Double> minFlow = mnec.getLowerBound(Side.LEFT, MEGAWATT);
                if (minFlow.isPresent()) {
                    double lb = Math.min(minFlow.get(), mnecInitialFlowInMW - mnecAcceptableMarginDiminution) + mnecConstraintAdjustmentCoefficient;
                    MPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(lb, linearProblem.infinity(), mnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, 1);
                }
            }
        );
    }

    public void fillObjectiveWithMnecPenaltyCost() {
        getMnecs().stream().filter(BranchCnec::isMonitored).forEach(mnec ->
            linearProblem.getObjective().setCoefficient(linearProblem.getMnecViolationVariable(mnec),
                    RaoUtil.getBranchFlowUnitMultiplier(mnec, Side.LEFT, MEGAWATT, unit) * mnecViolationCost)
        );
    }
}
