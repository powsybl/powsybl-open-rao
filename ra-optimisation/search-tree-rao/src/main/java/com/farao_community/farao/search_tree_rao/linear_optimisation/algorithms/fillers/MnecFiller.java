/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.parameters.extensions.MnecParametersExtension;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPConstraint;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPVariable;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecFiller implements ProblemFiller {
    private final FlowResult initialFlowResult;
    private final Set<FlowCnec> monitoredCnecs;
    private final Unit unit;
    private final double mnecViolationCost;
    private final double mnecAcceptableMarginDecrease;
    private final double mnecConstraintAdjustmentCoefficient;

    public MnecFiller(FlowResult initialFlowResult, Set<FlowCnec> monitoredCnecs, Unit unit, MnecParametersExtension mnecParameters) {
        this.initialFlowResult = initialFlowResult;
        this.monitoredCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.monitoredCnecs.addAll(monitoredCnecs);
        this.unit = unit;
        this.mnecViolationCost = mnecParameters.getViolationCost();
        this.mnecAcceptableMarginDecrease = mnecParameters.getAcceptableMarginDecrease();
        this.mnecConstraintAdjustmentCoefficient = mnecParameters.getConstraintAdjustmentCoefficient();
    }

    private Set<FlowCnec> getMonitoredCnecs() {
        return monitoredCnecs;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        buildMarginViolationVariable(linearProblem);
        buildMnecMarginConstraints(linearProblem);
        fillObjectiveWithMnecPenaltyCost(linearProblem);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    private void buildMarginViolationVariable(LinearProblem linearProblem) {
        getMonitoredCnecs().forEach(mnec -> mnec.getMonitoredSides().forEach(side ->
            linearProblem.addMnecViolationVariable(0, LinearProblem.infinity(), mnec, side)
        ));
    }

    private void buildMnecMarginConstraints(LinearProblem linearProblem) {
        getMonitoredCnecs().forEach(mnec -> mnec.getMonitoredSides().forEach(side -> {
                double mnecInitialFlowInMW = initialFlowResult.getFlow(mnec, side, unit) * RaoUtil.getFlowUnitMultiplier(mnec, side, unit, MEGAWATT);

                FaraoMPVariable flowVariable = linearProblem.getFlowVariable(mnec, side);
                FaraoMPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(mnec, side);

                Optional<Double> maxFlow = mnec.getUpperBound(side, MEGAWATT);
                if (maxFlow.isPresent()) {
                    double ub = Math.max(maxFlow.get(),  mnecInitialFlowInMW + mnecAcceptableMarginDecrease) - mnecConstraintAdjustmentCoefficient;
                    FaraoMPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(-LinearProblem.infinity(), ub, mnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, -1);
                }

                Optional<Double> minFlow = mnec.getLowerBound(side, MEGAWATT);
                if (minFlow.isPresent()) {
                    double lb = Math.min(minFlow.get(), mnecInitialFlowInMW - mnecAcceptableMarginDecrease) + mnecConstraintAdjustmentCoefficient;
                    FaraoMPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(lb, LinearProblem.infinity(), mnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, 1);
                }
            }
        ));
    }

    public void fillObjectiveWithMnecPenaltyCost(LinearProblem linearProblem) {
        getMonitoredCnecs().stream().filter(FlowCnec::isMonitored).forEach(mnec ->
            mnec.getMonitoredSides().forEach(side ->
            linearProblem.getObjective().setCoefficient(linearProblem.getMnecViolationVariable(mnec, side),
                    RaoUtil.getFlowUnitMultiplier(mnec, side, MEGAWATT, unit) * mnecViolationCost / mnec.getMonitoredSides().size())
            ));
    }
}
