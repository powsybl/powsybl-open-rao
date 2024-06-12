package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
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
            .stream().map(perimeter -> perimeter.getRangeActions().stream().collect(Collectors.toSet()))
            .collect(Collectors.toList());
        this.networksList = networksList;
        this.statesList = optimizationPerimeters
            .stream().map(perimeter -> perimeter.getMainOptimizationState())
            .collect(Collectors.toList());
        this.rangeActionParameters = rangeActionParameters;
        this.cnecsList = optimizationPerimeters
            .stream().map(perimeter -> perimeter.getFlowCnecs())
            .collect(Collectors.toList());
        this.raActivationFromParentLeaf = raActivationFromParentLeaf;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {

        // Better way to filter only PstRangeActions?
        List<Set<PstRangeAction>> pstRangeActionList = new ArrayList<>();
        for (Set<RangeAction<?>> rangeActionsSet : rangeActionsList) {
            Set<PstRangeAction> pstRangeActionsSet = new HashSet<>();
            for (RangeAction<?> rangeAction : rangeActionsSet) {
                if (rangeAction instanceof PstRangeAction pstRangeAction) {
                    pstRangeActionsSet.add(pstRangeAction);
                }
            }
            pstRangeActionList.add(pstRangeActionsSet);
        }
        updateFlowConstraints(linearProblem, sensitivityResult, pstRangeActionList);
        buildPstConstraintsAcrossTimeSteps(linearProblem, pstRangeActionList);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        //run?
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

    private void updateFlowConstraints(LinearProblem linearProblem, SensitivityResult sensitivityResult, List<Set<PstRangeAction>> pstRangeActionsList) {
        for (int i = 1; i < pstRangeActionsList.size(); i++) {
            for (PstRangeAction previousRangeAction : pstRangeActionsList.get(i - 1)) {
                if (!pstRangeActionsList.get(i).contains(previousRangeAction)) {
                    addImpactOfRangeActionOnLaterTimeSteps(linearProblem, sensitivityResult, previousRangeAction, i);
                }
            }
        }
    }

    private void addImpactOfRangeActionOnLaterTimeSteps(LinearProblem linearProblem, SensitivityResult sensitivityResult, PstRangeAction pstRangeAction, int i) {
        cnecsList.get(i).forEach(cnec -> {
            Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(cnecsList.get(i), sensitivityResult);
            if (validFlowCnecs.contains(cnec)) {
                cnec.getMonitoredSides().forEach(side -> {
                    OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec, side);
                    OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, statesList.get(i - 1));
                    double sensitivity = sensitivityResult.getSensitivityValue(cnec, side, pstRangeAction, Unit.MEGAWATT);

                    if (isRangeActionSensitivityAboveThreshold(pstRangeAction, Math.abs(sensitivity))) {
                        double currentSetPoint = raActivationFromParentLeaf.getOptimizedSetpoint(pstRangeAction, statesList.get(i - 1));

                        // care : might not be robust as getCurrentValue get the current setPoint from a network variant
                        //        we need to be sure that this variant has been properly set
                        flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentSetPoint);
                        flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentSetPoint);
                        flowConstraint.setCoefficient(setPointVariable, -sensitivity);
                    } else {
                        // We need to do this in case of an update
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

        varConstraintPositive.setLb(0);
        varConstraintPositive.setCoefficient(previousTSSetPointVariable, -1);
        varConstraintNegative.setLb(0);
        varConstraintNegative.setCoefficient(previousTSSetPointVariable, 1);

    }

    /**
     * Builds constraints between time steps for every Pst that have a range "RELATIVE_TO_PREVIOUS_TIME_STEP"
     */

    private void buildPstConstraintsAcrossTimeSteps(LinearProblem linearProblem, List<Set<PstRangeAction>> pstRangeActionsList) {

        for (int i = 1; i < pstRangeActionsList.size(); i++) {
            for (PstRangeAction currentRangeAction : pstRangeActionsList.get(i)) {
                Set<PstRangeAction> previousRangeActionSet = pstRangeActionsList.get(i - 1)
                    .stream()
                    .filter(rangeAction -> rangeAction.getNetworkElement().getName().equals(currentRangeAction.getNetworkElement().getName()))
                    .collect(Collectors.toSet());
                if (previousRangeActionSet.size() == 1) {
                    PstRangeAction previousRangeAction = previousRangeActionSet.stream().findAny().orElse(null);
                    if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.CONTINUOUS) {
                        buildConstraintOneTimeStepContinuous(
                            linearProblem,
                            currentRangeAction,
                            previousRangeAction,
                            i);
                    } else if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
                        buildConstraintOneTimeStepDiscrete(
                            linearProblem,
                            currentRangeAction,
                            previousRangeAction,
                            i);
                    }

                    updateObjectivePenaltyCost(linearProblem, currentRangeAction, previousRangeAction, i);
                } else if (previousRangeActionSet.size() > 1) {
                    throw new NotImplementedException(
                        previousRangeActionSet.size()
                            + " PST found for the same network element: "
                            + currentRangeAction.getNetworkElement().getName()
                    );
                }
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
