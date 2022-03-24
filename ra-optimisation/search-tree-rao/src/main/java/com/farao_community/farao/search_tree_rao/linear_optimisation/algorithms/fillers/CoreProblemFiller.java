/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range.TapRange;
import com.farao_community.farao.data.crac_api.range_action.*;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CoreProblemFiller implements ProblemFiller {

    private static final double RANGE_ACTION_SETPOINT_EPSILON = 1e-5;

    private final Network network;
    private final OptimizationPerimeter optimizationContext;
    private final Set<FlowCnec> flowCnecs;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpoints;
    private final RangeActionParameters rangeActionParameters;

    public CoreProblemFiller(Network network,
                             OptimizationPerimeter optimizationContext,
                             Set<FlowCnec> flowCnecs,
                             RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                             RangeActionParameters rangeActionParameters) {
        this.network = network;
        this.optimizationContext = optimizationContext;
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(flowCnecs);
        this.prePerimeterRangeActionSetpoints = prePerimeterRangeActionSetpoints;
        this.rangeActionParameters = rangeActionParameters;
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
        updateFlowConstraints(linearProblem, flowResult, sensitivityResult);
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
                linearProblem.addFlowVariable(-LinearProblem.infinity(), LinearProblem.infinity(), cnec)
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
     * Build one absolute variable variable AV[r] for each RangeAction r
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
     * This constraints link the estimated flow on a Cnec with the impact of the RangeActions
     * on this Cnec.
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void buildFlowConstraints(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        flowCnecs.forEach(cnec -> {
            // create constraint
            double referenceFlow = flowResult.getFlow(cnec, Unit.MEGAWATT);
            MPConstraint flowConstraint = linearProblem.addFlowConstraint(referenceFlow, referenceFlow, cnec);

            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (flowVariable == null) {
                throw new FaraoException(format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            flowConstraint.setCoefficient(flowVariable, 1);

            // add sensitivity coefficients
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, cnec);
        });
    }

    /**
     * Update the flow constraints, with the new reference flows and new sensitivities
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void updateFlowConstraints(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        flowCnecs.forEach(cnec -> {
            double referenceFlow = flowResult.getFlow(cnec, Unit.MEGAWATT);
            MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec);
            if (flowConstraint == null) {
                throw new FaraoException(format("Flow constraint on %s has not been defined yet.", cnec.getId()));
            }

            //reset bounds
            flowConstraint.setUb(referenceFlow);
            flowConstraint.setLb(referenceFlow);

            //reset sensitivity coefficients
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, cnec);
        });
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, FlowCnec cnec) {
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec);

        if (flowVariable == null || flowConstraint == null) {
            throw new FaraoException(format("Flow variable and/or constraint on %s has not been defined yet.", cnec.getId()));
        }

        List<State> statesBeforeCnec = getPreviousStates(cnec.getState()).stream()
            .sorted((s1, s2) -> Integer.compare(s2.getInstant().getOrder(), s1.getInstant().getOrder())) // start with curative state
            .collect(Collectors.toList());

        Set<RangeAction<?>> alreadyConsideredAction = new HashSet<>();

        for (State state : statesBeforeCnec) {
            for (RangeAction<?> rangeAction : optimizationContext.getRangeActionsPerState().get(state)) {
                // todo: make that cleaner, it is ugly
                if (!alreadyConsideredAction.contains(rangeAction)) {
                    addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, rangeAction, state, cnec, flowConstraint);
                    alreadyConsideredAction.addAll(getAvailableRangeActionsOnSameAction(rangeAction));
                }

            }
        }
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, RangeAction<?> rangeAction, State state, FlowCnec cnec, MPConstraint flowConstraint) {
        MPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state);
        if (setPointVariable == null) {
            throw new FaraoException(format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
        }

        double sensitivity = sensitivityResult.getSensitivityValue(cnec, rangeAction, Unit.MEGAWATT);

        if (isRangeActionSensitivityAboveThreshold(rangeAction, Math.abs(sensitivity))) {
            double currentSetPoint = rangeAction.getCurrentSetpoint(network);

            //todo: get that info not from network

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
            return sensitivity >= rangeActionParameters.getInjectionSensitivityThreshold();
        } else {
            throw new FaraoException("Type of RangeAction not yet handled by the LinearRao.");
        }
    }

    /**
     * Build two range action constraints for each RangeAction r.
     * These constraints link the set point variable of the RangeAction with its absolute
     * variation variable.
     *
     * AV[r] >= S[r] - initialSetPoint[r]     (NEGATIVE)
     * AV[r] >= initialSetPoint[r] - S[r]     (POSITIVE)
     */
    private void buildRangeActionConstraints(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().entrySet().stream()
            .sorted(Comparator.comparingInt(e -> e.getKey().getInstant().getOrder()))
            .forEach(entry ->
                entry.getValue().forEach(rangeAction -> {

                    MPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, entry.getKey());
                    MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, entry.getKey());
                    MPConstraint varConstraintNegative = linearProblem.addAbsoluteRangeActionVariationConstraint(
                        -LinearProblem.infinity(),
                        LinearProblem.infinity(),
                        rangeAction,
                        LinearProblem.AbsExtension.NEGATIVE
                    );
                    MPConstraint varConstraintPositive = linearProblem.addAbsoluteRangeActionVariationConstraint(
                        -LinearProblem.infinity(),
                        LinearProblem.infinity(),
                        rangeAction,
                        LinearProblem.AbsExtension.POSITIVE);

                    Pair<RangeAction<?>, State> lastAvailableRangeAction = RaoUtil.getLastAvailableRangeActionOnSameAction(optimizationContext, rangeAction, entry.getKey());

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
                        MPVariable previousSetpointVariable = linearProblem.getRangeActionSetpointVariable(lastAvailableRangeAction.getLeft(), lastAvailableRangeAction.getValue());

                        // if relative to previous instant range
                        double minAbsoluteSetpoint = Double.NEGATIVE_INFINITY;
                        double maxAbsoluteSetpoint = Double.POSITIVE_INFINITY;
                        double minRelativeSetpoint = Double.NEGATIVE_INFINITY;
                        double maxRelativeSetpoint = Double.POSITIVE_INFINITY;
                        if (rangeAction instanceof PstRangeAction) {
                            List<TapRange> ranges = ((PstRangeAction) rangeAction).getRanges();
                            int minAbsoluteTap = Integer.MIN_VALUE;
                            int maxAbsoluteTap = Integer.MAX_VALUE;
                            int minRelativeTap = Integer.MIN_VALUE;
                            int maxRelativeTap = Integer.MAX_VALUE;
                            for (TapRange range : ranges) {
                                if (range.getRangeType().equals(RangeType.ABSOLUTE)) {
                                    minAbsoluteTap = Math.max(minAbsoluteTap, range.getMinTap());
                                    maxAbsoluteTap = Math.min(maxAbsoluteTap, range.getMaxTap());
                                } else if (range.getRangeType().equals(RangeType.RELATIVE_TO_INITIAL_NETWORK)) {
                                    minAbsoluteTap = Math.max(minAbsoluteTap, ((PstRangeAction) rangeAction).getInitialTap() + range.getMinTap());
                                    maxAbsoluteTap = Math.min(maxAbsoluteTap, ((PstRangeAction) rangeAction).getInitialTap() + range.getMaxTap());
                                } else if (range.getRangeType().equals(RangeType.RELATIVE_TO_PREVIOUS_INSTANT)) {
                                    minRelativeTap = Math.max(minRelativeTap, range.getMinTap());
                                    maxRelativeTap = Math.min(maxRelativeTap, range.getMaxTap());
                                }
                            }
                            Map<Integer, Double> tapToAngleMap = ((PstRangeAction) rangeAction).getTapToAngleConversionMap();
                            // The taps are not necessarily in order of increasing angle.
                            double setPointMinAbsoluteTap = tapToAngleMap.get(minAbsoluteTap);
                            double setPointMaxAbsoluteTap = tapToAngleMap.get(maxAbsoluteTap);
                            minAbsoluteSetpoint = Math.min(setPointMinAbsoluteTap, setPointMaxAbsoluteTap);
                            maxAbsoluteSetpoint = Math.max(setPointMinAbsoluteTap, setPointMaxAbsoluteTap);
                            // Make sure we stay in the range by multiplying the relative tap by the smallest angle between taps.
                            // (As long as minRelativeTap is negative (or zero) and maxRelativeTap is positive (or zero).)
                            minRelativeSetpoint = minRelativeTap * ((PstRangeAction) rangeAction).getSmallestAngleDiff();
                            maxRelativeSetpoint = maxRelativeTap * ((PstRangeAction) rangeAction).getSmallestAngleDiff();
                        } else if (rangeAction instanceof HvdcRangeAction) {
                            List<StandardRange> ranges = ((HvdcRangeAction) rangeAction).getRanges();
                            for (StandardRange range : ranges) {
                                if (range.getRangeType().equals(RangeType.ABSOLUTE)) {
                                    minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, range.getMin());
                                    maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, range.getMax());
                                } else if (range.getRangeType().equals(RangeType.RELATIVE_TO_INITIAL_NETWORK)) {
                                    minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, ((HvdcRangeAction) rangeAction).getInitialSetpoint() + range.getMin());
                                    maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, ((HvdcRangeAction) rangeAction).getInitialSetpoint() + range.getMax());
                                } else if (range.getRangeType().equals(RangeType.RELATIVE_TO_PREVIOUS_INSTANT)) {
                                    minRelativeSetpoint = Math.max(minRelativeSetpoint, range.getMin());
                                    maxRelativeSetpoint = Math.min(maxRelativeSetpoint, range.getMax());
                                }
                            }
                        } else if (rangeAction instanceof InjectionRangeAction) {
                            List<StandardRange> ranges = ((InjectionRangeAction) rangeAction).getRanges();
                            for (StandardRange range : ranges) {
                                if (range.getRangeType().equals(RangeType.ABSOLUTE)) {
                                    minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, range.getMin());
                                    maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, range.getMax());
                                } else if (range.getRangeType().equals(RangeType.RELATIVE_TO_INITIAL_NETWORK)) {
                                    minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, ((InjectionRangeAction) rangeAction).getInitialSetpoint() + range.getMin());
                                    maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, ((InjectionRangeAction) rangeAction).getInitialSetpoint() + range.getMax());
                                } else if (range.getRangeType().equals(RangeType.RELATIVE_TO_PREVIOUS_INSTANT)) {
                                    minRelativeSetpoint = Math.max(minRelativeSetpoint, range.getMin());
                                    maxRelativeSetpoint = Math.min(maxRelativeSetpoint, range.getMax());
                                }
                            }
                        } else {
                            throw new NotImplementedException("range action type is not supported yet");
                        }

                        // relative range
                        MPConstraint relSetpointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(minRelativeSetpoint, maxRelativeSetpoint, rangeAction);
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
            )
        );
    }

    private Set<State> getPreviousStates(State refState) {
        return optimizationContext.getRangeActionsPerState().keySet().stream()
            .filter(s -> s.getContingency().equals(refState.getContingency()) || s.getContingency().isEmpty())
            .filter(s -> s.getInstant().comesBefore(refState.getInstant()))
            .collect(Collectors.toSet());
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
     * activations. This penalty cost prioritizes the solutions which change as less
     * as possible the set points of the RangeActions.
     * <p>
     * min( sum{r in RangeAction} penaltyCost[r] - AV[r] )
     */
    private void fillObjectiveWithRangeActionPenaltyCost(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
                MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(ra, state);

                // If the range action has been filtered out, then absoluteVariationVariable is null
                if (absoluteVariationVariable != null && ra instanceof PstRangeAction) {
                    linearProblem.getObjective().setCoefficient(absoluteVariationVariable, rangeActionParameters.getPstPenaltyCost());
                } else if (absoluteVariationVariable != null && ra instanceof HvdcRangeAction) {
                    linearProblem.getObjective().setCoefficient(absoluteVariationVariable, rangeActionParameters.getHvdcPenaltyCost());
                } else if (absoluteVariationVariable != null && ra instanceof InjectionRangeAction) {
                    linearProblem.getObjective().setCoefficient(absoluteVariationVariable, rangeActionParameters.getInjectionPenaltyCost());
                }
            }
        ));
    }
}
