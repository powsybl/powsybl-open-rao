/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.StandardRange;
import com.powsybl.openrao.data.cracapi.range.TapRange;
import com.powsybl.openrao.data.cracapi.rangeaction.*;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class MultiTSFiller implements ProblemFiller {

    //Each element from a list describes a given time step
    private final List<Set<RangeAction<?>>> rangeActionsList;
    private final List<Network> networksList;
    private final List<State> statesList;
    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final List<Set<FlowCnec>> cnecsList;
    private final RangeActionActivationResult raActivationFromParentLeaf;

    private final Map<RangeAction<?>, RangeAction<?>> currentAndPreviousRangeActions = new HashMap<>();

    public MultiTSFiller(List<OptimizationPerimeter> optimizationPerimeters,
                         List<Network> networksList,
                         RangeActionsOptimizationParameters rangeActionParameters,
                         RangeActionActivationResult raActivationFromParentLeaf
    ) {

        this.rangeActionsList = optimizationPerimeters
            .stream().map(perimeter -> new HashSet<>(perimeter.getRangeActions()))
            .collect(Collectors.toList());
        this.networksList = networksList;
        this.statesList = optimizationPerimeters
            .stream().map(OptimizationPerimeter::getMainOptimizationState)
            .collect(Collectors.toList());
        this.rangeActionParameters = rangeActionParameters;
        this.cnecsList = optimizationPerimeters
            .stream().map(OptimizationPerimeter::getFlowCnecs)
            .collect(Collectors.toList());
        this.raActivationFromParentLeaf = raActivationFromParentLeaf;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        updateFlowConstraints(linearProblem, sensitivityResult, raActivationFromParentLeaf);
        buildRangeActionConstraintsAcrossTimeSteps(linearProblem);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
            for (int i = 1; i < rangeActionsList.size(); i++) {
                for (RangeAction<?> currentRangeAction : rangeActionsList.get(i)) {
                    if (currentAndPreviousRangeActions.containsKey(currentRangeAction)) {
                        RangeAction<?> previousRangeAction = currentAndPreviousRangeActions.get(currentRangeAction);
                        updateTapValueConstraints(linearProblem, currentRangeAction, previousRangeAction, rangeActionActivationResult, i);
                    }
                }
            }
        }
        updateFlowConstraints(linearProblem, sensitivityResult, rangeActionActivationResult);
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
            for (int i = 1; i < rangeActionsList.size(); i++) {
                for (RangeAction<?> currentRangeAction : rangeActionsList.get(i)) {
                    if (currentAndPreviousRangeActions.containsKey(currentRangeAction)) {
                        RangeAction<?> previousRangeAction = currentAndPreviousRangeActions.get(currentRangeAction);
                        updateTapValueConstraints(linearProblem, currentRangeAction, previousRangeAction, rangeActionActivationResult, i);
                    }
                }
            }
        }
    }

    /**
     * Update flow constraints to take into account the range actions of previous time steps
     */
    private void updateFlowConstraints(LinearProblem linearProblem, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        for (int timeStepIndex = 0; timeStepIndex < rangeActionsList.size() - 1; timeStepIndex++) {
            for (RangeAction<?> currentRangeAction : rangeActionsList.get(timeStepIndex)) {
                for (int nextTimeStepIndex = timeStepIndex + 1; nextTimeStepIndex < rangeActionsList.size(); nextTimeStepIndex++) {
                    // check if next time steps contains current range action
                    boolean futureRangeActionFound = false;
                    for (RangeAction<?> previousRangeAction : rangeActionsList.get(nextTimeStepIndex)) {
                        boolean hasSameNetworksElements = currentRangeAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet())
                            .equals(previousRangeAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet()));
                        if (hasSameNetworksElements) {
                            futureRangeActionFound = true;
                            break;
                        }
                    }
                    //For injection, if no range action at future time step, use the value given by the network, not by the previous range action
                    if (!futureRangeActionFound && !(currentRangeAction instanceof InjectionRangeAction)) {
                        addImpactOfRangeActionOnLaterTimeSteps(linearProblem, sensitivityResult, currentRangeAction, timeStepIndex, nextTimeStepIndex, rangeActionActivationResult);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Add variable from previous time step to constraint and update sensi
     */
    private void addImpactOfRangeActionOnLaterTimeSteps(LinearProblem linearProblem, SensitivityResult sensitivityResult, RangeAction<?> pstRangeAction, int currentTimeStepIndex, int nextTimeStepIndex, RangeActionActivationResult rangeActionActivationResult) {
        Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(cnecsList.get(nextTimeStepIndex), sensitivityResult);
        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec, side);
            OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, statesList.get(currentTimeStepIndex));
            double sensitivity = sensitivityResult.getSensitivityValue(cnec, side, pstRangeAction, Unit.MEGAWATT);

            if (isRangeActionSensitivityAboveThreshold(pstRangeAction, Math.abs(sensitivity))) {
                double currentSetPoint = rangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, statesList.get(currentTimeStepIndex));
                flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentSetPoint);
                flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentSetPoint);
                flowConstraint.setCoefficient(setPointVariable, -sensitivity);
            } else {
                flowConstraint.setCoefficient(setPointVariable, 0);
            }
        }));
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

    /**
     * Update penalty cost for range actions in order to compare to previous TS and not initial value.
     * The goal is to have closer setpoints between time steps.
     */
    private void updateObjectivePenaltyCost(LinearProblem linearProblem, RangeAction<?> currentRangeAction, RangeAction<?> previousRangeAction, Integer i) {
        OpenRaoMPVariable previousTSSetPointVariable = linearProblem.getRangeActionSetpointVariable(previousRangeAction, statesList.get(i - 1));

        OpenRaoMPConstraint varConstraintPositive = linearProblem.getAbsoluteRangeActionVariationConstraint(
            currentRangeAction,
            statesList.get(i),
            LinearProblem.AbsExtension.POSITIVE
        );
        OpenRaoMPConstraint varConstraintNegative = linearProblem.getAbsoluteRangeActionVariationConstraint(
            currentRangeAction,
            statesList.get(i),
            LinearProblem.AbsExtension.NEGATIVE
        );
        // variation = set_point_current - set_point_previous_variable
        // instead of
        // variation = set_point_current - initial_set_point_value
        varConstraintPositive.setLb(0);
        varConstraintPositive.setCoefficient(previousTSSetPointVariable, -1);
        varConstraintNegative.setLb(0);
        varConstraintNegative.setCoefficient(previousTSSetPointVariable, 1);
    }

    /**
     * Builds constraints between time steps for every Pst that have a range "RELATIVE_TO_PREVIOUS_TIME_STEP"
     */
    private void buildRangeActionConstraintsAcrossTimeSteps(LinearProblem linearProblem) {
        for (int timeStepIndex = 1; timeStepIndex < rangeActionsList.size(); timeStepIndex++) {
            for (RangeAction<?> currentRangeAction : rangeActionsList.get(timeStepIndex)) {
                Set<RangeAction<?>> previousRangeActionSet = getPastRangeActions(currentRangeAction, timeStepIndex);

                if (previousRangeActionSet.size() == 1) {
                    RangeAction<?> previousRangeAction = previousRangeActionSet.stream().findAny().orElse(null);
                    if (currentRangeAction instanceof PstRangeAction pstCurrentRangeAction) {
                        PstRangeAction pstPreviousRangeAction = (PstRangeAction) previousRangeAction;
                        if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.CONTINUOUS) {
                            buildConstraintPstOneTimeStepContinuous(linearProblem, pstCurrentRangeAction, pstPreviousRangeAction, timeStepIndex);
                        } else if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
                            buildConstraintPstOneTimeStepDiscrete(linearProblem, pstCurrentRangeAction, pstPreviousRangeAction, timeStepIndex);
                        }
                    } else if (currentRangeAction instanceof InjectionRangeAction injectionCurrentRangeAction) {
                        StandardRangeAction<?> injectionPreviousRangeAction = (StandardRangeAction<?>) previousRangeAction;
                        buildConstraintOneTimeStepInjection(linearProblem, injectionCurrentRangeAction, injectionPreviousRangeAction, timeStepIndex);
                    }
                    updateObjectivePenaltyCost(linearProblem, currentRangeAction, previousRangeAction, timeStepIndex);
                } else if (previousRangeActionSet.size() > 1) {
                    throw new NotImplementedException(previousRangeActionSet.size() + " Range actions found for the same network elements: " + currentRangeAction.getNetworkElements().toString());
                }
            }
        }
    }

    /**
     * Get a set of RangeActions that is identical to currentRangeAction but from the closest previous time step
     * Stop the search as soon as a time step contains a corresponding RangeAction
     * Length of this set is supposed to be maximal to 1
     */
    private Set<RangeAction<?>> getPastRangeActions(RangeAction<?> currentRangeAction, int timeStepIndex) {
        Set<RangeAction<?>> previousRangeActionSet = new HashSet<>();
        int pastTimeStepIndex = timeStepIndex - 1;
        // currentRangeAction may be only defined several time steps before the current one
        // so we need to check every time step until we find it
        while (previousRangeActionSet.isEmpty() && pastTimeStepIndex >= 0) {
            for (RangeAction<?> previousRangeAction : rangeActionsList.get(pastTimeStepIndex)) {
                boolean hasSameNetworksElements = currentRangeAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet())
                    .equals(previousRangeAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet()));
                if (hasSameNetworksElements && currentRangeAction.getClass().equals(previousRangeAction.getClass())) {
                    previousRangeActionSet.add(previousRangeAction);
                }
            }
            --pastTimeStepIndex;
        }
        return previousRangeActionSet;
    }

    /**
     * Add constraint on the previous time step for an Injection
     * min_setpoint_variation < F[t] - F[t-1] < max_setpoint_variation
     * t: timestep
     */
    private void buildConstraintOneTimeStepInjection(LinearProblem linearProblem, StandardRangeAction<?> currentRangeAction, StandardRangeAction<?> previousRangeAction, int timeStepIndex) {
        OpenRaoMPVariable currentTimeStepVariable = linearProblem.getRangeActionSetpointVariable(currentRangeAction, statesList.get(timeStepIndex));
        OpenRaoMPVariable previousTimeStepVariable = linearProblem.getRangeActionSetpointVariable(previousRangeAction, statesList.get(timeStepIndex - 1));
        List<StandardRange> rangesRelativeTimeStep = currentRangeAction.getRanges()
            .stream()
            .filter(range -> range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)
            .toList();
        for (StandardRange range : rangesRelativeTimeStep) {
            double minRelativeSetpoint = range.getMin();
            double maxRelativeSetpoint = range.getMax();

            OpenRaoMPConstraint relSetPointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(minRelativeSetpoint, maxRelativeSetpoint, currentRangeAction, statesList.get(timeStepIndex), LinearProblem.RaRangeShrinking.FALSE);
            relSetPointConstraint.setCoefficient(currentTimeStepVariable, 1);
            relSetPointConstraint.setCoefficient(previousTimeStepVariable, -1);
        }
    }

    /**
     * Add constraint on the previous time step for a Pst
     * Continuous case: constraint on setpoint variables
     * min_tap_variation * smallest_angle_step < F[t] - F[t-1] < max_tap_variation * smallest_angle_step
     * t: timestep
     */
    private void buildConstraintPstOneTimeStepContinuous(LinearProblem linearProblem, PstRangeAction currentRangeAction, PstRangeAction previousRangeAction, int timeStepIndex) {
        OpenRaoMPVariable currentTimeStepVariable = linearProblem.getRangeActionSetpointVariable(currentRangeAction, statesList.get(timeStepIndex));
        OpenRaoMPVariable previousTimeStepVariable = linearProblem.getRangeActionSetpointVariable(previousRangeAction, statesList.get(timeStepIndex - 1));
        List<TapRange> rangesRelativeTimeStep = currentRangeAction.getRanges()
            .stream()
            .filter(range -> range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)
            .toList();
        for (TapRange range : rangesRelativeTimeStep) {
            double minRelativeTap = range.getMinTap();
            double maxRelativeTap = range.getMaxTap();

            double minRelativeSetpoint = minRelativeTap * currentRangeAction.getSmallestAngleStep();
            double maxRelativeSetpoint = maxRelativeTap * currentRangeAction.getSmallestAngleStep();

            OpenRaoMPConstraint relSetPointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(minRelativeSetpoint, maxRelativeSetpoint, currentRangeAction, statesList.get(timeStepIndex), LinearProblem.RaRangeShrinking.FALSE);
            relSetPointConstraint.setCoefficient(currentTimeStepVariable, 1);
            relSetPointConstraint.setCoefficient(previousTimeStepVariable, -1);
        }
    }

    /**
     * Add constraint on the previous time step for a Pst
     * Discrete case: constraint on tap variables
     * min_tap_variation < f[t] - f[t-1] +  (F[up,t] - F[down,t]) - (F[up,t-1] - F[down,t-1])  < max_tap_variation
     * t: timestep / f: tap position from previous iteration
     */
    private void buildConstraintPstOneTimeStepDiscrete(LinearProblem linearProblem, PstRangeAction currentRangeAction, PstRangeAction previousRangeAction, int timeStepIndex) {
        OpenRaoMPVariable pstTapCurrentDownwardVariationVariable = linearProblem.getPstTapVariationVariable(currentRangeAction, statesList.get(timeStepIndex), LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapCurrentUpwardVariationVariable = linearProblem.getPstTapVariationVariable(currentRangeAction, statesList.get(timeStepIndex), LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable pstTapPreviousDownwardVariationVariable = linearProblem.getPstTapVariationVariable(previousRangeAction, statesList.get(timeStepIndex - 1), LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapPreviousUpwardVariationVariable = linearProblem.getPstTapVariationVariable(previousRangeAction, statesList.get(timeStepIndex - 1), LinearProblem.VariationDirectionExtension.UPWARD);
        List<TapRange> rangesRelativeTimeStep = currentRangeAction.getRanges()
            .stream()
            .filter(range -> range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)
            .toList();
        for (TapRange range : rangesRelativeTimeStep) {
            // Store current and previous RangeActions, will be used when updating between MIP/Sensi
            currentAndPreviousRangeActions.put(currentRangeAction, previousRangeAction);

            double minRelativeTap = range.getMinTap();
            double maxRelativeTap = range.getMaxTap();
            Network currentTimeStepNetwork = networksList.get(timeStepIndex);
            Network previousTimeStepNetwork = networksList.get(timeStepIndex - 1);

            double lbConstraintTimeStep = minRelativeTap - currentRangeAction.getCurrentTapPosition(currentTimeStepNetwork) + previousRangeAction.getCurrentTapPosition(previousTimeStepNetwork);
            double ubConstraintTimeStep = maxRelativeTap - currentRangeAction.getCurrentTapPosition(currentTimeStepNetwork) + previousRangeAction.getCurrentTapPosition(previousTimeStepNetwork);

            OpenRaoMPConstraint relSetPointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(lbConstraintTimeStep, ubConstraintTimeStep, currentRangeAction, statesList.get(timeStepIndex), LinearProblem.RaRangeShrinking.FALSE);
            relSetPointConstraint.setCoefficient(pstTapCurrentUpwardVariationVariable, 1);
            relSetPointConstraint.setCoefficient(pstTapPreviousUpwardVariationVariable, -1);
            relSetPointConstraint.setCoefficient(pstTapCurrentDownwardVariationVariable, -1);
            relSetPointConstraint.setCoefficient(pstTapPreviousDownwardVariationVariable, 1);
        }
    }

    /**
     * Update bounds for constraints on tap variation
     */
    public void updateTapValueConstraints(LinearProblem linearProblem, RangeAction<?> currentRangeAction, RangeAction<?> previousRangeAction, RangeActionActivationResult rangeActionActivationResult, int timeStepIndex) {
        if (currentRangeAction instanceof PstRangeAction pstCurrentRangeAction && previousRangeAction instanceof PstRangeAction pstPreviousRangeAction) {
            OpenRaoMPConstraint tapRelTimeStepConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstCurrentRangeAction, statesList.get(timeStepIndex), LinearProblem.RaRangeShrinking.FALSE);
            List<TapRange> rangesRelativeTimeStep = pstCurrentRangeAction.getRanges()
                .stream()
                .filter(range -> range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)
                .toList();

            for (TapRange range : rangesRelativeTimeStep) {
                double minRelativeTap = range.getMinTap();
                double maxRelativeTap = range.getMaxTap();

                double currentTimeStepTapOptimized = rangeActionActivationResult.getOptimizedTap(pstCurrentRangeAction, statesList.get(timeStepIndex));
                double previousTimeStepTapOptimized = rangeActionActivationResult.getOptimizedTap(pstPreviousRangeAction, statesList.get(timeStepIndex - 1));

                double lbConstraintUpdate = minRelativeTap - currentTimeStepTapOptimized + previousTimeStepTapOptimized;
                double ubConstraintUpdate = maxRelativeTap - currentTimeStepTapOptimized + previousTimeStepTapOptimized;
                tapRelTimeStepConstraint.setBounds(lbConstraintUpdate, ubConstraintUpdate);
            }
        }
    }
}
