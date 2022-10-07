/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.commons.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static java.lang.String.format;

/**
 * This filler adds variables and constraints allowing the RAO to ignore some
 * cnecs, if they should not be optimized. This can happen in the following cases :
 *  * selectedRule = MARGIN_DECREASE : some operators' CNECs' margins will not be taken into account in the objective function,
 * unless they are worse than their pre-perimeter margins.
 *  * selectedRule = PST_LIMITATION : some cnecs parametrized as in series with as pst will not be taken into account in the objective
 *  function, as long as a there are enough setpoints on the pst left to absorb the margin deficit
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class UnoptimizedCnecFiller implements ProblemFiller {
    public static final String BINARY_VARIABLE_NOT_CREATED = "Optimize cnec binary variable has not yet been created for Cnec %s";
    private final OptimizationPerimeter optimizationContext;
    private final Set<FlowCnec> flowCnecs;
    private final FlowResult prePerimeterFlowResult;
    private final Set<String> operatorsNotToOptimize;
    private final double highestThresholdValue;
    private final Map<FlowCnec, PstRangeAction> flowCnecPstRangeActionMap;
    private UnoptimizedCnecFillerRule selectedRule;

    public UnoptimizedCnecFiller(OptimizationPerimeter optimizationContext,
                                 Set<FlowCnec> flowCnecs,
                                 FlowResult prePerimeterFlowResult,
                                 UnoptimizedCnecParameters unoptimizedCnecParameters) {
        this.optimizationContext = optimizationContext;
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(flowCnecs);
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.operatorsNotToOptimize = unoptimizedCnecParameters.getOperatorsNotToOptimize();
        this.highestThresholdValue = RaoUtil.getLargestCnecThreshold(flowCnecs);
        this.flowCnecPstRangeActionMap = unoptimizedCnecParameters.getUnoptimizedCnecsInSeriesWithPsts();
    }

    public enum UnoptimizedCnecFillerRule {
        MARGIN_DECREASE,
        PST_LIMITATION
    }

    private void selectUnoptimizedCnecFillerRule() {
        if (Objects.nonNull(flowCnecPstRangeActionMap)) {
            selectedRule = UnoptimizedCnecFillerRule.PST_LIMITATION;
        } else {
            selectedRule = UnoptimizedCnecFillerRule.MARGIN_DECREASE;
        }
    }

    public UnoptimizedCnecFillerRule getSelectedRule() {
        return selectedRule;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        // define UnoptimizedCnecFillerRule
        selectUnoptimizedCnecFillerRule();

        // build variables
        buildOptimizeCnecVariables(linearProblem);

        // build constraints
        buildOptimizeCnecConstraints(linearProblem, sensitivityResult);

        // update minimum margin objective function constraints
        updateMinimumMarginConstraints(linearProblem);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        updateOptimizeCnecConstraints(linearProblem, sensitivityResult);
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    /**
     * This method defines a binary variable that detects the decrease of the margin on the given CNEC compared to the preperimeter margin
     * The variable should be equal to 1 if there is a decrease
     */
    private void buildOptimizeCnecVariables(LinearProblem linearProblem) {
        getFlowCnecs().forEach(linearProblem::addOptimizeCnecBinaryVariable);
    }

    /**
     * Gathers flow cnecs that can be unoptimized depending on the ongoing UnoptimizedCnecFillerRule.
     */
    private Set<FlowCnec> getFlowCnecs() {
        if (selectedRule.equals(UnoptimizedCnecFillerRule.MARGIN_DECREASE)) {
            return flowCnecs.stream()
                    .filter(cnec -> operatorsNotToOptimize.contains(cnec.getOperator()))
                    .collect(Collectors.toSet());
        } else {
            return flowCnecPstRangeActionMap.keySet();
        }
    }

    private void buildOptimizeCnecConstraints(LinearProblem linearProblem, SensitivityResult sensitivityResult) {
        if (selectedRule.equals(UnoptimizedCnecFillerRule.MARGIN_DECREASE)) {
            buildOptimizeCnecConstraintsForTsosThatDoNotShareRas(linearProblem);
        } else {
            buildOptimizeCnecConstraintsForCnecsInSeriesWithPsts(linearProblem, sensitivityResult);
        }
    }

    private void updateOptimizeCnecConstraints(LinearProblem linearProblem, SensitivityResult sensitivityResult) {
        if (selectedRule.equals(UnoptimizedCnecFillerRule.PST_LIMITATION)) {
            updateOptimizeCnecConstraintsForCnecsInSeriesWithPsts(linearProblem, sensitivityResult);
        }
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
    private void buildOptimizeCnecConstraintsForTsosThatDoNotShareRas(LinearProblem linearProblem) {
        double worstMarginDecrease = 20 * highestThresholdValue;
        // No margin should be smaller than the worst margin computed above, otherwise it means the linear optimizer or
        // the search tree rao is degrading the situation
        // So we can use this to estimate the worst decrease possible of the margins on cnecs
        getFlowCnecs().forEach(cnec -> {
            double prePerimeterMargin = prePerimeterFlowResult.getMargin(cnec, MEGAWATT);

            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }
            MPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnec);
            if (optimizeCnecBinaryVariable == null) {
                throw new FaraoException(String.format(BINARY_VARIABLE_NOT_CREATED, cnec.getId()));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);

            if (minFlow.isPresent()) {
                MPConstraint decreaseMinmumThresholdMargin = linearProblem.addOptimizeCnecConstraint(
                        prePerimeterMargin + minFlow.get(),
                        LinearProblem.infinity(), cnec,
                        LinearProblem.MarginExtension.BELOW_THRESHOLD
                );
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, 1);
                decreaseMinmumThresholdMargin.setCoefficient(optimizeCnecBinaryVariable, worstMarginDecrease);
            }

            if (maxFlow.isPresent()) {
                MPConstraint decreaseMinmumThresholdMargin = linearProblem.addOptimizeCnecConstraint(
                        prePerimeterMargin - maxFlow.get(),
                        LinearProblem.infinity(), cnec,
                        LinearProblem.MarginExtension.ABOVE_THRESHOLD
                );
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, -1);
                decreaseMinmumThresholdMargin.setCoefficient(optimizeCnecBinaryVariable, worstMarginDecrease);
            }
        });
    }

    /**
     * This method defines, for each CNEC parameterized as in series with a pst in the given perimeter, a constraint
     * The constraint defines the behaviour of the binary variable optimize cnec
     * As long as a there are enough setpoints on the pst left to absorb the margin deficit, the cnec should not
     * participate in the definition of the minimum margin (i.e. the binary variable optimize cnec is 0)
     * When sensitivity >= 0 :
     *      (1) setpoint <= maxSetpoint - -(maxFlow - flow)/sensitivity + bigM * optimize_cnec
     *      (2) setpoint >= minSetpoint + -(flow - minFlow)/sensitivity - bigM * optimize_cnec
     * * When sensitivity < 0 :
     *      (1) setpoint <= maxSetpoint - -(flow - minFlow)/-sensitivity + bigM * optimize_cnec
     *      (2) setpoint >= minSetpoint + -(maxFlow - flow)/-sensitivity - bigM * optimize_cnec
     * bigM is computed to allow the PST to reach its bounds with optimize_cnec set to 1 :
     * bigM = maxSetpoint - minSetpoint
     */
    private void defineOptimizeCnecConstraintsForCnecsInSeriesWithPsts(LinearProblem linearProblem, SensitivityResult sensitivityResult, boolean buildConstraint) {
        getFlowCnecs().forEach(cnec -> {
            // Flow variable
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            checkVariableCreation(flowVariable, "Flow variable has not yet been created for Cnec %s", cnec.getId());

            // Optimize cnec binary variable
            MPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnec);
            checkVariableCreation(optimizeCnecBinaryVariable, BINARY_VARIABLE_NOT_CREATED, cnec.getId());

            State state = getLastStateWithRangeActionAvailableForCnec(cnec);
            MPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(flowCnecPstRangeActionMap.get(cnec), state);
            checkVariableCreation(setPointVariable, "Range action variable for PST %s has not been defined yet.", flowCnecPstRangeActionMap.get(cnec).getId());

            double maxSetpoint = setPointVariable.ub();
            double minSetpoint = setPointVariable.lb();
            double sensitivity = sensitivityResult.getSensitivityValue(cnec, flowCnecPstRangeActionMap.get(cnec), Unit.MEGAWATT);
            Optional<Double> minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            Optional<Double> maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
            double bigM = maxSetpoint - minSetpoint;

            if (minFlow.isPresent()) {
                MPConstraint extendSetpointBounds;
                if (buildConstraint) {
                    extendSetpointBounds = linearProblem.addOptimizeCnecConstraint(
                            -LinearProblem.infinity(),
                            LinearProblem.infinity(), cnec,
                            LinearProblem.MarginExtension.BELOW_THRESHOLD);
                    extendSetpointBounds.setCoefficient(flowVariable, 1);
                } else {
                    extendSetpointBounds = linearProblem.getOptimizeCnecConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                    if (extendSetpointBounds == null) {
                        throw new FaraoException(String.format("Optimize cnec constraint on cnec %s above threshold has not been defined yet.", cnec.getId()));
                    }
                }
                extendSetpointBounds.setCoefficient(setPointVariable, sensitivity);
                if (sensitivity >= 0) {
                    extendSetpointBounds.setLb(minSetpoint * sensitivity + minFlow.get());
                    extendSetpointBounds.setCoefficient(optimizeCnecBinaryVariable, bigM * sensitivity);
                } else {
                    extendSetpointBounds.setLb(maxSetpoint * sensitivity + minFlow.get());
                    extendSetpointBounds.setCoefficient(optimizeCnecBinaryVariable, -bigM * sensitivity);
                }
            }
            if (maxFlow.isPresent()) {
                MPConstraint extendSetpointBounds;
                if (buildConstraint) {
                    extendSetpointBounds = linearProblem.addOptimizeCnecConstraint(
                            -LinearProblem.infinity(),
                            LinearProblem.infinity(), cnec,
                            LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                    extendSetpointBounds.setCoefficient(flowVariable, -1);
                } else {
                    extendSetpointBounds = linearProblem.getOptimizeCnecConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                    if (extendSetpointBounds == null) {
                        throw new FaraoException(String.format("Optimize cnec constraint on cnec %s below threshold has not been defined yet.", cnec.getId()));
                    }
                }
                extendSetpointBounds.setCoefficient(setPointVariable, -sensitivity);
                if (sensitivity >= 0) {
                    extendSetpointBounds.setLb(-maxSetpoint * sensitivity - maxFlow.get());
                    extendSetpointBounds.setCoefficient(optimizeCnecBinaryVariable, bigM * sensitivity);
                } else {
                    extendSetpointBounds.setLb(-minSetpoint * sensitivity - maxFlow.get());
                    extendSetpointBounds.setCoefficient(optimizeCnecBinaryVariable, -bigM * sensitivity);
                }
            }
        });
    }

    private void checkVariableCreation(MPVariable variable, String errorMessage, String id) {
        if (variable == null) {
            throw new FaraoException(String.format(errorMessage, id));
        }
    }

    /**
     * This method defines, for each CNEC parameterized as in series with a pst in the given perimeter, a constraint
     * The constraint defines the behaviour of the binary variable optimize cnec
     * As long as a there are enough setpoints on the pst left to absorb the margin deficit, the cnec should not
     * participate in the definition of the minimum margin (i.e. the binary variable optimize cnec is 0)
     * When sensitivity >= 0 :
     *      (1) setpoint <= maxSetpoint - -(maxFlow - flow)/sensitivity + bigM * optimize_cnec
     *      (2) setpoint >= minSetpoint + -(flow - minFlow)/sensitivity - bigM * optimize_cnec
     * * When sensitivity < 0 :
     *      (1) setpoint <= maxSetpoint - -(flow - minFlow)/-sensitivity + bigM * optimize_cnec
     *      (2) setpoint >= minSetpoint + -(maxFlow - flow)/-sensitivity - bigM * optimize_cnec
     * bigM is computed to allow the PST to reach its bounds with optimize_cnec set to 1 :
     * bigM = maxSetpoint - minSetpoint
     */
    private void buildOptimizeCnecConstraintsForCnecsInSeriesWithPsts(LinearProblem linearProblem, SensitivityResult sensitivityResult) {
        defineOptimizeCnecConstraintsForCnecsInSeriesWithPsts(linearProblem, sensitivityResult, true);
    }

    private void updateOptimizeCnecConstraintsForCnecsInSeriesWithPsts(LinearProblem linearProblem, SensitivityResult sensitivityResult) {
        defineOptimizeCnecConstraintsForCnecsInSeriesWithPsts(linearProblem, sensitivityResult, false);
    }

    private State getLastStateWithRangeActionAvailableForCnec(FlowCnec cnec) {
        List<State> statesBeforeCnec = FillersUtil.getPreviousStates(cnec.getState(), optimizationContext).stream()
                .sorted((s1, s2) -> Integer.compare(s2.getInstant().getOrder(), s1.getInstant().getOrder())) // start with curative state
                .collect(Collectors.toList());

        for (State state : statesBeforeCnec) {
            // Only consider the last instant on which rangeAction is available
            for (RangeAction<?> rangeAction : optimizationContext.getRangeActionsPerState().get(state)) {
                if (!rangeAction.equals(flowCnecPstRangeActionMap.get(cnec))) {
                    continue;
                }
                if (!(rangeAction instanceof PstRangeAction)) {
                    throw new FaraoException(format("Range action %s in series with cnec %s is not a PstRangeAction", rangeAction.getId(), cnec.getId()));
                }
                return state;
            }
        }
        throw new FaraoException(format("Range action %s is not optimized on any state before cnec %s", flowCnecPstRangeActionMap.get(cnec).getId(), cnec.getId()));
    }

