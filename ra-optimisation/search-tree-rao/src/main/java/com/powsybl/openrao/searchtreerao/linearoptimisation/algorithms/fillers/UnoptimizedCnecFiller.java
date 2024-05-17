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
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
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
 * cnecs, if they should not be optimized. This can happen in the following cases :
 *  * selectedRule = MARGIN_DECREASE : some operators' CNECs' margins will not be taken into account in the objective function,
 * unless they are worse than their pre-perimeter margins.
 *  * selectedRule = PST_LIMITATION : some cnecs parametrized as in series with a pst will not be taken into account in the objective
 *  function, as long as a there are enough setpoints on the pst left to absorb the margin deficit
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class UnoptimizedCnecFiller implements ProblemFiller {
    public static final String VARIABLE_NOT_CREATED = "%s variable has not yet been created for Cnec %s (side %s)";
    public static final String OPTIMIZE_CNEC_BINARY = "Optimize cnec binary";
    private final Set<FlowCnec> flowCnecs;
    private final FlowResult prePerimeterFlowResult;
    private final Set<String> operatorsNotToOptimize;
    private final double highestThresholdValue;

    public UnoptimizedCnecFiller(OptimizationPerimeter optimizationContext,
                                 Set<FlowCnec> flowCnecs,
                                 FlowResult prePerimeterFlowResult,
                                 UnoptimizedCnecParameters unoptimizedCnecParameters,
                                 RangeActionsOptimizationParameters rangeActionParameters) {
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(flowCnecs);
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.operatorsNotToOptimize = unoptimizedCnecParameters.getOperatorsNotToOptimize();
        this.highestThresholdValue = RaoUtil.getLargestCnecThreshold(flowCnecs, MEGAWATT);
    }

    public enum UnoptimizedCnecFillerRule {
        MARGIN_DECREASE,
        PST_LIMITATION
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {

        // build variables
        buildDontOptimizeCnecVariables(linearProblem);

        // build constraints
        buildDontOptimizeCnecConstraints(linearProblem);

        // update minimum margin objective function constraints
        updateMinimumMarginConstraints(linearProblem);
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
    private void buildDontOptimizeCnecVariables(LinearProblem linearProblem) {
        getFlowCnecs().forEach(cnec -> cnec.getMonitoredSides().forEach(side ->
            linearProblem.addOptimizeCnecBinaryVariable(cnec, side)
        ));
    }

    /**
     * Gathers flow cnecs that can be unoptimized depending on the ongoing UnoptimizedCnecFillerRule.
     */
    private Set<FlowCnec> getFlowCnecs() {
        return flowCnecs.stream()
                .filter(cnec -> operatorsNotToOptimize.contains(cnec.getOperator()))
                .collect(Collectors.toSet());
    }

    private void buildDontOptimizeCnecConstraints(LinearProblem linearProblem) {
        buildDontOptimizeCnecConstraintsForTsosThatDoNotShareRas(linearProblem);
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
    private void buildDontOptimizeCnecConstraintsForTsosThatDoNotShareRas(LinearProblem linearProblem) {
        double worstMarginDecrease = 20 * highestThresholdValue;
        // No margin should be smaller than the worst margin computed above, otherwise it means the linear optimizer or
        // the search tree rao is degrading the situation
        // So we can use this to estimate the worst decrease possible of the margins on cnecs
        getFlowCnecs().forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
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
                        LinearProblem.infinity(), cnec, side,
                        LinearProblem.MarginExtension.BELOW_THRESHOLD
                );
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, 1);
                decreaseMinmumThresholdMargin.setCoefficient(optimizeCnecBinaryVariable, worstMarginDecrease);
            }

            if (maxFlow.isPresent()) {
                OpenRaoMPConstraint decreaseMinmumThresholdMargin = linearProblem.addDontOptimizeCnecConstraint(
                        prePerimeterMargin - maxFlow.get(),
                        LinearProblem.infinity(), cnec, side,
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
    private void updateMinimumMarginConstraints(LinearProblem linearProblem) {
        double bigM = 2 * highestThresholdValue;
        getFlowCnecs().forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnec, side);
            try {
                updateMinimumMarginConstraint(
                    linearProblem.getMinimumMarginConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
                );
            } catch (OpenRaoException ignored) {
            }
            try {
                updateMinimumMarginConstraint(
                    linearProblem.getMinimumMarginConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
                );
            } catch (OpenRaoException ignored) {
            }
            try {
                updateMinimumMarginConstraint(
                    linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
                );
            } catch (OpenRaoException ignored) {
            }
            try {
                updateMinimumMarginConstraint(
                    linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
                );
            } catch (OpenRaoException ignored) {
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
