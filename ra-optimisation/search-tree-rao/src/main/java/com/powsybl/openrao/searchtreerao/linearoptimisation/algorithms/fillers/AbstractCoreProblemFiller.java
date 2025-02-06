/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.StandardRangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.time.OffsetDateTime;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractCoreProblemFiller implements ProblemFiller {
    protected static final double RANGE_ACTION_SETPOINT_EPSILON = 1e-5;
    protected final OptimizationPerimeter optimizationContext;
    protected final RangeActionSetpointResult prePerimeterRangeActionSetpoints;
    protected final Set<FlowCnec> flowCnecs;
    protected final RangeActionsOptimizationParameters rangeActionParameters;
    protected final SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;
    protected final Unit unit;
    protected int iteration = 0;
    protected static final double RANGE_SHRINK_RATE = 0.667;
    protected final boolean raRangeShrinking;
    protected final PstModel pstModel;
    protected final OffsetDateTime timestamp;

    protected AbstractCoreProblemFiller(OptimizationPerimeter optimizationContext,
                                        RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                                        RangeActionsOptimizationParameters rangeActionParameters,
                                        SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension,
                                        Unit unit,
                                        boolean raRangeShrinking,
                                        PstModel pstModel,
                                        OffsetDateTime timestamp) {
        this.optimizationContext = optimizationContext;
        this.prePerimeterRangeActionSetpoints = prePerimeterRangeActionSetpoints;
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(optimizationContext.getFlowCnecs());
        this.rangeActionParameters = rangeActionParameters;
        this.rangeActionParametersExtension = rangeActionParametersExtension;
        this.unit = unit;
        this.raRangeShrinking = raRangeShrinking;
        this.pstModel = pstModel;
        this.timestamp = timestamp;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(flowCnecs, sensitivityResult);

        // add variables
        buildFlowVariables(linearProblem, validFlowCnecs);
        buildRangeActionVariables(linearProblem);

        // add constraints
        buildFlowConstraints(linearProblem, validFlowCnecs, flowResult, sensitivityResult, rangeActionActivationResult);
        buildRangeActionConstraints(linearProblem);
        checkAndActivateRangeShrinking(linearProblem, rangeActionActivationResult);

        // complete objective
        fillObjective(linearProblem);

    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    /**
     * Build one flow variable F[c] for each Cnec c
     * This variable describes the estimated flow on the given Cnec c, in MEGAWATT
     */
    private void buildFlowVariables(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        validFlowCnecs.forEach(cnec ->
            cnec.getMonitoredSides().forEach(side -> linearProblem.addFlowVariable(-linearProblem.infinity(), linearProblem.infinity(), cnec, side, Optional.ofNullable(timestamp)))
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
     * <p>
     * Build one absolute variation variable AV[r] for each RangeAction r
     * This variable describes the absolute difference between the range action setpoint
     * and its initial value. It is given in the same unit as S[r].
     */
    private void buildRangeActionVariables(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.forEach(rangeAction -> addAllRangeActionVariables(linearProblem, rangeAction, state))
        );
    }

    protected void addAllRangeActionVariables(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        addBasicRangeActionVariables(linearProblem, rangeAction, state);
    }

    private void addBasicRangeActionVariables(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        linearProblem.addRangeActionSetpointVariable(-linearProblem.infinity(), linearProblem.infinity(), rangeAction, state, Optional.ofNullable(timestamp));
        linearProblem.addAbsoluteRangeActionVariationVariable(0, linearProblem.infinity(), rangeAction, state, Optional.ofNullable(timestamp));
        linearProblem.addRangeActionVariationVariable(linearProblem.infinity(), rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD, Optional.ofNullable(timestamp));
        linearProblem.addRangeActionVariationVariable(linearProblem.infinity(), rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD, Optional.ofNullable(timestamp));
    }

    /**
     * Build one flow constraint for each Cnec c.
     * These constraints link the estimated flow on a Cnec with the impact of the RangeActions
     * on this Cnec.
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void buildFlowConstraints(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            // create constraint
            double referenceFlow = flowResult.getFlow(cnec, side, unit) * RaoUtil.getFlowUnitMultiplier(cnec, side, unit, Unit.MEGAWATT);
            OpenRaoMPConstraint flowConstraint = linearProblem.addFlowConstraint(referenceFlow, referenceFlow, cnec, side, Optional.ofNullable(timestamp));

            OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side, Optional.ofNullable(timestamp));
            flowConstraint.setCoefficient(flowVariable, 1);

            // add sensitivity coefficients
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, cnec, side, rangeActionActivationResult);
        }));
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, FlowCnec cnec, TwoSides side, RangeActionActivationResult rangeActionActivationResult) {
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec, side, Optional.ofNullable(timestamp));

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

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, RangeAction<?> rangeAction, State state, FlowCnec cnec, TwoSides side, OpenRaoMPConstraint flowConstraint, RangeActionActivationResult rangeActionActivationResult) {
        double sensitivity = sensitivityResult.getSensitivityValue(cnec, side, rangeAction, Unit.MEGAWATT);

        if (!isRangeActionSensitivityAboveThreshold(rangeAction, Math.abs(sensitivity))) {
            // don't consider this RA's impact on this CNEC
            return;
        }

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state, Optional.ofNullable(timestamp));
        double currentSetPoint = rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);

        flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentSetPoint);
        flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentSetPoint);

        flowConstraint.setCoefficient(setPointVariable, -sensitivity);
    }

    private boolean isRangeActionSensitivityAboveThreshold(RangeAction<?> rangeAction, double sensitivity) {
        if (rangeAction instanceof PstRangeAction) {
            return sensitivity >= getPstSensitivityThreshold(rangeActionParametersExtension);
        } else if (rangeAction instanceof HvdcRangeAction) {
            return sensitivity >= getHvdcSensitivityThreshold(rangeActionParametersExtension);
        } else if (rangeAction instanceof InjectionRangeAction) {
            return sensitivity >= getInjectionRaSensitivityThreshold(rangeActionParametersExtension);
        } else {
            throw new OpenRaoException("Type of RangeAction not yet handled by the LinearRao.");
        }
    }

    private void buildRangeActionConstraints(LinearProblem linearProblem) {
        optimizationContext.getRangeActionsPerState().entrySet().stream()
            .sorted(Comparator.comparingInt(e -> e.getKey().getInstant().getOrder()))
            .forEach(entry -> {
                addGlobalInjectionBalanceConstraint(linearProblem, entry.getKey());
                entry.getValue().forEach(rangeAction -> buildConstraintsForRangeActionAndState(linearProblem, rangeAction, entry.getKey()));
            });
    }

    private void addGlobalInjectionBalanceConstraint(LinearProblem linearProblem, State state) {
        if (optimizationContext.getRangeActionsPerState().get(state).stream().anyMatch(InjectionRangeAction.class::isInstance)) {
            linearProblem.addInjectionBalanceConstraint(state, Optional.ofNullable(timestamp));
        }
    }

    private void checkAndActivateRangeShrinking(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        if (!raRangeShrinking) {
            return;
        }
        if (iteration > 0) {
            // don't shrink the range for the first iteration
            optimizationContext.getRangeActionsPerState().forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction -> updateConstraintsForRangeAction(linearProblem, rangeAction, state, rangeActionActivationResult, iteration, timestamp)));
        }
        iteration++;
    }

    private static void updateConstraintsForRangeAction(LinearProblem linearProblem, RangeAction<?> rangeAction, State state, RangeActionActivationResult rangeActionActivationResult, int iteration, OffsetDateTime timestamp) {
        double previousSetPointValue = rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);
        List<Double> minAndMaxAbsoluteAndRelativeSetpoints = getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction, linearProblem.infinity());
        double minAbsoluteSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(0);
        double maxAbsoluteSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(1);
        double constrainedSetPointRange = (maxAbsoluteSetpoint - minAbsoluteSetpoint) * Math.pow(RANGE_SHRINK_RATE, iteration);
        double lb = previousSetPointValue - constrainedSetPointRange;
        double ub = previousSetPointValue + constrainedSetPointRange;
        try {
            OpenRaoMPConstraint iterativeShrink = linearProblem.getRangeActionRelativeSetpointConstraint(rangeAction, state, LinearProblem.RaRangeShrinking.TRUE, Optional.ofNullable(timestamp));
            iterativeShrink.setLb(lb);
            iterativeShrink.setUb(ub);
        } catch (OpenRaoException ignored) {
            // Constraint iterativeShrink has not yet been created
            OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state, Optional.ofNullable(timestamp));
            OpenRaoMPConstraint iterativeShrink = linearProblem.addRangeActionRelativeSetpointConstraint(lb, ub, rangeAction, state, LinearProblem.RaRangeShrinking.TRUE, Optional.ofNullable(timestamp));
            iterativeShrink.setCoefficient(setPointVariable, 1);
        }
    }

    /**
     * Build range action constraints for each RangeAction r.
     * These constraints link the set-point variable of the RangeAction with its
     * variation variables, and bounds the set-point in an admissible range.
     * S[r,s] = initialSetPoint[r,s] + upwardVariation[r,s] - downwardVariation[r,s]
     *
     * If r in an injection action, add contribution of its variation to the global balancing constraint (as defined below)
     * sum{r InjectionRangeAction} (upwardVariation[r,s] - downwardVariation[r,s]) x sum{distribution keys of r} = 0
     */
    protected abstract void buildConstraintsForRangeActionAndState(LinearProblem linearProblem, RangeAction<?> rangeAction, State state);

    protected void addSetPointConstraints(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state, Optional.ofNullable(timestamp));
        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state, Optional.ofNullable(timestamp));
        OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD, Optional.ofNullable(timestamp));
        OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD, Optional.ofNullable(timestamp));

        OpenRaoMPConstraint absoluteVariationConstraint = linearProblem.addRangeActionAbsoluteVariationConstraint(rangeAction, state, Optional.ofNullable(timestamp));
        absoluteVariationConstraint.setCoefficient(absoluteVariationVariable, 1.0);
        absoluteVariationConstraint.setCoefficient(upwardVariationVariable, -1.0);
        absoluteVariationConstraint.setCoefficient(downwardVariationVariable, -1.0);

        OpenRaoMPConstraint setPointVariationConstraint = linearProblem.addRangeActionSetPointVariationConstraint(rangeAction, state, Optional.ofNullable(timestamp));
        setPointVariationConstraint.setCoefficient(setPointVariable, 1.0);
        setPointVariationConstraint.setCoefficient(upwardVariationVariable, -1.0);
        setPointVariationConstraint.setCoefficient(downwardVariationVariable, 1.0);

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

            setPointVariationConstraint.setLb(prePerimeterSetPoint);
            setPointVariationConstraint.setUb(prePerimeterSetPoint);
        } else {

            // range action have been activated in a previous instant
            // getRangeActionSetpointVariable from previous instant
            OpenRaoMPVariable previousSetpointVariable = linearProblem.getRangeActionSetpointVariable(lastAvailableRangeAction.getLeft(), lastAvailableRangeAction.getValue(), Optional.ofNullable(timestamp));

            List<Double> minAndMaxAbsoluteAndRelativeSetpoints = getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction, linearProblem.infinity());
            double minAbsoluteSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(0);
            double maxAbsoluteSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(1);
            double minRelativeSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(2);
            double maxRelativeSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(3);

            // relative range
            if (PstModel.CONTINUOUS.equals(pstModel) || !(rangeAction instanceof PstRangeAction)) {
                OpenRaoMPConstraint relSetpointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(minRelativeSetpoint, maxRelativeSetpoint, rangeAction, state, LinearProblem.RaRangeShrinking.FALSE, Optional.ofNullable(timestamp));
                relSetpointConstraint.setCoefficient(setPointVariable, 1);
                relSetpointConstraint.setCoefficient(previousSetpointVariable, -1);
            }

            // absolute range
            setPointVariable.setLb(minAbsoluteSetpoint - RANGE_ACTION_SETPOINT_EPSILON);
            setPointVariable.setUb(maxAbsoluteSetpoint + RANGE_ACTION_SETPOINT_EPSILON);

            setPointVariationConstraint.setCoefficient(previousSetpointVariable, -1.0);
        }

        if (rangeAction instanceof InjectionRangeAction injectionRangeAction) {
            double totalShiftKey = injectionRangeAction.getInjectionDistributionKeys().values().stream().mapToDouble(v -> v).sum();
            OpenRaoMPConstraint injectionBalanceConstraint = linearProblem.getInjectionBalanceConstraint(state, Optional.ofNullable(timestamp));
            injectionBalanceConstraint.setCoefficient(upwardVariationVariable, totalShiftKey);
            injectionBalanceConstraint.setCoefficient(downwardVariationVariable, -totalShiftKey);
        }
    }

    protected static List<Double> getMinAndMaxAbsoluteAndRelativeSetpoints(RangeAction<?> rangeAction, double infinity) {

        // if relative to previous instant range
        double minAbsoluteSetpoint = -infinity;
        double maxAbsoluteSetpoint = infinity;
        double minRelativeSetpoint = -infinity;
        double maxRelativeSetpoint = infinity;
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            Map<Integer, Double> tapToAngleMap = pstRangeAction.getTapToAngleConversionMap();
            List<TapRange> ranges = pstRangeAction.getRanges();

            int minAbsoluteTap = tapToAngleMap.keySet().stream().mapToInt(k -> k).min().orElseThrow();
            int maxAbsoluteTap = tapToAngleMap.keySet().stream().mapToInt(k -> k).max().orElseThrow();
            double minRelativeTap = -infinity;
            double maxRelativeTap = infinity;
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
                    default:
                        throw new OpenRaoException(String.format("Unsupported range type %s", rangeType));
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
                    default:
                        throw new OpenRaoException(String.format("Unsupported range type %s", rangeType));
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

    protected abstract void fillObjective(LinearProblem linearProblem);

    protected static double getRangeActionPenaltyCost(RangeAction<?> rangeAction, RangeActionsOptimizationParameters rangeActionParameters) {
        if (rangeAction instanceof PstRangeAction) {
            return rangeActionParameters.getPstRAMinImpactThreshold();
        } else if (rangeAction instanceof HvdcRangeAction) {
            return rangeActionParameters.getHvdcRAMinImpactThreshold();
        } else if (rangeAction instanceof InjectionRangeAction) {
            return rangeActionParameters.getInjectionRAMinImpactThreshold();
        } else {
            throw new OpenRaoException("Unexpected type of range action: '%s'.".formatted(rangeAction.getClass().getSimpleName()));
        }
    }
}
