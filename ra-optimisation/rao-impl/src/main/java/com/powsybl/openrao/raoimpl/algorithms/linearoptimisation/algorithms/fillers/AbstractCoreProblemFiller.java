/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering.FlowCnecFilter;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.raoimpl.commons.RaoUtil;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionActivationResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.raoimpl.result.api.SensitivityResult;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.PstModel;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractCoreProblemFiller implements ProblemFiller {
    protected static final double RANGE_ACTION_SETPOINT_EPSILON = RangeActionSetpointBounds.RANGE_ACTION_SETPOINT_EPSILON;
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
    private final RangeActionImpacts rangeActionImpacts;
    private final FlowCnecFilter flowCnecFilter;

    protected AbstractCoreProblemFiller(OptimizationPerimeter optimizationContext,
                                        RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                                        RangeActionsOptimizationParameters rangeActionParameters,
                                        SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension,
                                        Unit unit,
                                        boolean raRangeShrinking,
                                        PstModel pstModel,
                                        OffsetDateTime timestamp,
                                        FlowCnecFilter flowCnecFilter) {
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
        this.rangeActionImpacts = new RangeActionImpacts(optimizationContext, rangeActionParametersExtension, unit);
        this.flowCnecFilter = flowCnecFilter;
    }

    public FlowCnecFilter getFlowCnecFilter() {
        return flowCnecFilter;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        Set<FlowCnec> validFlowCnecs = flowCnecFilter.retain(FillersUtil.getFlowCnecsComputationStatusOk(flowCnecs, sensitivityResult), flowResult, sensitivityResult, rangeActionActivationResult);

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
     * This variable describes the estimated flow on the given Cnec c, in the unit of the objective function
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
     *     <li>in the unit of the objective function for HVDC range actions</li>
     *     <li>in the unit of the objective function for Injection range actions</li>
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
        linearProblem.addRangeActionSetpointVariable(-linearProblem.infinity(), linearProblem.infinity(), rangeAction, state);
        linearProblem.addRangeActionVariationVariable(linearProblem.infinity(), rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        linearProblem.addRangeActionVariationVariable(linearProblem.infinity(), rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
    }

    /**
     * Build one flow constraint for each Cnec c.
     * These constraints link the estimated flow on a Cnec with the impact of the RangeActions
     * on this Cnec.
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void buildFlowConstraints(LinearProblem linearProblem,
                                      Set<FlowCnec> validFlowCnecs,
                                      FlowResult flowResult,
                                      SensitivityResult sensitivityResult,
                                      RangeActionActivationResult rangeActionActivationResult) {
        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            // create constraint
            double referenceFlow = flowResult.getFlow(cnec, side, unit); // get flow in the unit of the objective function
            OpenRaoMPConstraint flowConstraint = linearProblem.addFlowConstraint(referenceFlow, referenceFlow, cnec, side, Optional.ofNullable(timestamp));

            OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side, Optional.ofNullable(timestamp));
            flowConstraint.setCoefficient(flowVariable, 1);

            // add sensitivity coefficients
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, cnec, side, rangeActionActivationResult);
        }));
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, FlowCnec cnec, TwoSides side, RangeActionActivationResult rangeActionActivationResult) {
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec, side, Optional.ofNullable(timestamp));

        for (RangeActionImpacts.Impact impact : rangeActionImpacts.getImpacts(cnec, side, sensitivityResult)) {
            OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(impact.rangeAction(), impact.state());
            double currentSetPoint = rangeActionActivationResult.getOptimizedSetpoint(impact.rangeAction(), impact.state());

            flowConstraint.setLb(flowConstraint.lb() - impact.sensitivity() * currentSetPoint);
            flowConstraint.setUb(flowConstraint.ub() - impact.sensitivity() * currentSetPoint);

            flowConstraint.setCoefficient(setPointVariable, -impact.sensitivity());
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
            linearProblem.addInjectionBalanceConstraint(state);
        }
    }

    private void checkAndActivateRangeShrinking(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        if (!raRangeShrinking) {
            return;
        }
        if (iteration > 0) {
            // don't shrink the range for the first iteration
            optimizationContext.getRangeActionsPerState().forEach((state, rangeActionSet) ->
                rangeActionSet.forEach(rangeAction -> updateConstraintsForRangeAction(linearProblem, rangeAction, state, rangeActionActivationResult, iteration))
            );
        }
        iteration++;
    }

    private static void updateConstraintsForRangeAction(LinearProblem linearProblem, RangeAction<?> rangeAction, State state, RangeActionActivationResult rangeActionActivationResult, int iteration) {
        double previousSetPointValue = rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);
        List<Double> minAndMaxAbsoluteAndRelativeSetpoints = RangeActionSetpointBounds.getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction, linearProblem.infinity());
        double minAbsoluteSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(0);
        double maxAbsoluteSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(1);
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
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction, state);
        OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

        OpenRaoMPConstraint setPointVariationConstraint = linearProblem.addRangeActionSetPointVariationConstraint(rangeAction, state);
        setPointVariationConstraint.setCoefficient(setPointVariable, 1.0);
        setPointVariationConstraint.setCoefficient(upwardVariationVariable, -1.0);
        setPointVariationConstraint.setCoefficient(downwardVariationVariable, 1.0);

        Pair<RangeAction<?>, State> lastAvailableRangeAction = RaoUtil.getLastAvailableRangeActionOnSameNetworkElement(optimizationContext, rangeAction, state);

        RangeActionSetpointBounds.Interval bounds = RangeActionSetpointBounds.getSetpointVariableBounds(
            optimizationContext, rangeAction, state, prePerimeterRangeActionSetpoints, linearProblem.infinity());

        if (lastAvailableRangeAction == null) {
            // if state is equal to masterState,
            // or if rangeAction is not available for a previous state
            // then, rangeAction could not have been activated in a previous instant

            double prePerimeterSetPoint = prePerimeterRangeActionSetpoints.getSetpoint(rangeAction);

            setPointVariable.setLb(bounds.min());
            setPointVariable.setUb(bounds.max());

            setPointVariationConstraint.setLb(prePerimeterSetPoint);
            setPointVariationConstraint.setUb(prePerimeterSetPoint);
        } else {

            // range action have been activated in a previous instant
            // getRangeActionSetpointVariable from previous instant
            OpenRaoMPVariable previousSetpointVariable = linearProblem.getRangeActionSetpointVariable(lastAvailableRangeAction.getLeft(), lastAvailableRangeAction.getValue());

            List<Double> minAndMaxAbsoluteAndRelativeSetpoints = RangeActionSetpointBounds.getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction, linearProblem.infinity());
            double minRelativeSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(2);
            double maxRelativeSetpoint = minAndMaxAbsoluteAndRelativeSetpoints.get(3);

            // relative range
            if (PstModel.CONTINUOUS.equals(pstModel) || !(rangeAction instanceof PstRangeAction)) {
                OpenRaoMPConstraint relSetpointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(
                    minRelativeSetpoint, maxRelativeSetpoint, rangeAction, state, LinearProblem.RaRangeShrinking.FALSE);
                relSetpointConstraint.setCoefficient(setPointVariable, 1);
                relSetpointConstraint.setCoefficient(previousSetpointVariable, -1);
            }

            // absolute range
            setPointVariable.setLb(bounds.min());
            setPointVariable.setUb(bounds.max());

            setPointVariationConstraint.setCoefficient(previousSetpointVariable, -1.0);
        }

        if (rangeAction instanceof InjectionRangeAction injectionRangeAction) {
            double totalShiftKey = injectionRangeAction.getInjectionDistributionKeysIds().values().stream().mapToDouble(v -> v).sum();
            OpenRaoMPConstraint injectionBalanceConstraint = linearProblem.getInjectionBalanceConstraint(state);
            injectionBalanceConstraint.setCoefficient(upwardVariationVariable, totalShiftKey);
            injectionBalanceConstraint.setCoefficient(downwardVariationVariable, -totalShiftKey);
        }
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
