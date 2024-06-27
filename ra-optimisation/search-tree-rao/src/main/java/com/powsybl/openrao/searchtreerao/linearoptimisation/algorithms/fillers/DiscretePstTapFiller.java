/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.iidm.network.Network;

import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DiscretePstTapFiller implements ProblemFiller {

    private final Network network;
    private final State optimizedState;
    private final Map<State, Set<PstRangeAction>> rangeActions;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpoints;

    public DiscretePstTapFiller(Network network,
                                State optimizedState,
                                Map<State, Set<PstRangeAction>> rangeActions,
                                RangeActionSetpointResult prePerimeterRangeActionSetpoints) {
        this.network = network;
        this.optimizedState = optimizedState;
        this.rangeActions = rangeActions;
        this.prePerimeterRangeActionSetpoints = prePerimeterRangeActionSetpoints;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        rangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            buildPstTapVariablesAndConstraints(linearProblem, rangeAction, state)
        ));
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        rangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            refineTapToAngleConversionCoefficientAndUpdateBounds(linearProblem, rangeAction, rangeActionActivationResult, state)
        ));
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        rangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            refineTapToAngleConversionCoefficientAndUpdateBounds(linearProblem, rangeAction, rangeActionActivationResult, state)
        ));
    }

    private void buildPstTapVariablesAndConstraints(LinearProblem linearProblem, PstRangeAction pstRangeAction, State state) {

        // compute a few values on PST taps and angle
        double prePerimeterAngle = prePerimeterRangeActionSetpoints.getSetpoint(pstRangeAction);
        double currentAngle = pstRangeAction.getCurrentSetpoint(network);
        int currentTap = pstRangeAction.getCurrentTapPosition(network);

        int minAdmissibleTap = Math.min(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));
        int maxAdmissibleTap = Math.max(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));

        int maxDownwardTapVariation = Math.max(0, currentTap - minAdmissibleTap);
        int maxUpwardTapVariation = Math.max(0, maxAdmissibleTap - currentTap);

        // create and get variables
        OpenRaoMPVariable pstTapDownwardVariationVariable = linearProblem.addPstTapVariationVariable(0, (double) maxDownwardTapVariation + maxUpwardTapVariation, pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapUpwardVariationVariable = linearProblem.addPstTapVariationVariable(0, (double) maxDownwardTapVariation + maxUpwardTapVariation, pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);

        OpenRaoMPVariable pstTapDownwardVariationBinary = linearProblem.addPstTapVariationBinary(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapUpwardVariationBinary = linearProblem.addPstTapVariationBinary(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);

        // create constraints
        // tap to angle conversion constraint

        // note : the conversion "angleToTap" is not theoretically strictly linear
        // however, to fit in the linear problem without adding too much binary variables, two "angleToTap" conversion
        // factors are defined, which approximates how taps should be converted in setpoints:
        //      - when we increase the tap of the PST
        //      - when we decrease it
        //
        // in the first MIP, we calibrate the 'constant tap to angle factor' with the extremities of the PST
        // when updating the MIP, the factors will be calibrated on a change of one tap (see update() method)
        OpenRaoMPConstraint tapToAngleConversionConstraint = linearProblem.addTapToAngleConversionConstraint(currentAngle, currentAngle, pstRangeAction, state);
        tapToAngleConversionConstraint.setCoefficient(setPointVariable, 1);

        if (maxDownwardTapVariation > 0) {
            double angleToTapDownwardConversionFactor = (pstRangeAction.getTapToAngleConversionMap().get(currentTap) - pstRangeAction.getTapToAngleConversionMap().get(minAdmissibleTap)) / maxDownwardTapVariation;
            tapToAngleConversionConstraint.setCoefficient(pstTapDownwardVariationVariable, angleToTapDownwardConversionFactor);
        }
        if (maxUpwardTapVariation > 0) {
            double angleToTapUpwardConversionFactor = (pstRangeAction.getTapToAngleConversionMap().get(maxAdmissibleTap) - pstRangeAction.getTapToAngleConversionMap().get(currentTap)) / maxUpwardTapVariation;
            tapToAngleConversionConstraint.setCoefficient(pstTapUpwardVariationVariable, -angleToTapUpwardConversionFactor);
        }

        // variation can only be upward or downward
        OpenRaoMPConstraint upOrDownConstraint = linearProblem.addUpOrDownPstVariationConstraint(pstRangeAction, state);
        upOrDownConstraint.setCoefficient(pstTapDownwardVariationBinary, 1);
        upOrDownConstraint.setCoefficient(pstTapUpwardVariationBinary, 1);
        upOrDownConstraint.setUb(1);

        // variation can be made in one direction, only if it is authorized by the binary variable
        OpenRaoMPConstraint downAuthorizationConstraint = linearProblem.addIsVariationInDirectionConstraint(-LinearProblem.infinity(), 0, pstRangeAction, state, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.DOWNWARD);
        downAuthorizationConstraint.setCoefficient(pstTapDownwardVariationVariable, 1);
        downAuthorizationConstraint.setCoefficient(pstTapDownwardVariationBinary, -maxDownwardTapVariation);

        OpenRaoMPConstraint upAuthorizationConstraint = linearProblem.addIsVariationInDirectionConstraint(-LinearProblem.infinity(), 0, pstRangeAction, state, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.UPWARD);
        upAuthorizationConstraint.setCoefficient(pstTapUpwardVariationVariable, 1);
        upAuthorizationConstraint.setCoefficient(pstTapUpwardVariationBinary, -maxUpwardTapVariation);
    }

    private void refineTapToAngleConversionCoefficientAndUpdateBounds(LinearProblem linearProblem, PstRangeAction pstRangeAction, RangeActionActivationResult rangeActionActivationResult, State state) {

        // compute a few values on PST taps and angle
        double newAngle = rangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, state);
        int newTapPosition = rangeActionActivationResult.getOptimizedTap(pstRangeAction, state);

        double prePerimeterAngle = prePerimeterRangeActionSetpoints.getSetpoint(pstRangeAction);
        int minAdmissibleTap = Math.min(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));
        int maxAdmissibleTap = Math.max(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));

        int maxDownwardTapVariation = Math.max(0, newTapPosition - minAdmissibleTap);
        int maxUpwardTapVariation = Math.max(0, maxAdmissibleTap - newTapPosition);

        Map<Integer, Double> tapToAngleConversionMap = pstRangeAction.getTapToAngleConversionMap();

        // get variables and constraints
        OpenRaoMPConstraint tapToAngleConversionConstraint = linearProblem.getTapToAngleConversionConstraint(pstRangeAction, state);
        OpenRaoMPVariable pstTapUpwardVariationVariable = linearProblem.getPstTapVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable pstTapDownwardVariationVariable = linearProblem.getPstTapVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPConstraint downAuthorizationConstraint = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, state, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPConstraint upAuthorizationConstraint = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, state, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable pstTapDownwardVariationBinary = linearProblem.getPstTapVariationBinary(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapUpwardVariationBinary = linearProblem.getPstTapVariationBinary(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);

        // update second member of the tap-to-angle conversion constraint with the new current angle
        tapToAngleConversionConstraint.setUb(newAngle);
        tapToAngleConversionConstraint.setLb(newAngle);

        // update coefficients of the constraint with newly calculated ones, except if the tap is already at the limit of the PST range
        // when updating the MIP, the factors are calibrated on a change of one tap
        if (tapToAngleConversionMap.containsKey(newTapPosition + 1)) {
            double angleToTapUpwardConversionFactor = tapToAngleConversionMap.get(newTapPosition + 1) - tapToAngleConversionMap.get(newTapPosition);
            tapToAngleConversionConstraint.setCoefficient(pstTapUpwardVariationVariable, -angleToTapUpwardConversionFactor);
        }

        if (tapToAngleConversionMap.containsKey(newTapPosition - 1)) {
            double angleToTapDownwardConversionFactor = tapToAngleConversionMap.get(newTapPosition) - tapToAngleConversionMap.get(newTapPosition - 1);
            tapToAngleConversionConstraint.setCoefficient(pstTapDownwardVariationVariable, angleToTapDownwardConversionFactor);
        }

        // update the coefficient on the min/max tap variations of the isVariationInDirectionConstraints
        downAuthorizationConstraint.setCoefficient(pstTapDownwardVariationBinary, -maxDownwardTapVariation);
        upAuthorizationConstraint.setCoefficient(pstTapUpwardVariationBinary, -maxUpwardTapVariation);
    }
}
