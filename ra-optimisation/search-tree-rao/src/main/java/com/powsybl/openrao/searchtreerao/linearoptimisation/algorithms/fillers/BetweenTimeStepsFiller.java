package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.TapRange;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BetweenTimeStepsFiller implements ProblemFiller {

    //Each crac describes a given time step
    private final List<Crac> cracsList;
    private final List<Network> networksList;
    private final State state;
    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final Map<RangeAction<?>, RangeAction<?>> rangeActionsConstraintsToUpdate = new HashMap<>();

    public BetweenTimeStepsFiller(List<Crac> cracsList,
                                  List<Network> networksList,
                                  State state,
                                  RangeActionsOptimizationParameters rangeActionParameters) {
        this.cracsList = cracsList;
        this.networksList = networksList;
        this.state = state;
        this.rangeActionParameters = rangeActionParameters;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        buildConstraintsAcrossTimeSteps(linearProblem, cracsList, state);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        //run?
        if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
            for (Map.Entry<RangeAction<?>,RangeAction<?>> rangeAction: rangeActionsConstraintsToUpdate.entrySet()) {
                updateTapValueContraints(linearProblem, rangeAction.getKey(), rangeAction.getValue(), state, rangeActionActivationResult);
            }
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
            for (Map.Entry<RangeAction<?>,RangeAction<?>> rangeAction: rangeActionsConstraintsToUpdate.entrySet()) {
                updateTapValueContraints(linearProblem, rangeAction.getKey(), rangeAction.getValue(), state, rangeActionActivationResult);
            }
        }
    }

    /**
     * Builds constraints between time steps for every Pst that have a range "RELATIVE_TO_PREVIOUS_TIME_STEP"
     */

    private void buildConstraintsAcrossTimeSteps(LinearProblem linearProblem, List<Crac> cracsList, State state) {

        for (int i = 1; i < cracsList.size(); i++) {
            for (PstRangeAction currentRangeAction : cracsList.get(i).getPstRangeActions()) {
                Set<PstRangeAction> previousRangeActionSet = cracsList.get(i - 1)
                        .getPstRangeActions()
                        .stream()
                        .filter(pstRangeAction -> pstRangeAction.getNetworkElement().getName().equals(currentRangeAction.getNetworkElement().getName()))
                        .collect(Collectors.toSet());
                if (previousRangeActionSet.size() == 1) {
                    if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.CONTINUOUS) {
                        buildConstraintOneTimeStepContinuous(
                                linearProblem,
                                currentRangeAction,
                                previousRangeActionSet.stream().findAny().orElse(null),
                                state);
                    } else if (rangeActionParameters.getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
                        buildConstraintOneTimeStepDiscrete(
                                linearProblem,
                                currentRangeAction,
                                previousRangeActionSet.stream().findAny().orElse(null),
                                state,
                                i);
                    }

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
     *
     */
    private void buildConstraintOneTimeStepContinuous(LinearProblem linearProblem, PstRangeAction currentRangeAction, PstRangeAction previousRangeAction, State state) {
        OpenRaoMPVariable currentTimeStepVariable = linearProblem.getRangeActionSetpointVariable(currentRangeAction, state);
        OpenRaoMPVariable previousTimeStepVariable = linearProblem.getRangeActionSetpointVariable(previousRangeAction, state);
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

            OpenRaoMPConstraint relSetPointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(minRelativeSetpoint, maxRelativeSetpoint, currentRangeAction, state, LinearProblem.RaRangeShrinking.FALSE);
            relSetPointConstraint.setCoefficient(currentTimeStepVariable, 1);
            relSetPointConstraint.setCoefficient(previousTimeStepVariable, -1);
        }
    }


    /**
     * Add constrainton the preivous time step for a Pst
     * Discrete case: constraint on tap variables
     *
     */
    private void buildConstraintOneTimeStepDiscrete(LinearProblem linearProblem, PstRangeAction currentRangeAction, PstRangeAction previousRangeAction, State state, int timeStepIndex) {
        OpenRaoMPVariable pstTapCurrentDownwardVariationVariable = linearProblem.getPstTapVariationVariable(currentRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapCurrentUpwardVariationVariable = linearProblem.getPstTapVariationVariable(currentRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable pstTapPreviousDownwardVariationVariable = linearProblem.getPstTapVariationVariable(previousRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapPreviousUpwardVariationVariable = linearProblem.getPstTapVariationVariable(previousRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);


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

            //getCurrentTapPosition????
            double lbConstraintTimeStep = minRelativeTap - currentRangeAction.getCurrentTapPosition(currentTimeStepNetwork) + previousRangeAction.getCurrentTapPosition(previousTimeStepNetwork);
            double ubConstraintTimeStep = maxRelativeTap - currentRangeAction.getCurrentTapPosition(currentTimeStepNetwork) + previousRangeAction.getCurrentTapPosition(previousTimeStepNetwork);

            // Right constraint? (addRangeActionRelativeSetpointConstraint)
            OpenRaoMPConstraint relSetPointConstraint = linearProblem.addRangeActionRelativeSetpointConstraint(lbConstraintTimeStep, ubConstraintTimeStep, currentRangeAction, state, LinearProblem.RaRangeShrinking.FALSE);
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

    public void updateTapValueContraints(LinearProblem linearProblem, RangeAction<?> currentRangeAction, RangeAction<?> previousRangeAction, State state, RangeActionActivationResult rangeActionActivationResult) {
        if (currentRangeAction instanceof PstRangeAction pstCurrentRangeAction && previousRangeAction instanceof PstRangeAction pstPreviousRangeAction) {
            OpenRaoMPConstraint tapRelTimeStepConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstCurrentRangeAction, state, LinearProblem.RaRangeShrinking.FALSE);

            List<TapRange> ranges = pstCurrentRangeAction.getRanges();
            List<TapRange> rangesRelativeTimeStep = ranges
                    .stream()
                    .filter(range -> range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)
                    .toList();

            for (TapRange range : rangesRelativeTimeStep) {
                List<Integer> minAndMaxRelativeTaps = getMinAndMaxTapsTimeStep(range);
                double minRelativeTap = minAndMaxRelativeTaps.get(0);
                double maxRelativeTap = minAndMaxRelativeTaps.get(1);

                double currentTimeStepTapOptimized = rangeActionActivationResult.getOptimizedTap(pstCurrentRangeAction, state);
                double previousTimeStepTapOptimized = rangeActionActivationResult.getOptimizedTap(pstPreviousRangeAction, state);

                double lbConstraintUpdate = minRelativeTap - currentTimeStepTapOptimized + previousTimeStepTapOptimized;
                double ubConstraintUpdate = maxRelativeTap - currentTimeStepTapOptimized + previousTimeStepTapOptimized;
                System.out.println(lbConstraintUpdate);
                System.out.println(ubConstraintUpdate);
                tapRelTimeStepConstraint.setBounds(lbConstraintUpdate,ubConstraintUpdate);
            }
        }
    }
}
