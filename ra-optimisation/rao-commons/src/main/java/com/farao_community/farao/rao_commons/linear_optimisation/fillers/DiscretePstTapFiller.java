/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;

import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem.VariationExtension.DOWNWARD;
import static com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem.VariationExtension.UPWARD;
import static java.lang.String.format;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DiscretePstTapFiller implements ProblemFiller {
    private final Network network;
    private final Set<RangeAction> rangeActions;
    private final RangeActionResult prePerimeterRangeActionResult;

    public DiscretePstTapFiller(Network network,
                                Set<RangeAction> rangeActions,
                                RangeActionResult prePerimeterRangeActionResult) {
        this.network = network;
        this.rangeActions = rangeActions;
        this.prePerimeterRangeActionResult = prePerimeterRangeActionResult;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        // add variables and constraints
        rangeActions.forEach(rangeAction -> {
            if (rangeAction instanceof PstRangeAction) {
                buildPstTapVariablesAndConstraints(linearProblem, (PstRangeAction) rangeAction);
            }
        });
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        //refine PST tap to angle conversion coefficients
        rangeActions.forEach(rangeAction -> refineTapToAngleConversionCoefficient(linearProblem, (PstRangeAction) rangeAction, rangeActionResult));
    }

    //todo: some javadoc
    private void buildPstTapVariablesAndConstraints(LinearProblem linearProblem, PstRangeAction pstRangeAction) {

        // compute a few values on PST taps and angle
        double prePerimeterAngle = prePerimeterRangeActionResult.getOptimizedSetPoint(pstRangeAction);
        double currentAngle = pstRangeAction.getCurrentSetpoint(network);

        int minAdmissibleTap = Math.min(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));
        int maxAdmissibleTap = Math.max(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));
        int currentTap = pstRangeAction.getCurrentTapPosition(network);

        int maxDownwardTapVariation  = Math.max(0, currentTap - minAdmissibleTap);
        int maxUpwardTapVariation = Math.max(0, maxAdmissibleTap - currentTap);

        // create and get variables
        // bounds should be at least equals to 2, otherwise integer variables are somehow automatically converted
        // to binary by or-tools, which can cause errors in the update() methods
        MPVariable pstTapDownwardVariationVariable = linearProblem.addPstTapVariationVariable(0, maxDownwardTapVariation + maxUpwardTapVariation, pstRangeAction, DOWNWARD);
        MPVariable pstTapUpwardVariationVariable = linearProblem.addPstTapVariationVariable(0, maxDownwardTapVariation + maxUpwardTapVariation, pstRangeAction, UPWARD);

        MPVariable pstTapDownwardVariationBinary = linearProblem.addPstTapVariationBinary(pstRangeAction, DOWNWARD);
        MPVariable pstTapUpwardVariationBinary = linearProblem.addPstTapVariationBinary(pstRangeAction, UPWARD);

        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(pstRangeAction);

        if (setPointVariable == null) {
            throw new FaraoException(format("PST Range action variable for %s has not been defined yet.", pstRangeAction.getId()));
        }

        // note : the conversion "angleToTap" is not theoretically strictly linear
        // however, to fit in the linear problem without adding too much binary variables, two "angleToTap" conversion
        // factors are defined, which approximates how taps should be converted in setpoints:
        //      - when we increase the tap of the PST
        //      - when we decrease it
        //
        // in the first MIP, we calibrate the 'constant tap to angle factor' with the extremities of the PST
        // when updating the MIP, the factors will be calibrated on a change of one tap (see update() method)
        double angleToTapDownwardConversionFactor = (pstRangeAction.getTapToAngleConversionMap().get(currentTap) - pstRangeAction.getTapToAngleConversionMap().get(minAdmissibleTap)) / Math.max(1, maxDownwardTapVariation);
        double angleToTapUpwardConversionFactor = (pstRangeAction.getTapToAngleConversionMap().get(maxAdmissibleTap) - pstRangeAction.getTapToAngleConversionMap().get(currentTap)) / Math.max(1, maxUpwardTapVariation);

        // create constraint
        // tap to angle conversion constraint
        MPConstraint tapToAngleConversionConstraint = linearProblem.addTapToAngleConversionConstraint(currentAngle, currentAngle, pstRangeAction);
        tapToAngleConversionConstraint.setCoefficient(setPointVariable, 1);
        tapToAngleConversionConstraint.setCoefficient(pstTapUpwardVariationVariable, -angleToTapUpwardConversionFactor);
        tapToAngleConversionConstraint.setCoefficient(pstTapDownwardVariationVariable, angleToTapDownwardConversionFactor);

        // variation can only be upward or downward
        MPConstraint upOrDownConstraint = linearProblem.addUpOrDownPstVariationConstraint(pstRangeAction);
        upOrDownConstraint.setCoefficient(pstTapDownwardVariationBinary, 1);
        upOrDownConstraint.setCoefficient(pstTapUpwardVariationBinary, 1);
        upOrDownConstraint.setUb(1);

        // variation can be made in one direction, only if it is authorized by the binary variable
        MPConstraint downAuthorizationConstraint = linearProblem.addIsVariationInDirectionConstraint(pstRangeAction, DOWNWARD);
        MPConstraint upAuthorizationConstraint = linearProblem.addIsVariationInDirectionConstraint(pstRangeAction, UPWARD);
        downAuthorizationConstraint.setCoefficient(pstTapDownwardVariationVariable, 1);
        downAuthorizationConstraint.setCoefficient(pstTapDownwardVariationBinary, -maxDownwardTapVariation);
        downAuthorizationConstraint.setUb(0);

        upAuthorizationConstraint.setCoefficient(pstTapUpwardVariationVariable, 1);
        upAuthorizationConstraint.setCoefficient(pstTapUpwardVariationBinary, -maxUpwardTapVariation);
        upAuthorizationConstraint.setUb(0);
    }

    //todo: javadoc
    private void refineTapToAngleConversionCoefficient(LinearProblem linearProblem, PstRangeAction pstRangeAction, RangeActionResult rangeActionResult) {

        // when updating the MIP, the factors are calibrated on a change of one tap
        MPConstraint tapToAngleConversionConstraint = linearProblem.getTapToAngleConversionConstraint(pstRangeAction);
        MPVariable pstTapUpwardVariationVariable = linearProblem.getPstTapVariationVariable(pstRangeAction, UPWARD);
        MPVariable pstTapDownwardVariationVariable = linearProblem.getPstTapVariationVariable(pstRangeAction, DOWNWARD);

        if (tapToAngleConversionConstraint == null || pstTapUpwardVariationVariable == null || pstTapDownwardVariationVariable == null) {
            throw new FaraoException(format("PST Range action tap variables and/or constraints for %s has not been defined yet.", pstRangeAction.getId()));
        }

        // delete and recreate variables
        // (done to avoid a strange bug, in which integer variables bounded by [0,1] are automatically converted into binaries)

        double newAngle = rangeActionResult.getOptimizedSetPoint(pstRangeAction);
        int newTapPosition = rangeActionResult.getOptimizedTap(pstRangeAction);

        double prePerimeterAngle = prePerimeterRangeActionResult.getOptimizedSetPoint(pstRangeAction);
        int minAdmissibleTap = Math.min(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));
        int maxAdmissibleTap = Math.max(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));

        int maxDownwardTapVariation  = Math.max(0, newTapPosition - minAdmissibleTap);
        int maxUpwardTapVariation = Math.max(0, maxAdmissibleTap - newTapPosition);

        // update second member of the constraint with the new current angle
        tapToAngleConversionConstraint.setUb(newAngle);
        tapToAngleConversionConstraint.setLb(newAngle);

        Map<Integer, Double> tapToAngleConversionMap = pstRangeAction.getTapToAngleConversionMap();

        // update coefficients of the constraint with newly calculated ones, except if the tap is already at the limit of the PST range
        if (tapToAngleConversionMap.containsKey(newTapPosition + 1)) {
            double angleToTapUpwardConversionFactor = tapToAngleConversionMap.get(newTapPosition + 1) - tapToAngleConversionMap.get(newTapPosition);
            tapToAngleConversionConstraint.setCoefficient(pstTapUpwardVariationVariable, -angleToTapUpwardConversionFactor);
        }

        if (tapToAngleConversionMap.containsKey(newTapPosition - 1)) {
            double angleToTapDownwardConversionFactor = tapToAngleConversionMap.get(newTapPosition) - tapToAngleConversionMap.get(newTapPosition - 1);
            tapToAngleConversionConstraint.setCoefficient(pstTapDownwardVariationVariable, angleToTapDownwardConversionFactor);
        }

        // update the coefficient on the min/max tap variations of the isVariationInDirectionConstraints
        MPConstraint downAuthorizationConstraint = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, DOWNWARD);
        MPConstraint upAuthorizationConstraint = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, UPWARD);
        MPVariable pstTapDownwardVariationBinary = linearProblem.getPstTapVariationBinary(pstRangeAction, DOWNWARD);
        MPVariable pstTapUpwardVariationBinary = linearProblem.getPstTapVariationBinary(pstRangeAction, UPWARD);
        downAuthorizationConstraint.setCoefficient(pstTapDownwardVariationBinary, -maxDownwardTapVariation);
        upAuthorizationConstraint.setCoefficient(pstTapUpwardVariationBinary, -maxUpwardTapVariation);

    }
}