/**
     * For CNECs with binary variable optimize_cnecs set to 0, deactivate their participation in the definition of the minimum margin
     * Do this by adding (1 - optimize_cnecs) * bigM to the right side of the inequality
     * bigM is computed as 2 times the largest absolute threshold between all CNECs
     * Of course this can be restrictive as CNECs can have hypothetically infinite margins if they are monitored in one direction only
     * But we'll suppose for now that the minimum margin can never be greater than 1 * the largest threshold
     */
    private void updateMinimumMarginConstraints(LinearProblem linearProblem) {
        MPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }

        double bigM = 2 * highestThresholdValue;
        getFlowCnecs().forEach(cnec -> {
            MPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnec);
            if (optimizeCnecBinaryVariable == null) {
                throw new FaraoException(String.format(BINARY_VARIABLE_NOT_CREATED, cnec.getId()));
            }
            updateMinimumMarginConstraint(
                    linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
            );
            updateMinimumMarginConstraint(
                    linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
            );
            updateMinimumMarginConstraint(
                    linearProblem.getMinimumRelativeMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
            );
            updateMinimumMarginConstraint(
                    linearProblem.getMinimumRelativeMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
            );
        });
    }

    /**
     * Add a big coefficient to the minimum margin definition constraint, allowing it to be relaxed if the
     * binary variable is equal to 1
     */
    private void updateMinimumMarginConstraint(MPConstraint constraint, MPVariable optimizeCnecBinaryVariable,
                                               double bigM) {
        if (constraint != null) {
            constraint.setCoefficient(optimizeCnecBinaryVariable, bigM);
            constraint.setUb(constraint.ub() + bigM);
        }
    }
}
