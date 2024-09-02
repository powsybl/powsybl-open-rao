/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;

/**
 * This filler adds variables and constraints allowing the RAO to ignore some
 * cnecs, if they should not be optimized. This can happen when some operators'
 * CNECs' margins will not be taken into account in the objective function,
 * unless they are worse than their pre-perimeter margins.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class UnoptimizedCnecFiller implements ProblemFiller {
    private final Set<FlowCnec> flowCnecs;
    private final FlowResult prePerimeterFlowResult;
    private final Set<String> operatorsNotToOptimize;
    private final double highestThresholdValue;

    public UnoptimizedCnecFiller(Set<FlowCnec> flowCnecs,
                                 FlowResult prePerimeterFlowResult,
                                 UnoptimizedCnecParameters unoptimizedCnecParameters) {
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(FillersUtil.getFlowCnecsNotNaNFlow(flowCnecs, prePerimeterFlowResult));
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.operatorsNotToOptimize = unoptimizedCnecParameters.getOperatorsNotToOptimize();
        this.highestThresholdValue = RaoUtil.getLargestCnecThreshold(flowCnecs, MEGAWATT);
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {

        // Get list of valid flow CNECs
        Set<FlowCnec> validFlowCnecs = getValidFlowCnecs(sensitivityResult);

        // build variables
        buildDontOptimizeCnecVariables(linearProblem, validFlowCnecs);

        // build constraints
        buildDontOptimizeCnecConstraints(linearProblem, validFlowCnecs);

        // update minimum margin objective function constraints
        updateMinimumMarginConstraints(linearProblem, validFlowCnecs);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    /**
     * This method defines a binary variable that detects the decrease of the margin on the given CNEC compared to the preperimeter margin
     * The variable should be equal to 1 if there is a decrease
     */
    private void buildDontOptimizeCnecVariables(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side ->
            linearProblem.addOptimizeCnecBinaryVariable(cnec, side)
        ));
    }

    /**
     * Gathers flow cnecs that can be unoptimized depending on the ongoing UnoptimizedCnecFillerRule.
     */
    private Set<FlowCnec> getValidFlowCnecs(SensitivityResult sensitivityResult) {
        return FillersUtil.getFlowCnecsComputationStatusOk(flowCnecs, sensitivityResult).stream()
            .filter(cnec -> operatorsNotToOptimize.contains(cnec.getOperator()))
            .collect(Collectors.toSet());

    }

    private void buildDontOptimizeCnecConstraints(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        buildDontOptimizeCnecConstraintsForTsosThatDoNotShareRas(linearProblem, validFlowCnecs);
    }

    /**
     * This method defines, for each CNEC belonging to a TSO that does not share RAs in the given perimeter, a constraint
     * The constraint defines the behaviour of the binary variable optimize cnec
     * margin >= margin_preperimeter - optimize_cnec * bigM
     * => (1) -flow + optimize_cnec * bigM >= margin_preperimeter - maxFlow
     * and (2) flow + optimize_cnec * bigM >= margin_preperimeter + minFlow
     * bigM is computed to be equal to the maximum margin decrease possible, which is the amount that decreases the
     * cnec's margin to the initial worst margin
     */
    private void buildDontOptimizeCnecConstraintsForTsosThatDoNotShareRas(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        double worstMarginDecrease = 20 * highestThresholdValue;
        // No margin should be smaller than the worst margin computed above, otherwise it means the linear optimizer or
        // the search tree rao is degrading the situation
        // So we can use this to estimate the worst decrease possible of the margins on cnecs
        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            double prePerimeterMargin = prePerimeterFlowResult.getMargin(cnec, side, MEGAWATT);

            OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);
            OpenRaoMPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnec, side);

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(side, MEGAWATT);
            maxFlow = cnec.getUpperBound(side, MEGAWATT);

            if (minFlow.isPresent()) {
                OpenRaoMPConstraint decreaseMinmumThresholdMargin = linearProblem.addDontOptimizeCnecConstraint(
                    prePerimeterMargin + minFlow.get(),
                    linearProblem.infinity(), cnec, side,
                    LinearProblem.MarginExtension.BELOW_THRESHOLD
                );
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, 1);
                decreaseMinmumThresholdMargin.setCoefficient(optimizeCnecBinaryVariable, worstMarginDecrease);
            }

            if (maxFlow.isPresent()) {
                OpenRaoMPConstraint decreaseMinmumThresholdMargin = linearProblem.addDontOptimizeCnecConstraint(
                    prePerimeterMargin - maxFlow.get(),
                    linearProblem.infinity(), cnec, side,
                    LinearProblem.MarginExtension.ABOVE_THRESHOLD
                );
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, -1);
                decreaseMinmumThresholdMargin.setCoefficient(optimizeCnecBinaryVariable, worstMarginDecrease);
            }
        }));
    }

    /**
     * For CNECs with binary variable optimize_cnecs set to 0, deactivate their participation in the definition of the minimum margin
     * Do this by adding (1 - optimize_cnecs) * bigM to the right side of the inequality
     * bigM is computed as 2 times the largest absolute threshold between all CNECs
     * Of course this can be restrictive as CNECs can have hypothetically infinite margins if they are monitored in one direction only
     * But we'll suppose for now that the minimum margin can never be greater than 1 * the largest threshold
     */
    private void updateMinimumMarginConstraints(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        double bigM = 2 * highestThresholdValue;
        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnec, side);
            try {
                updateMinimumMarginConstraint(
                    linearProblem.getMinimumMarginConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
                );
            } catch (OpenRaoException ignored) {
                //exception is ignored
            }
            try {
                updateMinimumMarginConstraint(
                    linearProblem.getMinimumMarginConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
                );
            } catch (OpenRaoException ignored) {
                //exception is ignored
            }
            try {
                updateMinimumMarginConstraint(
                    linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
                );
            } catch (OpenRaoException ignored) {
                //exception is ignored
            }
            try {
                updateMinimumMarginConstraint(
                    linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
                );
            } catch (OpenRaoException ignored) {
                //exception is ignored
            }
        }));
    }

    /**
     * Add a big coefficient to the minimum margin definition constraint, allowing it to be relaxed if the
     * binary variable is equal to 1
     */
    private void updateMinimumMarginConstraint(OpenRaoMPConstraint constraint, OpenRaoMPVariable optimizeCnecBinaryVariable, double bigM) {
        constraint.setCoefficient(optimizeCnecBinaryVariable, bigM);
        constraint.setUb(constraint.ub() + bigM);
    }
}
