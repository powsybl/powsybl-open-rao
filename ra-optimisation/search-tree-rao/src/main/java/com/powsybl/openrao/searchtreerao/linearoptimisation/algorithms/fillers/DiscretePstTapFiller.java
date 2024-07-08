/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.TapRange;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.searchtreerao.result.impl.MultiStateRemedialActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DiscretePstTapFiller implements ProblemFiller {

    private final Network network;
    private final OptimizationPerimeter optimizationPerimeter;
    private final Map<State, Set<PstRangeAction>> rangeActions;
    private final RangeActionResult prePerimeterRangeActionSetpoints;

    public DiscretePstTapFiller(Network network,
                                OptimizationPerimeter optimizationPerimeter,
                                Map<State, Set<PstRangeAction>> rangeActions,
                                RangeActionResult prePerimeterRangeActionSetpoints) {
        this.network = network;
        this.optimizationPerimeter = optimizationPerimeter;
        this.rangeActions = rangeActions;
        this.prePerimeterRangeActionSetpoints = prePerimeterRangeActionSetpoints;
    }

    @Override
    public void fill(LinearProblem linearProblem, PerimeterResultWithCnecs flowAndSensiResult) {
        rangeActions.entrySet().stream()
            .sorted(Comparator.comparingInt(e -> e.getKey().getInstant().getOrder())).forEach(entry -> entry.getValue().forEach(rangeAction ->
                buildPstTapVariablesAndConstraints(linearProblem, rangeAction, entry.getKey())));
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, PerimeterResultWithCnecs flowAndSensiResult, MultiStateRemedialActionResultImpl rangeActionResult) {
        rangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            refineTapToAngleConversionCoefficientAndUpdateBounds(linearProblem, rangeAction, rangeActionResult, state)
        ));
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, MultiStateRemedialActionResultImpl rangeActionResult) {
        rangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            refineTapToAngleConversionCoefficientAndUpdateBounds(linearProblem, rangeAction, rangeActionResult, state)
        ));
    }

    private void buildPstTapVariablesAndConstraints(LinearProblem linearProblem, PstRangeAction pstRangeAction, State state) {

        // compute a few values on PST taps and angle
        double prePerimeterAngle = prePerimeterRangeActionSetpoints.getOptimizedSetpoint(pstRangeAction);
        double currentAngle = pstRangeAction.getCurrentSetpoint(network);
        int currentTap = pstRangeAction.getCurrentTapPosition(network);

        Pair<RangeAction<?>, State> lastAvailableRangeAction = RaoUtil.getLastAvailableRangeActionOnSameNetworkElement(optimizationPerimeter, pstRangeAction, state);
        Pair<Integer, Integer> admissibleTaps = getMinAndMaxAdmissibleTaps(pstRangeAction, lastAvailableRangeAction);
        int minAdmissibleTap = admissibleTaps.getLeft();
        int maxAdmissibleTap = admissibleTaps.getRight();

        int maxDownwardTapVariation = Math.max(0, currentTap - minAdmissibleTap);
        int maxUpwardTapVariation = Math.max(0, maxAdmissibleTap - currentTap);

        // create and get variables
        OpenRaoMPVariable pstTapDownwardVariationVariable = linearProblem.addPstTapVariationVariable(0, (double) maxDownwardTapVariation + maxUpwardTapVariation, pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapUpwardVariationVariable = linearProblem.addPstTapVariationVariable(0, (double) maxDownwardTapVariation + maxUpwardTapVariation, pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);

        OpenRaoMPVariable pstTapDownwardVariationBinary = linearProblem.addPstTapVariationBinary(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapUpwardVariationBinary = linearProblem.addPstTapVariationBinary(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);

        // build integer constraint as it wasn't built in CoreProblemFiller
        if (lastAvailableRangeAction != null) {
            RangeAction<?> preventiveRangeAction = lastAvailableRangeAction.getKey();
            Pair<Integer, Integer> pstLimits = getMinAndMaxRelativeTaps(pstRangeAction);
            int maxRelativeTap = pstLimits.getRight();
            int minRelativeTap = pstLimits.getLeft();
            OpenRaoMPConstraint relativeTapConstraint = linearProblem.addPstRelativeTapConstraint(minRelativeTap, maxRelativeTap, pstRangeAction, state);
            OpenRaoMPVariable preventivePstTapUpwardVariationVariable = linearProblem.getPstTapVariationVariable((PstRangeAction) preventiveRangeAction, optimizationPerimeter.getMainOptimizationState(), LinearProblem.VariationDirectionExtension.UPWARD);
            OpenRaoMPVariable preventivePstTapDownwardVariationVariable = linearProblem.getPstTapVariationVariable((PstRangeAction) preventiveRangeAction, optimizationPerimeter.getMainOptimizationState(), LinearProblem.VariationDirectionExtension.DOWNWARD);
            relativeTapConstraint.setCoefficient(pstTapUpwardVariationVariable, 1);
            relativeTapConstraint.setCoefficient(pstTapDownwardVariationVariable, 1);
            relativeTapConstraint.setCoefficient(preventivePstTapUpwardVariationVariable, -1);
            relativeTapConstraint.setCoefficient(preventivePstTapDownwardVariationVariable, -1);
        }

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

    private void refineTapToAngleConversionCoefficientAndUpdateBounds(LinearProblem linearProblem, PstRangeAction pstRangeAction, MultiStateRemedialActionResultImpl rangeActionResult, State state) {

        // compute a few values on PST taps and angle
        double newAngle = rangeActionResult.getOptimizedSetpointOnState(pstRangeAction, state);
        int newTapPosition = rangeActionResult.getOptimizedTapOnState(pstRangeAction, state);

        double prePerimeterAngle = prePerimeterRangeActionSetpoints.getOptimizedSetpoint(pstRangeAction);
        int minAdmissibleTap = Math.min(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));
        int maxAdmissibleTap = Math.max(pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle)), pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle)));

        int maxDownwardTapVariation = Math.max(0, newTapPosition - minAdmissibleTap);
        int maxUpwardTapVariation = Math.max(0, maxAdmissibleTap - newTapPosition);

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
        Map<Integer, Double> tapToAngleConversionMap = pstRangeAction.getTapToAngleConversionMap();
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

    /**
     * Returns min and max admissible taps for a given PST in a given state.
     * In the nominal case, it computes these values with the PST ranges and its pre-perimeter setpoint.
     * However, in Second Preventive with Global Optimization, we can optimize a PST in both preventive and curative.
     * If so, we can't predict the curative limits as they depend on the preventive ones.
     * In such a case, we return the network limits.
     */
    private Pair<Integer, Integer> getMinAndMaxAdmissibleTaps(PstRangeAction pstRangeAction, Pair<RangeAction<?>, State> lastAvailableRangeAction) {
        double prePerimeterAngle = prePerimeterRangeActionSetpoints.getOptimizedSetpoint(pstRangeAction);
        int minTap = pstRangeAction.convertAngleToTap(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterAngle));
        int maxTap = pstRangeAction.convertAngleToTap(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterAngle));
        int minAdmissibleTap = Math.min(maxTap, minTap);
        int maxAdmissibleTap = Math.max(maxTap, minTap);

        if (lastAvailableRangeAction != null) {
            Set<Integer> pstTapsSet = pstRangeAction.getTapToAngleConversionMap().keySet();
            IntSummaryStatistics tapStats = pstTapsSet.stream().mapToInt(k -> k).summaryStatistics();
            minAdmissibleTap = tapStats.getMin();
            maxAdmissibleTap = tapStats.getMax();
        }
        return Pair.of(minAdmissibleTap, maxAdmissibleTap);
    }

    private Pair<Integer, Integer> getMinAndMaxRelativeTaps(PstRangeAction pstRangeAction) {
        int minRelativeTap = -LinearProblem.infinity();
        int maxRelativeTap = LinearProblem.infinity();
        List<TapRange> ranges = pstRangeAction.getRanges();
        for (TapRange range : ranges) {
            if (range.getRangeType().equals(RangeType.RELATIVE_TO_PREVIOUS_INSTANT)) {
                minRelativeTap = Math.max(minRelativeTap, range.getMinTap());
                maxRelativeTap = Math.min(maxRelativeTap, range.getMaxTap());
            }
        }
        minRelativeTap = Math.min(0, minRelativeTap);
        maxRelativeTap = Math.max(0, maxRelativeTap);
        return Pair.of(minRelativeTap, maxRelativeTap);
    }
}
