/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Identifiable;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.range.RangeType;
import com.powsybl.open_rao.data.crac_api.range.StandardRange;
import com.powsybl.open_rao.data.crac_api.range.TapRange;
import com.powsybl.open_rao.data.crac_api.range_action.*;
import com.powsybl.open_rao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.powsybl.open_rao.search_tree_rao.commons.RaoUtil;
import com.powsybl.open_rao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.OpenRaoMPConstraint;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.OpenRaoMPVariable;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.powsybl.open_rao.search_tree_rao.result.api.FlowResult;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionActivationResult;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.open_rao.search_tree_rao.result.api.SensitivityResult;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CoreProblemFiller implements ProblemFiller {

    private static final double RANGE_ACTION_SETPOINT_EPSILON = 1e-5;

    private final OptimizationPerimeter optimizationContext;
    private final Set<FlowCnec> flowCnecs;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpoints;
    private final RangeActionActivationResult raActivationFromParentLeaf;
    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final Unit unit;
    private int iteration = 0;
    private static final double RANGE_SHRINK_RATE = 0.667;
    private final boolean raRangeShrinking;

    public CoreProblemFiller(OptimizationPerimeter optimizationContext,
                             RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                             RangeActionActivationResult raActivationFromParentLeaf,
                             RangeActionsOptimizationParameters rangeActionParameters,
                             Unit unit,
                             boolean raRangeShrinking) {
        this.optimizationContext = optimizationContext;
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(optimizationContext.getFlowCnecs());
        this.prePerimeterRangeActionSetpoints = prePerimeterRangeActionSetpoints;
        this.raActivationFromParentLeaf = raActivationFromParentLeaf;
        this.rangeActionParameters = rangeActionParameters;
        this.unit = unit;
        this.raRangeShrinking = raRangeShrinking;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        // add variables
        buildFlowVariables(linearProblem);
        buildRangeActionVariables(linearProblem);

        // add constraints
        buildFlowConstraints(linearProblem, flowResult, sensitivityResult);
        buildRangeActionConstraints(linearProblem);

        // complete objective
        fillObjectiveWithRangeActionPenaltyCost(linearProblem);

    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // update reference flow and sensitivities of flow constraints
        updateFlowConstraints(linearProblem, flowResult, sensitivityResult, rangeActionActivationResult);
        if (raRangeShrinking) {
            updateRangeActionConstraints(linearProblem, rangeActionActivationResult);
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    /**
     * Build one flow variable F[c] for each Cnec c
     * This variable describes the estimated flow on the given Cnec c, in MEGAWATT
     */
    private void buildFlowVariables(LinearProblem linearProblem) {
        flowCnecs.forEach(cnec ->
                cnec.getMonitoredSides().forEach(side -> linearProblem.addFlowVariable(-LinearProblem.infinity(), LinearProblem.infinity(), cnec, side))
        );
    }

    /**
     * Build one set point variable S[r] for each RangeAction r
     * This variable describes the setpoint of the given RangeAction r, given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     *     <li>in MEGAWATT for HVDC range actions</li>
     *     <li>in MEGAWATT for Injection range actions</li>
     * </ul>
     *
     * Build one absolute variation variable AV[r] for each RangeAction r
     * This variable describes the absolute difference between the range action setpoint
     * and its initial value. It is given in the same unit as S[r].
     *
     */
    private void buildRangeActionVariables(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> {
                linearProblem.addRangeActionSetpointVariable(-LinearProblem.infinity(), LinearProblem.infinity(), rangeAction, state);
                linearProblem.addAbsoluteRangeActionVariationVariable(0, LinearProblem.infinity(), rangeAction, state);
            })
        );
    }

    /**
     * Build one flow constraint for each Cnec c.
     * These constraints link the estimated flow on a Cnec with the impact of the RangeActions
     * on this Cnec.
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void buildFlowConstraints(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        flowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            // create constraint
            double referenceFlow = flowResult.getFlow(cnec, side, unit) * RaoUtil.getFlowUnitMultiplier(cnec, side, unit, Unit.MEGAWATT);
            OpenRaoMPConstraint flowConstraint = linearProblem.addFlowConstraint(referenceFlow, referenceFlow, cnec, side);

            OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);
            flowConstraint.setCoefficient(flowVariable, 1);

            // add sensitivity coefficients
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, cnec, side, raActivationFromParentLeaf);
        }));
    }

    /**
     * Update the flow constraints, with the new reference flows and new sensitivities
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void updateFlowConstraints(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        flowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            double referenceFlow = flowResult.getFlow(cnec, side, unit) * RaoUtil.getFlowUnitMultiplier(cnec, side, unit, Unit.MEGAWATT);
            OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec, side);

            //reset bounds
            flowConstraint.setUb(referenceFlow);
            flowConstraint.setLb(referenceFlow);

            //reset sensitivity coefficients
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, cnec, side, rangeActionActivationResult);
        }));
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, FlowCnec cnec, Side side, RangeActionActivationResult rangeActionActivationResult) {
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec, side);

        List<State> statesBeforeCnec = FillersUtil.getPreviousStates(cnec.getState(), optimizationContext).stream()
            .sorted((s1, s2) -> Integer.compare(s2.getInstant().getOrder(), s1.getInstant().getOrder())) // start with curative state
            .toList();

        Set<RangeAction<?>> alreadyConsideredAction = new HashSet<>();

        for (State state : statesBeforeCnec) {
            // Impact of range action on cnec is only added on the last instant on which rangeAction is available
            for (RangeAction<?> rangeAction : optimizationContext.getRangeActionsPerState().get(state)) {
                // todo: make that cleaner, it is ugly
                if (!alreadyConsideredAction.contains(rangeAction)) {
                    addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, rangeAction, state, cnec, side, flowConstraint, rangeActionActivationResult);
                    alreadyConsideredAction.addAll(getAvailableRangeActionsOnSameAction(rangeAction));
                }

            }
        }
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, RangeAction<?> rangeAction, State state, FlowCnec cnec, Side side, OpenRaoMPConstraint flowConstraint, RangeActionActivationResult rangeActionActivationResult) {
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state);

        double sensitivity = sensitivityResult.getSensitivityValue(cnec, side, rangeAction, Unit.MEGAWATT);

        if (isRangeActionSensitivityAboveThreshold(rangeAction, Math.abs(sensitivity))) {
            //TODO: compute this only once somehow
            double currentSetPoint = rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);

            // care : might not be robust as getCurrentValue get the current setPoint from a network variant
            //        we need to be sure that this variant has been properly set
            flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentSetPoint);
            flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentSetPoint);

            flowConstraint.setCoefficient(setPointVariable, -sensitivity);
        } else {
            // We need to do this in case of an update
            flowConstraint.setCoefficient(setPointVariable, 0);
        }
    }

    private boolean isRangeActionSensitivityAboveThreshold(RangeAction<?> rangeAction, double sensitivity) {
        if (rangeAction instanceof PstRangeAction) {
            return sensitivity >= rangeActionParameters.getPstSensitivityThreshold();
        } else if (rangeAction instanceof HvdcRangeAction) {
            return sensitivity >= rangeActionParameters.getHvdcSensitivityThreshold();
        } else if (rangeAction instanceof InjectionRangeAction) {
            return sensitivity >= rangeActionParameters.getInjectionRaSensitivityThreshold();
        } else {
            throw new OpenRaoException("Type of RangeAction not yet handled by the LinearRao.");
        }
    }

    private void buildRangeActionConstraints(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().entrySet().stream()
            .sorted(Comparator.comparingInt(e -> e.getKey().getInstant().getOrder()))
            .forEach(entry ->
                entry.getValue().forEach(rangeAction -> buildConstraintsForRangeActionAndState(linearProblem, rangeAction, entry.getKey())
            )
        );
    }

    private void updateRangeActionConstraints(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        iteration++;
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction -> updateConstraintsForRangeAction(linearProblem, rangeAction, state, rangeActionActivationResult, iteration)
        ));
    }

    private void updateConstraintsForRangeAction(LinearProblem linearProblem, RangeAction<?> rangeAction, State state, RangeActionActivationResult rangeActionActivationResult, int iteration) {
        double previousSetPointValue = rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);
        List<Double> minAndMaxAbsoluteAndRelativeSetpoints = getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction);
        double minAbsoluteSetpoint = Math.max(minAndMaxAbsoluteAndRelativeSetpoints.get(0), -LinearProblem.infinity());
        double maxAbsoluteSetpoint = Math.min(minAndMaxAbsoluteAndRelativeSetpoints.get(1), LinearProblem.infinity());
        double constrainedSetPointRange = (maxAbsoluteSetpoint - minAbsoluteSetpoint) * Math.pow(RANGE_SHRINK_RATE, iteration);
        double lb = previousSetPointValue - constrainedSetPointRange;
        double ub = previousSetPointValue + constrainedSetPointRange;
        try {
            OpenRaoMPConstraint iterativeShrink = linearProblem.getRangeActionRelativeSetpointConstraint(rangeAction, state, LinearProblem.RaRangeShrinking.TRUE);
            iterativeShrink.setLb(lb);
            iterativeShrink.setUb(ub);
        } catch (OpenRaoException ignored) {
            // Constraint iterativeShrink has not yet been created
            OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state);
            OpenRaoMPConstraint iterativeShrink = linearProblem.addRangeActionRelativeSetpointConstraint(lb, ub, rangeAction, state, LinearProblem.RaRangeShrinking.TRUE);
            iterativeShrink.setCoefficient(setPointVariable, 1);
        }
    }

    /**
     * Build two range action constraints for each RangeAction r.
     * These constraints link the set point variable of the RangeAction with its absolute
     * variation variable.
     * AV[r] >= S[r] - initialSetPoint[r]     (NEGATIVE)
     * AV[r] >= initialSetPoint[r] - S[r]     (POSITIVE)
     */
    private void buildConstraintsForRangeActionAndState(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state);
        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state);
        OpenRaoMPConstraint varConstraintNegative = linearProblem.addAbsoluteRangeActionVariationConstraint(
            -LinearProblem.infinity(),
            LinearProblem.infinity(),
            rangeAction,
            state,
            LinearProblem.AbsExtension.NEGATIVE
        );
        OpenRaoMPConstraint varConstraintPositive = linearProblem.addAbsoluteRangeActionVariationConstraint(
            -LinearProblem.infinity(),
            LinearProblem.infinity(),
            rangeAction,
            state,
            LinearProblem.AbsExtension.POSITIVE);

        Pair<RangeAction<?>, State> lastAvailableRangeAction = RaoUtil.getLastAvailableRangeActionOnSameNetworkElement(optimizationContext, rangeAction, state);

        if (lastAvailableRangeAction == null) {
            // if state is equal to masterState,
            // or if rangeAction is not available for a previous state
            // then, rangeAction could not have been activated in a previous instant

            double prePerimeterSetPoint = prePerimeterRangeActionSetpoints.getSetpoint(rangeAction);
            double minSetPoint = rangeAction.getMinAdmissibleSetpoint(prePerimeterSetPoint);
            double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(prePerimeterSetPoint);

            setPointVariable.setLb(minSetPoint - RANGE_ACTION_SETPOINT_EPSILON);
            setPointVariable.setUb(maxSetPoint + RANGE_ACTION_SETPOINT_EPSILON);

            varConstraintNegative.setLb(-prePerimeterSetPoint);
            varConstraintNegative.setCoefficient(absoluteVariationVariable, 1);
            varConstraintNegative.setCoefficient(setPointVariable, -1);

            varConstraintPositive.setLb(prePerimeterSetPoint);
            varConstraintPositive.setCoefficient(absoluteVariationVariable, 1);
            varConstraintPositive.setCoefficient(setPointVariable, 1);
        } else {

            // range action have been activated in a previous instant
            // getRangeActionSetpointVariable from previous instant
            OpenRaoMPVariable previousSetpointVariable = linearProblem.getRangeActionSetpointVariable(lastAvailableRangeAction.getLeft(), lastAvailableRangeAction.getValue());

            List<Double> minAndMaxAbsoluteAndRelativeSetpoints = getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction);
            double minAbsoluteSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(0);
            double maxAbsoluteSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(1);
            double minRelativeSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(2);
            double maxRelativeSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(3);

            // relative range
            minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, -LinearProblem.infinity());
            maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, LinearProblem.infinity());
            minRelativeSetpoint = Math.max(minRelativeSetpoint, -LinearProblem.infinity());
            maxRelativeSetpoint = Math.min(maxRelativeSetpoint, LinearProblem.infinity());
            OpenRaoMPConstraint relSetpointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(minRelativeSetpoint, maxRelativeSetpoint, rangeAction, state, LinearProblem.RaRangeShrinking.FALSE);
            relSetpointConstraint.setCoefficient(setPointVariable, 1);
            relSetpointConstraint.setCoefficient(previousSetpointVariable, -1);

            // absolute range
            setPointVariable.setLb(minAbsoluteSetpoint - RANGE_ACTION_SETPOINT_EPSILON);
            setPointVariable.setUb(maxAbsoluteSetpoint + RANGE_ACTION_SETPOINT_EPSILON);

            // define absolute range action variation
            varConstraintNegative.setLb(0);
            varConstraintNegative.setCoefficient(absoluteVariationVariable, 1);
            varConstraintNegative.setCoefficient(setPointVariable, -1);
            varConstraintNegative.setCoefficient(previousSetpointVariable, 1);

            varConstraintPositive.setLb(0);
            varConstraintPositive.setCoefficient(absoluteVariationVariable, 1);
            varConstraintPositive.setCoefficient(setPointVariable, 1);
            varConstraintPositive.setCoefficient(previousSetpointVariable, -1);
        }
    }

    private List<Double> getMinAndMaxAbsoluteAndRelativeSetpoints(RangeAction<?> rangeAction) {

        // if relative to previous instant range
        double minAbsoluteSetpoint = Double.NEGATIVE_INFINITY;
        double maxAbsoluteSetpoint = Double.POSITIVE_INFINITY;
        double minRelativeSetpoint = Double.NEGATIVE_INFINITY;
        double maxRelativeSetpoint = Double.POSITIVE_INFINITY;
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            Map<Integer, Double> tapToAngleMap = pstRangeAction.getTapToAngleConversionMap();
            List<TapRange> ranges = pstRangeAction.getRanges();

            int minAbsoluteTap = tapToAngleMap.keySet().stream().mapToInt(k -> k).min().orElseThrow();
            int maxAbsoluteTap = tapToAngleMap.keySet().stream().mapToInt(k -> k).max().orElseThrow();
            int minRelativeTap = tapToAngleMap.keySet().stream().mapToInt(k -> k).min().orElseThrow();
            int maxRelativeTap = tapToAngleMap.keySet().stream().mapToInt(k -> k).max().orElseThrow();
            for (TapRange range : ranges) {
                RangeType rangeType = range.getRangeType();
                switch (rangeType) {
                    case ABSOLUTE:
                        minAbsoluteTap = Math.max(minAbsoluteTap, range.getMinTap());
                        maxAbsoluteTap = Math.min(maxAbsoluteTap, range.getMaxTap());
                        break;
                    case RELATIVE_TO_INITIAL_NETWORK:
                        minAbsoluteTap = Math.max(minAbsoluteTap, pstRangeAction.getInitialTap() + range.getMinTap());
                        maxAbsoluteTap = Math.min(maxAbsoluteTap, pstRangeAction.getInitialTap() + range.getMaxTap());
                        break;
                    case RELATIVE_TO_PREVIOUS_INSTANT:
                        minRelativeTap = Math.max(minRelativeTap, range.getMinTap());
                        maxRelativeTap = Math.min(maxRelativeTap, range.getMaxTap());
                        break;
                }
            }
            // The taps are not necessarily in order of increasing angle.
            double setPointMinAbsoluteTap = tapToAngleMap.get(minAbsoluteTap);
            double setPointMaxAbsoluteTap = tapToAngleMap.get(maxAbsoluteTap);
            minAbsoluteSetpoint = Math.min(setPointMinAbsoluteTap, setPointMaxAbsoluteTap);
            maxAbsoluteSetpoint = Math.max(setPointMinAbsoluteTap, setPointMaxAbsoluteTap);
            // Make sure we stay in the range by multiplying the relative tap by the smallest angle between taps.
            // (As long as minRelativeTap is negative (or zero) and maxRelativeTap is positive (or zero).)
            minRelativeSetpoint = minRelativeTap * pstRangeAction.getSmallestAngleStep();
            maxRelativeSetpoint = maxRelativeTap * pstRangeAction.getSmallestAngleStep();
        } else if (rangeAction instanceof StandardRangeAction<?> standardRangeAction) {
            List<StandardRange> ranges = standardRangeAction.getRanges();
            for (StandardRange range : ranges) {
                RangeType rangeType = range.getRangeType();
                switch (rangeType) {
                    case ABSOLUTE:
                        minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, range.getMin());
                        maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, range.getMax());
                        break;
                    case RELATIVE_TO_INITIAL_NETWORK:
                        minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, standardRangeAction.getInitialSetpoint() + range.getMin());
                        maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, standardRangeAction.getInitialSetpoint() + range.getMax());
                        break;
                    case RELATIVE_TO_PREVIOUS_INSTANT:
                        minRelativeSetpoint = Math.max(minRelativeSetpoint, range.getMin());
                        maxRelativeSetpoint = Math.min(maxRelativeSetpoint, range.getMax());
                        break;
                }
            }
        } else {
            throw new NotImplementedException("range action type is not supported yet");
        }

        return List.of(minAbsoluteSetpoint, maxAbsoluteSetpoint, minRelativeSetpoint, maxRelativeSetpoint);
    }

    private Set<RangeAction<?>> getAvailableRangeActionsOnSameAction(RangeAction<?> rangeAction) {
        Set<RangeAction<?>> rangeActions = new HashSet<>();
        optimizationContext.getRangeActionsPerState().forEach((state, raSet) ->
            raSet.forEach(ra -> {
                if (ra.getId().equals(rangeAction.getId()) || ra.getNetworkElements().equals(rangeAction.getNetworkElements())) {
                    rangeActions.add(ra);
                }
            })
        );
        return rangeActions;
    }

    /**
     * Add in the objective function a penalty cost associated to the RangeAction
     * activations. This penalty cost prioritizes the solutions which change as little
     * as possible the set points of the RangeActions.
     * <p>
     * min( sum{r in RangeAction} penaltyCost[r] - AV[r] )
     */
    private void fillObjectiveWithRangeActionPenaltyCost(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
                OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(ra, state);

                // If the range action has been filtered out, then absoluteVariationVariable is null
                if (absoluteVariationVariable != null && ra instanceof PstRangeAction) {
                    linearProblem.getObjective().setCoefficient(absoluteVariationVariable, rangeActionParameters.getPstPenaltyCost());
                } else if (absoluteVariationVariable != null && ra instanceof HvdcRangeAction) {
                    linearProblem.getObjective().setCoefficient(absoluteVariationVariable, rangeActionParameters.getHvdcPenaltyCost());
                } else if (absoluteVariationVariable != null && ra instanceof InjectionRangeAction) {
                    linearProblem.getObjective().setCoefficient(absoluteVariationVariable, rangeActionParameters.getInjectionRaPenaltyCost());
                }
            }
        ));
    }
}
