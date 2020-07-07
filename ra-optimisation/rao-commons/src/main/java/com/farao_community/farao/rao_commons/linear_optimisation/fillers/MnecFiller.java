/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Objects;
import java.util.Optional;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecFiller implements ProblemFiller {
    private double mnecAcceptableMarginDiminution;
    private double mnecViolationCost;
    private double mnecConstraintAdjustmentCoefficient;

    public MnecFiller(double mnecAcceptableMarginDiminution, double mnecViolationCost, double mnecConstraintAdjustmentCoefficient) {
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
        this.mnecViolationCost = mnecViolationCost;
        this.mnecConstraintAdjustmentCoefficient = mnecConstraintAdjustmentCoefficient;
    }

    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        buildMarginViolationVariable(raoData, linearProblem);
        buildMnecMarginConstraints(raoData, linearProblem);
        fillObjectiveWithMnecPenaltyCost(raoData, linearProblem);
    }

    private void buildMarginViolationVariable(RaoData raoData, LinearProblem linearProblem) {
        raoData.getCrac().getCnecs().stream().filter(Cnec::isMonitored).forEach(mnec ->
            linearProblem.addMnecViolationVariable(0, linearProblem.infinity(), mnec)
        );
    }

    private void buildMnecMarginConstraints(RaoData raoData, LinearProblem linearProblem) {
        String initialVariantId =  raoData.getCrac().getExtension(ResultVariantManager.class).getPreOptimVariantId();

        raoData.getCrac().getCnecs().stream().filter(Cnec::isMonitored).forEach(mnec -> {
                if (Objects.isNull(mnec.getExtension(CnecResultExtension.class))) {
                    return;
                }
                double flow = mnec.getExtension(CnecResultExtension.class).getVariant(initialVariantId).getFlowInMW();

                MPVariable flowVariable = linearProblem.getFlowVariable(mnec);

                if (flowVariable == null) {
                    throw new FaraoException(String.format("Flow variable has not yet been created for Mnec %s", mnec.getId()));
                }

                MPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(mnec);

                if (mnecViolationVariable == null) {
                    throw new FaraoException(String.format("Mnec violation variable has not yet been created for Mnec %s", mnec.getId()));
                }

                Optional<Double> maxFlow = mnec.getMaxThreshold(MEGAWATT);
                if (maxFlow.isPresent()) {
                    double ub = Math.max(maxFlow.get(), flow + mnecAcceptableMarginDiminution) - mnecConstraintAdjustmentCoefficient;
                    MPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(-linearProblem.infinity(), ub, mnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, -1);
                }

                Optional<Double> minFlow = mnec.getMinThreshold(MEGAWATT);
                if (minFlow.isPresent()) {
                    double lb = Math.min(minFlow.get(), flow - mnecAcceptableMarginDiminution) + mnecConstraintAdjustmentCoefficient;
                    MPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(lb, linearProblem.infinity(), mnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, 1);
                }
            }
        );
    }

    public void fillObjectiveWithMnecPenaltyCost(RaoData raoData, LinearProblem linearProblem) {
        raoData.getCrac().getCnecs().stream().filter(Cnec::isMonitored).forEach(mnec ->
            linearProblem.getObjective().setCoefficient(linearProblem.getMnecViolationVariable(mnec), mnecViolationCost)
        );
    }

    @Override
    public void update(RaoData raoData, LinearProblem linearProblem) {
        // TO DO
    }
}
