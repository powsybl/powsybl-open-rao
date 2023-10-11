/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPConstraint;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPVariable;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static java.lang.Math.abs;

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
    private final OptimizationPerimeter optimizationContext;
    private final Set<FlowCnec> flowCnecs;
    private final FlowResult prePerimeterFlowResult;
    private final Set<String> operatorsNotToOptimize;
    private final double highestThresholdValue;
    private final Map<FlowCnec, RangeAction<?>> flowCnecRangeActionMap;
    private final RangeActionsOptimizationParameters rangeActionParameters;
    private UnoptimizedCnecFillerRule selectedRule;

    public UnoptimizedCnecFiller(OptimizationPerimeter optimizationContext,
                                 Set<FlowCnec> flowCnecs,
                                 FlowResult prePerimeterFlowResult,
                                 UnoptimizedCnecParameters unoptimizedCnecParameters,
                                 RangeActionsOptimizationParameters rangeActionParameters) {
        this.optimizationContext = optimizationContext;
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(flowCnecs);
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.operatorsNotToOptimize = unoptimizedCnecParameters.getOperatorsNotToOptimize();
        this.highestThresholdValue = RaoUtil.getLargestCnecThreshold(flowCnecs, MEGAWATT);
        this.flowCnecRangeActionMap = unoptimizedCnecParameters.getDoNotOptimizeCnecsSecuredByTheirPst();
        this.rangeActionParameters = rangeActionParameters;
    }

    public enum UnoptimizedCnecFillerRule {
        MARGIN_DECREASE,
        PST_LIMITATION
    }

    private void selectUnoptimizedCnecFillerRule() {
        if (Objects.nonNull(flowCnecRangeActionMap)) {
            selectedRule = UnoptimizedCnecFillerRule.PST_LIMITATION;
        } else {
            selectedRule = UnoptimizedCnecFillerRule.MARGIN_DECREASE;
        }
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        // define UnoptimizedCnecFillerRule
        selectUnoptimizedCnecFillerRule();

        // build variables
        buildDontOptimizeCnecVariables(linearProblem);

        // build constraints
        buildDontOptimizeCnecConstraints(linearProblem, sensitivityResult);

        // update minimum margin objective function constraints
        updateMinimumMarginConstraints(linearProblem);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        updateDontOptimizeCnecConstraints(linearProblem, sensitivityResult);
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
        if (selectedRule.equals(UnoptimizedCnecFillerRule.MARGIN_DECREASE)) {
            return flowCnecs.stream()
                    .filter(cnec -> operatorsNotToOptimize.contains(cnec.getOperator()))
                    .collect(Collectors.toSet());
        } else {
            return optimizationContext.getFlowCnecs().stream()
                    .filter(flowCnecRangeActionMap::containsKey)
                    .collect(Collectors.toSet());
        }
    }

    private void buildDontOptimizeCnecConstraints(LinearProblem linearProblem, SensitivityResult sensitivityResult) {
        if (selectedRule.equals(UnoptimizedCnecFillerRule.MARGIN_DECREASE)) {
            buildDontOptimizeCnecConstraintsForTsosThatDoNotShareRas(linearProblem);
        } else {
            buildDontOptimizeCnecConstraintsForCnecsInSeriesWithPsts(linearProblem, sensitivityResult);
        }
    }

    private void updateDontOptimizeCnecConstraints(LinearProblem linearProblem, SensitivityResult sensitivityResult) {
        if (selectedRule.equals(UnoptimizedCnecFillerRule.PST_LIMITATION)) {
            updateDontOptimizeCnecConstraintsForCnecsInSeriesWithPsts(linearProblem, sensitivityResult);
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
    private void buildDontOptimizeCnecConstraintsForTsosThatDoNotShareRas(LinearProblem linearProblem) {
        double worstMarginDecrease = 20 * highestThresholdValue;
        // No margin should be smaller than the worst margin computed above, otherwise it means the linear optimizer or
        // the search tree rao is degrading the situation
        // So we can use this to estimate the worst decrease possible of the margins on cnecs
        getFlowCnecs().forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            double prePerimeterMargin = prePerimeterFlowResult.getMargin(cnec, side, MEGAWATT);

            FaraoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);
            if (flowVariable == null) {
                throw new FaraoException(String.format(VARIABLE_NOT_CREATED, "Flow", cnec.getId(), side));
            }
            FaraoMPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnec, side);
            if (optimizeCnecBinaryVariable == null) {
                throw new FaraoException(String.format(VARIABLE_NOT_CREATED, OPTIMIZE_CNEC_BINARY, cnec.getId(), side));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(side, MEGAWATT);
            maxFlow = cnec.getUpperBound(side, MEGAWATT);

            if (minFlow.isPresent()) {
                FaraoMPConstraint decreaseMinmumThresholdMargin = linearProblem.addDontOptimizeCnecConstraint(
                        prePerimeterMargin + minFlow.get(),
                        LinearProblem.infinity(), cnec, side,
                        LinearProblem.MarginExtension.BELOW_THRESHOLD
                );
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, 1);
                decreaseMinmumThresholdMargin.setCoefficient(optimizeCnecBinaryVariable, worstMarginDecrease);
            }

            if (maxFlow.isPresent()) {
                FaraoMPConstraint decreaseMinmumThresholdMargin = linearProblem.addDontOptimizeCnecConstraint(
                        prePerimeterMargin - maxFlow.get(),
                        LinearProblem.infinity(), cnec, side,
                        LinearProblem.MarginExtension.ABOVE_THRESHOLD
                );
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, -1);
                decreaseMinmumThresholdMargin.setCoefficient(optimizeCnecBinaryVariable, worstMarginDecrease);
            }
        }));
    }

    private void defineDontOptimizeCnecConstraintsForCnecsInSeriesWithPsts(LinearProblem linearProblem, SensitivityResult sensitivityResult, boolean buildConstraint) {
        getFlowCnecs().forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            // Flow variable
            FaraoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);
            checkVariableCreation(flowVariable, String.format(VARIABLE_NOT_CREATED, "Flow", cnec.getId(), side));

            // Optimize cnec binary variable
            FaraoMPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnec, side);
            checkVariableCreation(optimizeCnecBinaryVariable, String.format(VARIABLE_NOT_CREATED, OPTIMIZE_CNEC_BINARY, cnec.getId(), side));

            State state = getLastStateWithRangeActionAvailableForCnec(cnec);
            if (Objects.isNull(state)) {
                return;
            }
            FaraoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(flowCnecRangeActionMap.get(cnec), state);
            checkVariableCreation(setPointVariable, String.format("Range action variable for PST %s has not been defined yet.", flowCnecRangeActionMap.get(cnec).getId()));

            double maxSetpoint = setPointVariable.ub();
            double minSetpoint = setPointVariable.lb();
            double sensitivity = zeroIfSensitivityBelowThreshold(
                flowCnecRangeActionMap.get(cnec), sensitivityResult.getSensitivityValue(cnec, side, flowCnecRangeActionMap.get(cnec), MEGAWATT));
            Optional<Double> minFlow = cnec.getLowerBound(side, MEGAWATT);
            Optional<Double> maxFlow = cnec.getUpperBound(side, MEGAWATT);

            double bigM = 20 * highestThresholdValue;
            if (minFlow.isPresent()) {
                FaraoMPConstraint extendSetpointBounds;
                if (buildConstraint) {
                    extendSetpointBounds = linearProblem.addDontOptimizeCnecConstraint(
                            -LinearProblem.infinity(),
                            LinearProblem.infinity(), cnec,
                            side,
                            LinearProblem.MarginExtension.BELOW_THRESHOLD);
                    extendSetpointBounds.setCoefficient(flowVariable, 1);
                } else {
                    extendSetpointBounds = linearProblem.getDontOptimizeCnecConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                    if (extendSetpointBounds == null) {
                        throw new FaraoException(String.format("Optimize cnec constraint on cnec %s above threshold has not been defined yet.", cnec.getId()));
                    }
                }
                extendSetpointBounds.setCoefficient(setPointVariable, -sensitivity);
                extendSetpointBounds.setCoefficient(optimizeCnecBinaryVariable, bigM);
                double lb =  minFlow.get();
                if (sensitivity >= 0) {
                    lb += -maxSetpoint * sensitivity;
                } else {
                    lb += -minSetpoint * sensitivity;
                }
                extendSetpointBounds.setLb(lb);
            }
            if (maxFlow.isPresent()) {
                FaraoMPConstraint extendSetpointBounds;
                if (buildConstraint) {
                    extendSetpointBounds = linearProblem.addDontOptimizeCnecConstraint(
                            -LinearProblem.infinity(),
                            LinearProblem.infinity(), cnec, side,
                            LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                    extendSetpointBounds.setCoefficient(flowVariable, -1);
                } else {
                    extendSetpointBounds = linearProblem.getDontOptimizeCnecConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                    if (extendSetpointBounds == null) {
                        throw new FaraoException(String.format("Optimize cnec constraint on cnec %s below threshold has not been defined yet.", cnec.getId()));
                    }
                }
                extendSetpointBounds.setCoefficient(setPointVariable, sensitivity);
                extendSetpointBounds.setCoefficient(optimizeCnecBinaryVariable, bigM);
                double lb =  -maxFlow.get();
                if (sensitivity >= 0) {
                    lb += minSetpoint * abs(sensitivity);
                } else {
                    lb += maxSetpoint * abs(sensitivity);
                }
                extendSetpointBounds.setLb(lb);
            }
        }));
    }

    private void checkVariableCreation(FaraoMPVariable variable, String errorMessage) {
        if (variable == null) {
            throw new FaraoException(errorMessage);
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
    private void buildDontOptimizeCnecConstraintsForCnecsInSeriesWithPsts(LinearProblem linearProblem, SensitivityResult sensitivityResult) {
        defineDontOptimizeCnecConstraintsForCnecsInSeriesWithPsts(linearProblem, sensitivityResult, true);
    }

    private void updateDontOptimizeCnecConstraintsForCnecsInSeriesWithPsts(LinearProblem linearProblem, SensitivityResult sensitivityResult) {
        defineDontOptimizeCnecConstraintsForCnecsInSeriesWithPsts(linearProblem, sensitivityResult, false);
    }

    /**
     * This method finds the most recent state amongst the states in optimizationContext previous to cnec's state
     * for which the pst range action in series with cnec was available.
     **/
    private State getLastStateWithRangeActionAvailableForCnec(FlowCnec cnec) {
        List<State> statesBeforeCnec = FillersUtil.getPreviousStates(cnec.getState(), optimizationContext).stream()
                .sorted((s1, s2) -> Integer.compare(s2.getInstant().getOrder(), s1.getInstant().getOrder())) // start with curative state
                .toList();

        Optional<State> lastState = statesBeforeCnec.stream().filter(state ->
                optimizationContext.getRangeActionsPerState().get(state).contains(flowCnecRangeActionMap.get(cnec)))
                .findFirst();
        // Range action (referenced for "cnec" in flowCnecPstRangeActionMap) is unavailable for cnec
        return lastState.orElse(null);
    }

/**
     * For CNECs with binary variable optimize_cnecs set to 0, deactivate their participation in the definition of the minimum margin
     * Do this by adding (1 - optimize_cnecs) * bigM to the right side of the inequality
     * bigM is computed as 2 times the largest absolute threshold between all CNECs
     * Of course this can be restrictive as CNECs can have hypothetically infinite margins if they are monitored in one direction only
     * But we'll suppose for now that the minimum margin can never be greater than 1 * the largest threshold
     */
    private void updateMinimumMarginConstraints(LinearProblem linearProblem) {
        FaraoMPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }

        double bigM = 2 * highestThresholdValue;
        getFlowCnecs().forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            FaraoMPVariable optimizeCnecBinaryVariable  = linearProblem.getOptimizeCnecBinaryVariable(cnec, side);
            if (optimizeCnecBinaryVariable  == null) {
                throw new FaraoException(String.format(VARIABLE_NOT_CREATED, OPTIMIZE_CNEC_BINARY, cnec.getId(), side));
            }
            updateMinimumMarginConstraint(
                    linearProblem.getMinimumMarginConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
            );
            updateMinimumMarginConstraint(
                    linearProblem.getMinimumMarginConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
            );
            updateMinimumMarginConstraint(
                    linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
            );
            updateMinimumMarginConstraint(
                    linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD),
                    optimizeCnecBinaryVariable,
                    bigM
            );
        }));
    }

    /**
     * Add a big coefficient to the minimum margin definition constraint, allowing it to be relaxed if the
     * binary variable is equal to 1
     */
    private void updateMinimumMarginConstraint(FaraoMPConstraint constraint, FaraoMPVariable optimizeCnecBinaryVariable,
                                               double bigM) {
        if (constraint != null) {
            constraint.setCoefficient(optimizeCnecBinaryVariable, bigM);
            constraint.setUb(constraint.ub() + bigM);
        }
    }

    /**
     * Replace small sensitivity values with zero to avoid numerical issues
     */
    private double zeroIfSensitivityBelowThreshold(RangeAction<?> rangeAction, double sensitivity) {
        double threshold;
        if (rangeAction instanceof PstRangeAction) {
            threshold = rangeActionParameters.getPstSensitivityThreshold();
        } else if (rangeAction instanceof HvdcRangeAction) {
            threshold =  rangeActionParameters.getHvdcSensitivityThreshold();
        } else if (rangeAction instanceof InjectionRangeAction) {
            threshold =  rangeActionParameters.getInjectionRaSensitivityThreshold();
        } else {
            throw new FaraoException("Type of RangeAction not yet handled by the LinearRao.");
        }
        return Math.abs(sensitivity) >= threshold ? sensitivity : 0;
    }
}
