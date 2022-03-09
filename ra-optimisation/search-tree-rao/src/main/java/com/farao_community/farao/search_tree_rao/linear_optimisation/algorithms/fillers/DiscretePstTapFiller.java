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
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DiscretePstTapFiller implements ProblemFiller {
    private final Network network;
    private final State optimizedState;
    private final Set<RangeAction<?>> rangeActions;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpoints;

    public DiscretePstTapFiller(Network network,
                                State optimizedState,
                                Map<State, Set<PstRangeAction>> rangeActions,
                                RangeActionSetpointResult prePerimeterRangeActionSetpoints) {
        this.network = network;
        this.optimizedState = optimizedState;
        this.rangeActions = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.rangeActions.addAll(rangeActions);
        this.prePerimeterRangeActionSetpoints = prePerimeterRangeActionSetpoints;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        rangeActions.stream().filter(PstRangeAction.class::isInstance).forEach(rangeAction ->
            buildPstTapVariablesAndConstraints(linearProblem, (PstRangeAction) rangeAction)
        );
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        rangeActions.stream().filter(PstRangeAction.class::isInstance).forEach(rangeAction ->
            refineTapToAngleConversionCoefficientAndUpdateBounds(linearProblem, (PstRangeAction) rangeAction, rangeActionActivationResult)
        );
    }

    private void buildPstTapVariablesAndConstraints(LinearProblem linearProblem, PstRangeAction pstRangeAction) {

        // compute a few values on PST taps and angle
        double prePerimeterAngle = prePerimeterRangeActionSetpoints.getSetpoint(pstRangeAction);
        double currentAngle = pstRangeAction.getCurrentSetpoint(network);
        int currentTap = pstRangeAction.getCurrentTapPosition(network);

        int minAdmissibleTap = Math.min(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));
        int maxAdmissibleTap = Math.max(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));

        int maxDownwardTapVariation = Math.max(0, currentTap - minAdmissibleTap);
        int maxUpwardTapVariation = Math.max(0, maxAdmissibleTap - currentTap);

        // create and get variables
        MPVariable pstTapDownwardVariationVariable = linearProblem.addPstTapVariationVariable(0, (double) maxDownwardTapVariation + maxUpwardTapVariation, pstRangeAction, LinearProblem.VariationDirectionExtension.DOWNWARD);
        MPVariable pstTapUpwardVariationVariable = linearProblem.addPstTapVariationVariable(0, (double) maxDownwardTapVariation + maxUpwardTapVariation, pstRangeAction, LinearProblem.VariationDirectionExtension.UPWARD);

        MPVariable pstTapDownwardVariationBinary = linearProblem.addPstTapVariationBinary(pstRangeAction, LinearProblem.VariationDirectionExtension.DOWNWARD);
        MPVariable pstTapUpwardVariationBinary = linearProblem.addPstTapVariationBinary(pstRangeAction, LinearProblem.VariationDirectionExtension.UPWARD);

        MPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction);

        if (setPointVariable == null) {
            throw new FaraoException(format("PST Range action variable for %s has not been defined yet.", pstRangeAction.getId()));
        }

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
        MPConstraint tapToAngleConversionConstraint = linearProblem.addTapToAngleConversionConstraint(currentAngle, currentAngle, pstRangeAction);
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
        MPConstraint upOrDownConstraint = linearProblem.addUpOrDownPstVariationConstraint(pstRangeAction);
        upOrDownConstraint.setCoefficient(pstTapDownwardVariationBinary, 1);
        upOrDownConstraint.setCoefficient(pstTapUpwardVariationBinary, 1);
        upOrDownConstraint.setUb(1);

        // variation can be made in one direction, only if it is authorized by the binary variable
        MPConstraint downAuthorizationConstraint = linearProblem.addIsVariationInDirectionConstraint(-LinearProblem.infinity(), 0, pstRangeAction, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.DOWNWARD);
        downAuthorizationConstraint.setCoefficient(pstTapDownwardVariationVariable, 1);
        downAuthorizationConstraint.setCoefficient(pstTapDownwardVariationBinary, -maxDownwardTapVariation);

        MPConstraint upAuthorizationConstraint = linearProblem.addIsVariationInDirectionConstraint(-LinearProblem.infinity(), 0, pstRangeAction, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.UPWARD);
        upAuthorizationConstraint.setCoefficient(pstTapUpwardVariationVariable, 1);
        upAuthorizationConstraint.setCoefficient(pstTapUpwardVariationBinary, -maxUpwardTapVariation);
    }

    private void refineTapToAngleConversionCoefficientAndUpdateBounds(LinearProblem linearProblem, PstRangeAction pstRangeAction, RangeActionActivationResult rangeActionActivationResult) {

        // compute a few values on PST taps and angle
        double newAngle = rangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, optimizedState);
        int newTapPosition = rangeActionActivationResult.getOptimizedTap(pstRangeAction, optimizedState);

        double prePerimeterAngle = prePerimeterRangeActionSetpoints.getTap(pstRangeAction);
        int minAdmissibleTap = Math.min(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));
        int maxAdmissibleTap = Math.max(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));

        int maxDownwardTapVariation = Math.max(0, newTapPosition - minAdmissibleTap);
        int maxUpwardTapVariation = Math.max(0, maxAdmissibleTap - newTapPosition);

        Map<Integer, Double> tapToAngleConversionMap = pstRangeAction.getTapToAngleConversionMap();

        // get variables and constraints
        MPConstraint tapToAngleConversionConstraint = linearProblem.getTapToAngleConversionConstraint(pstRangeAction);
        MPVariable pstTapUpwardVariationVariable = linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationDirectionExtension.UPWARD);
        MPVariable pstTapDownwardVariationVariable = linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationDirectionExtension.DOWNWARD);
        MPConstraint downAuthorizationConstraint = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.DOWNWARD);
        MPConstraint upAuthorizationConstraint = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.UPWARD);
        MPVariable pstTapDownwardVariationBinary = linearProblem.getPstTapVariationBinary(pstRangeAction, LinearProblem.VariationDirectionExtension.DOWNWARD);
        MPVariable pstTapUpwardVariationBinary = linearProblem.getPstTapVariationBinary(pstRangeAction, LinearProblem.VariationDirectionExtension.UPWARD);

        if (tapToAngleConversionConstraint == null || pstTapUpwardVariationVariable == null || pstTapDownwardVariationVariable == null
                || downAuthorizationConstraint == null || upAuthorizationConstraint == null || pstTapDownwardVariationBinary == null
                || pstTapUpwardVariationBinary == null) {
            throw new FaraoException(format("PST Range action tap variables and/or constraints for %s has not been defined yet.", pstRangeAction.getId()));
        }

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
