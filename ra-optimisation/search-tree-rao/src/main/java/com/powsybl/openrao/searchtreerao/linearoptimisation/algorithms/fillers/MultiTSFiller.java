package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.TapRange;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
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

public class MultiTSFiller implements ProblemFiller {

    //Each crac describes a given time step
    private final List<Set<RangeAction<?>>> rangeActionsList;
    private final List<Set<PstRangeAction>> pstRangeActionsList;
    private final List<Network> networksList;
    private final List<State> statesList;
    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final List<Set<FlowCnec>> cnecsList;
    private final RangeActionActivationResult raActivationFromParentLeaf;

    private final Map<RangeAction<?>, RangeAction<?>> rangeActionsConstraintsToUpdate = new HashMap<>();

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

        // Better way to filter only PstRangeActions?
        List<Set<PstRangeAction>> pstRangeActionsList = new ArrayList<>();
        for (Set<RangeAction<?>> rangeActionsSet : rangeActionsList) {
            Set<PstRangeAction> pstRangeActionsSet = new HashSet<>();
            for (RangeAction<?> rangeAction : rangeActionsSet) {
                if (rangeAction instanceof PstRangeAction pstRangeAction) {
                    pstRangeActionsSet.add(pstRangeAction);
                }
            }
            pstRangeActionsList.add(pstRangeActionsSet);
        }
        this.pstRangeActionsList = pstRangeActionsList;
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
                    if (rangeActionsConstraintsToUpdate.containsKey(currentRangeAction)) {
                        RangeAction<?> previousRangeAction = rangeActionsConstraintsToUpdate.get(currentRangeAction);
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
                    if (rangeActionsConstraintsToUpdate.containsKey(currentRangeAction)) {
                        RangeAction<?> previousRangeAction = rangeActionsConstraintsToUpdate.get(currentRangeAction);
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
            for (RangeAction <?> currentRangeAction : rangeActionsList.get(timeStepIndex)) {
                // we can't use rangeActionsList instead because we need getNetworkElement()
                for (int nextTimeStepIndex = timeStepIndex + 1; nextTimeStepIndex < rangeActionsList.size(); nextTimeStepIndex++) {
                    // check if next time steps contains current pst
                    boolean futureRangeActionFound = false;
                    for (RangeAction <?> previousRangeAction : rangeActionsList.get(nextTimeStepIndex)) {
                        boolean hasSameNetworksElements = currentRangeAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet())
                            .equals(previousRangeAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet()));
                        if (hasSameNetworksElements) {
                            futureRangeActionFound = true;
                            break;
                        }
                    }
                    if (!futureRangeActionFound) {
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
    //TODO: check if only works for pst or all range acitons
    private void addImpactOfRangeActionOnLaterTimeSteps(LinearProblem linearProblem, SensitivityResult sensitivityResult, RangeAction <?> pstRangeAction, int currentTimeStepIndex, int nextTimeStepIndex, RangeActionActivationResult rangeActionActivationResult) {
        cnecsList.get(nextTimeStepIndex).forEach(cnec -> {
            Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(cnecsList.get(nextTimeStepIndex), sensitivityResult);
            if (validFlowCnecs.contains(cnec)) {
                cnec.getMonitoredSides().forEach(side -> {
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
                });
            }
        });
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
     * Update penalty cost for PSTs in order to compare to previous TS and not initial value
     */
    private void updateObjectivePenaltyCost(LinearProblem linearProblem, PstRangeAction currentRangeAction, PstRangeAction previousRangeAction, Integer i) {
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
            buildPstConstraints(linearProblem, timeStepIndex);
        }
    }

    private void buildPstConstraints(LinearProblem linearProblem, int timeStepIndex) {
        for (RangeAction<?> currentRangeAction : pstRangeActionsList.get(timeStepIndex)) {
            Set<RangeAction<?>> previousRangeActionSet = new HashSet<>();
            int pastTimeStepIndex = timeStepIndex - 1;
            // currentRangeAction may be only defined several time steps before the current one
            // so we need to check every time step until we find it

//            while (previousRangeActionSet.isEmpty() && timeStepIndex >= 0) {
//                previousRangeActionSet = pstRangeActionsList.get(timeStepIndex)
//                    .stream()
//                    .filter(rangeAction -> rangeAction.getNetworkElement().getId().equals(currentRangeAction.getNetworkElement().getId()))
//                    .collect(Collectors.toSet());
//                --timeStepIndex;
//            }
            while (previousRangeActionSet.isEmpty() && pastTimeStepIndex >= 0) {
                for (RangeAction <?> previousRangeAction : pstRangeActionsList.get(pastTimeStepIndex)) {
                    boolean hasSameNetworksElements = currentRangeAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet())
                        .equals(previousRangeAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet()));
                    if (hasSameNetworksElements) {
                        previousRangeActionSet.add(previousRangeAction);
                    }
                }
                --pastTimeStepIndex;
            }

            if (previousRangeActionSet.size() == 1) {
                if (currentRangeAction instanceof PstRangeAction pstCurrentRangeAction) {
                    PstRangeAction previousRangeAction = (PstRangeAction) previousRangeActionSet.stream().findAny().orElse(null);
                    if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.CONTINUOUS) {
                        buildConstraintOneTimeStepContinuous(linearProblem, pstCurrentRangeAction, previousRangeAction, timeStepIndex);
                    } else if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
                        buildConstraintOneTimeStepDiscrete(linearProblem, pstCurrentRangeAction, previousRangeAction, timeStepIndex);
                    }
                    updateObjectivePenaltyCost(linearProblem, pstCurrentRangeAction, previousRangeAction, timeStepIndex);
                }
            } else if (previousRangeActionSet.size() > 1) {
                throw new NotImplementedException(
                    previousRangeActionSet.size()
                        + " Range action found for the same network elements: "
                        + currentRangeAction.getNetworkElements().toString()
                );
            }
        }
    }

    /**
     * Add constraint on the preivous time step for a Pst
     * Continuous case: constraint on setpoint variables
     */
    private void buildConstraintOneTimeStepContinuous(LinearProblem linearProblem, PstRangeAction currentRangeAction, PstRangeAction previousRangeAction, int timeStepIndex) {
        OpenRaoMPVariable currentTimeStepVariable = linearProblem.getRangeActionSetpointVariable(currentRangeAction, statesList.get(timeStepIndex));
        OpenRaoMPVariable previousTimeStepVariable = linearProblem.getRangeActionSetpointVariable(previousRangeAction, statesList.get(timeStepIndex - 1));
        List<TapRange> ranges = currentRangeAction.getRanges();
        List<TapRange> rangesRelativeTimeStep = ranges
            .stream()
            .filter(range -> range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)
            .toList();
        for (TapRange range : rangesRelativeTimeStep) {
            List<Integer> minAndMaxRelativeSetPoints = getMinAndMaxTapsTimeStep(range);

            double minRelativeTap = minAndMaxRelativeSetPoints.get(0);
            double maxRelativeTap = minAndMaxRelativeSetPoints.get(1);

            double minRelativeSetpoint = minRelativeTap * currentRangeAction.getSmallestAngleStep();
            double maxRelativeSetpoint = maxRelativeTap * currentRangeAction.getSmallestAngleStep();

            OpenRaoMPConstraint relSetPointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(minRelativeSetpoint, maxRelativeSetpoint, currentRangeAction, statesList.get(timeStepIndex), LinearProblem.RaRangeShrinking.FALSE);
            relSetPointConstraint.setCoefficient(currentTimeStepVariable, 1);
            relSetPointConstraint.setCoefficient(previousTimeStepVariable, -1);
        }
    }

    /**
     * Add constraint on the preivous time step for a Pst
     * Discrete case: constraint on tap variables
     * min_tap_variation < f[t] - f[t-1] +  (F[up,t] - F[down,t]) - (F[up,t-1] - F[up,t-1])  < max_tap_variation
     * t: timestep / f: tap position from previous iteration
     */
    private void buildConstraintOneTimeStepDiscrete(LinearProblem linearProblem, PstRangeAction currentRangeAction, PstRangeAction previousRangeAction, int timeStepIndex) {
        OpenRaoMPVariable pstTapCurrentDownwardVariationVariable = linearProblem.getPstTapVariationVariable(currentRangeAction, statesList.get(timeStepIndex), LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapCurrentUpwardVariationVariable = linearProblem.getPstTapVariationVariable(currentRangeAction, statesList.get(timeStepIndex), LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable pstTapPreviousDownwardVariationVariable = linearProblem.getPstTapVariationVariable(previousRangeAction, statesList.get(timeStepIndex - 1), LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapPreviousUpwardVariationVariable = linearProblem.getPstTapVariationVariable(previousRangeAction, statesList.get(timeStepIndex - 1), LinearProblem.VariationDirectionExtension.UPWARD);

        List<TapRange> ranges = currentRangeAction.getRanges();
        List<TapRange> rangesRelativeTimeStep = ranges
            .stream()
            .filter(range -> range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)
            .toList();
        for (TapRange range : rangesRelativeTimeStep) {
            List<Integer> minAndMaxRelativeTaps = getMinAndMaxTapsTimeStep(range);
            rangeActionsConstraintsToUpdate.put(currentRangeAction, previousRangeAction);

            double minRelativeTap = minAndMaxRelativeTaps.get(0);
            double maxRelativeTap = minAndMaxRelativeTaps.get(1);
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

    private List<Integer> getMinAndMaxTapsTimeStep(TapRange range) {

        int minRelativeTap = Integer.MIN_VALUE;
        int maxRelativeTap = Integer.MAX_VALUE;

        RangeType rangeType = range.getRangeType();
        if (rangeType == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP) {
            minRelativeTap = range.getMinTap();
            maxRelativeTap = range.getMaxTap();
        } else {
            throw new NotImplementedException("range action type is not supported yet");
        }
        return List.of(minRelativeTap, maxRelativeTap);
    }

    /**
     * Update bounds for constraints on tap variation
     */
    public void updateTapValueConstraints(LinearProblem linearProblem, RangeAction<?> currentRangeAction, RangeAction<?> previousRangeAction, RangeActionActivationResult rangeActionActivationResult, int timeStepIndex) {
        if (currentRangeAction instanceof PstRangeAction pstCurrentRangeAction && previousRangeAction instanceof PstRangeAction pstPreviousRangeAction) {
            OpenRaoMPConstraint tapRelTimeStepConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstCurrentRangeAction, statesList.get(timeStepIndex), LinearProblem.RaRangeShrinking.FALSE);

            List<TapRange> ranges = pstCurrentRangeAction.getRanges();
            List<TapRange> rangesRelativeTimeStep = ranges
                .stream()
                .filter(range -> range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)
                .toList();

            for (TapRange range : rangesRelativeTimeStep) {
                List<Integer> minAndMaxRelativeTaps = getMinAndMaxTapsTimeStep(range);
                double minRelativeTap = minAndMaxRelativeTaps.get(0);
                double maxRelativeTap = minAndMaxRelativeTaps.get(1);

                double currentTimeStepTapOptimized = rangeActionActivationResult.getOptimizedTap(pstCurrentRangeAction, statesList.get(timeStepIndex));
                double previousTimeStepTapOptimized = rangeActionActivationResult.getOptimizedTap(pstPreviousRangeAction, statesList.get(timeStepIndex - 1));

                double lbConstraintUpdate = minRelativeTap - currentTimeStepTapOptimized + previousTimeStepTapOptimized;
                double ubConstraintUpdate = maxRelativeTap - currentTimeStepTapOptimized + previousTimeStepTapOptimized;
                tapRelTimeStepConstraint.setBounds(lbConstraintUpdate, ubConstraintUpdate);
            }
        }
    }
}
