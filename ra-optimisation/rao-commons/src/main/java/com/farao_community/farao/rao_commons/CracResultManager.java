/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.ValidationException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracResultManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracResultManager.class);

    private final RaoData raoData;

    CracResultManager(RaoData raoData) {
        this.raoData = raoData;
    }

    /**
     * This method works from the working variant. It is filling CRAC result extension of the working variant
     * with values in network of the working variant.
     */
    public void fillRangeActionResultsWithNetworkValues() {
        Set<State> statesAfterOptimizedState = getStatesAfter(raoData.getOptimizedState());
        for (RangeAction rangeAction : raoData.getCrac().getRangeActions()) {
            double setPointValueInNetwork = rangeAction.getCurrentValue(raoData.getNetwork());
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(raoData.getWorkingVariantId());
            statesAfterOptimizedState.forEach(state -> rangeActionResult.setSetPoint(state.getId(), setPointValueInNetwork));
            if (rangeAction instanceof PstRangeAction) {
                PstRangeResult pstRangeResult = (PstRangeResult) rangeActionResult;
                int tapValueInNetwork = ((PstRangeAction) rangeAction).computeTapPosition(setPointValueInNetwork);
                statesAfterOptimizedState.forEach(state -> pstRangeResult.setTap(state.getId(), tapValueInNetwork));
            }
        }
    }

    /**
     * This method works from the working variant. It is applying on the network working variant
     * according to the values present in the CRAC result extension of the working variant.
     */
    public void applyRangeActionResultsOnNetwork() {
        for (RangeAction rangeAction : raoData.getAvailableRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            rangeAction.apply(raoData.getNetwork(),
                    rangeActionResultMap.getVariant(raoData.getWorkingVariantId()).getSetPoint(raoData.getOptimizedState().getId()));
        }
    }

    /**
     * This method compares CRAC result extension of two different variants. It compares the set point values
     * of all the range actions.
     *
     * @param variantId1: First variant to compare.
     * @param variantId2: Second variant to compare.
     * @return True if all the range actions are set at the same values and false otherwise.
     */
    public boolean sameRemedialActions(String variantId1, String variantId2) {
        for (RangeAction rangeAction : raoData.getAvailableRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            double value1 = rangeActionResultMap.getVariant(variantId1).getSetPoint(raoData.getOptimizedState().getId());
            double value2 = rangeActionResultMap.getVariant(variantId2).getSetPoint(raoData.getOptimizedState().getId());
            if (value1 != value2 && (!Double.isNaN(value1) || !Double.isNaN(value2))) {
                return false;
            }
        }
        return true;
    }

    public void fillRangeActionResultsWithLinearProblem(LinearProblem linearProblem) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Expected minimum margin: %.2f", linearProblem.getMinimumMarginVariable().solutionValue()));
            LOGGER.debug(String.format("Expected optimisation criterion: %.2f", linearProblem.getObjective().value()));
        }
        Set<State> statesAfterOptimizedState = getStatesAfter(raoData.getOptimizedState());

        Map<PstRangeAction, Integer> bestTaps = computeBestTaps(linearProblem);

        for (RangeAction rangeAction : raoData.getCrac().getRangeActions()) {
            if (rangeAction instanceof PstRangeAction) {
                RangeActionResultExtension pstRangeResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
                int approximatedPostOptimTap;
                double approximatedPostOptimAngle;
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();
                Optional<RangeAction> sameNetworkElementRangeAction = raoData.getAvailableRangeActions().stream()
                        .filter(ra -> ra.getNetworkElements().iterator().next().getId().equals(networkElementId) && linearProblem.getRangeActionSetPointVariable(ra) != null)
                        .findAny();
                if (sameNetworkElementRangeAction.isPresent()) {
                    PstRangeAction pstRangeAction = (PstRangeAction) sameNetworkElementRangeAction.get();
                    TwoWindingsTransformer transformer = raoData.getNetwork().getTwoWindingsTransformer(networkElementId);

                    approximatedPostOptimTap = bestTaps.get(pstRangeAction);
                    approximatedPostOptimAngle = transformer.getPhaseTapChanger().getStep(approximatedPostOptimTap).getAlpha();

                    LOGGER.debug("Range action {} has been set to tap {}", pstRangeAction.getName(), approximatedPostOptimTap);
                } else {
                    // For range actions that are not available in the perimeter or filtered out of optimization, copy their setpoint from the initial variant
                    approximatedPostOptimTap = ((PstRangeResult) pstRangeResultMap.getVariant(raoData.getPreOptimVariantId())).getTap(raoData.getOptimizedState().getId());
                    approximatedPostOptimAngle = pstRangeResultMap.getVariant(raoData.getPreOptimVariantId()).getSetPoint(raoData.getOptimizedState().getId());
                }

                PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
                statesAfterOptimizedState.forEach(state -> {
                    pstRangeResult.setSetPoint(state.getId(), approximatedPostOptimAngle);
                    pstRangeResult.setTap(state.getId(), approximatedPostOptimTap);
                });
            }
        }
    }

    /**
     * This function computes the best tap positions for PstRangeActions that were optimized in the linear problem.
     * It is a little smarter than just rounding the optimal angle to the closest tap position:
     * if the optimal angle is close to the limit between two tap positions, it will chose the one that maximizes the
     * minimum margin on the 10 most limiting elements (pre-optim)
     * Exception: if choosing the tap that is not the closest one to the optimal angle does not improve the margin
     * enough (current threshold of 10%), then the closest tap is kept
     *
     * @param linearProblem: the linear problem that was optimizes
     * @return a map containing the best tap position for every PstRangeAction that was optimized in the linear problem
     */
    Map<PstRangeAction, Integer> computeBestTaps(LinearProblem linearProblem) {
        List<BranchCnec> mostLimitingElements = RaoUtil.getMostLimitingElements(raoData.getCnecs(), raoData.getPreOptimVariantId(), MEGAWATT, raoData.getRaoParameters().getObjectiveFunction().relativePositiveMargins(), 10);

        Map<PstRangeAction, Integer> bestTaps = new HashMap<>();
        Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap = new HashMap<>();

        Set<PstRangeAction> pstRangeActions = raoData.getAvailableRangeActions().stream()
                .filter(ra -> ra instanceof PstRangeAction && linearProblem.getRangeActionSetPointVariable(ra) != null)
                .map(PstRangeAction.class::cast).collect(Collectors.toSet());
        for (PstRangeAction pstRangeAction : pstRangeActions) {
            double rangeActionVal = linearProblem.getRangeActionSetPointVariable(pstRangeAction).solutionValue();
            minMarginPerTap.put(pstRangeAction, computeMinMarginsForBestTaps(pstRangeAction, rangeActionVal, mostLimitingElements));
        }

        Map<String, Integer> bestTapPerPstGroup = computeBestTapPerPstGroup(minMarginPerTap);

        for (PstRangeAction pstRangeAction : pstRangeActions) {
            if (pstRangeAction.getGroupId().isPresent()) {
                bestTaps.put(pstRangeAction, bestTapPerPstGroup.get(pstRangeAction.getGroupId().get()));
            } else {
                int bestTap = minMarginPerTap.get(pstRangeAction).entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
                bestTaps.put(pstRangeAction, bestTap);
            }
        }
        return bestTaps;
    }

    /**
     * This function computes, for every group of PSTs, the common tap position that maximizes the minimum margin
     * @param minMarginPerTap: a map containing for each PstRangeAction, a map with tap positions and resulting minimum margin
     * @return a map containing for each group ID, the best common tap position for the PSTs
     */
    static Map<String, Integer> computeBestTapPerPstGroup(Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap) {
        Map<String, Integer> bestTapPerPstGroup = new HashMap<>();
        Set<PstRangeAction> pstRangeActions = minMarginPerTap.keySet();
        Set<String> pstGroups = pstRangeActions.stream().map(PstRangeAction::getGroupId).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        for (String pstGroup : pstGroups) {
            Set<PstRangeAction> pstsOfGroup = pstRangeActions.stream()
                    .filter(pstRangeAction -> pstRangeAction.getGroupId().isPresent() && pstRangeAction.getGroupId().get().equals(pstGroup))
                    .collect(Collectors.toSet());
            Map<Integer, Double> groupMinMarginPerTap = new HashMap<>();
            for (PstRangeAction pstRangeAction : pstsOfGroup) {
                Map<Integer, Double> pstMinMarginPerTap = minMarginPerTap.get(pstRangeAction);
                for (Map.Entry<Integer, Double> entry : pstMinMarginPerTap.entrySet()) {
                    int tap = entry.getKey();
                    if (groupMinMarginPerTap.containsKey(tap)) {
                        groupMinMarginPerTap.put(tap, Math.min(entry.getValue(), groupMinMarginPerTap.get(tap)));
                    } else {
                        groupMinMarginPerTap.put(tap, entry.getValue());
                    }
                }
            }
            int bestGroupTap = groupMinMarginPerTap.entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
            bestTapPerPstGroup.put(pstGroup, bestGroupTap);
        }
        return bestTapPerPstGroup;
    }

    /**
     * This function computes the best tap positions for an optimized PST range action, using the optimal angle
     * computed by the linear problem
     * It first chooses the closest tap position to the angle, then the second closest one, if the angle is close enough
     * (15% threshold) to the limit between two tap positions
     * It computes the minimum margin among the most limiting cnecs for both tap positions and returns them in a map
     * Exceptions:
     * - if the closest tap position is at a min or max limit, and the angle is close to the angle limit, then only
     *   the closest tap is returned. The margin is not computed but replaced with Double.MAX_VALUE
     * - if the angle is not close enough to the limit between two tap positions, only the closest tap is returned
     *   with a Double.MAX_VALUE margin
     * - if the second closest tap position does not improve the margin enough (10% threshold), then only the closest
     *   tap is returned with a Double.MAX_VALUE margin
     * @param pstRangeAction: the PstRangeAction for which we need the best taps and margins
     * @param angle: the optimal angle computed by the linear problem
     * @param mostLimitingCnecs: the cnecs upon which we compute the minimum margin
     * @return a map containing the minimum margin for each best tap position (one or two taps)
     */
    Map<Integer, Double> computeMinMarginsForBestTaps(PstRangeAction pstRangeAction, double angle, List<BranchCnec> mostLimitingCnecs) {
        int closestTap = pstRangeAction.computeTapPosition(angle);
        double closestAngle = pstRangeAction.convertTapToAngle(closestTap);

        Integer otherTap = null;

        // We don't have access to min and max tap positions directly
        // We have access to min and max angles, but angles and taps do not necessarily increase/decrese in the same direction
        // So we have to try/catch in order to know if we're at the tap limits
        boolean testTapPlus1 = true;
        try {
            pstRangeAction.convertTapToAngle(closestTap + 1);
        } catch (ValidationException e) {
            testTapPlus1 = false;
        }
        boolean testTapMinus1 = true;
        try {
            pstRangeAction.convertTapToAngle(closestTap - 1);
        } catch (ValidationException e) {
            testTapMinus1 = false;
        }

        if (testTapPlus1 && testTapMinus1) {
            // We can test tap+1 and tap-1
            double angleOfTapPlus1 = pstRangeAction.convertTapToAngle(closestTap + 1);
            otherTap = (Math.signum(angleOfTapPlus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) ? closestTap + 1 : closestTap - 1;
        } else if (testTapPlus1) {
            // We can only test tap+1, if the optimal angle is between the closest angle and the angle of tap+1
            double angleOfTapPlus1 = pstRangeAction.convertTapToAngle(closestTap + 1);
            if (Math.signum(angleOfTapPlus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) {
                otherTap = closestTap + 1;
            }
        } else if (testTapMinus1) {
            // We can only test tap-1, if the optimal angle is between the closest angle and the angle of tap-1
            double angleOfTapMinus1 = pstRangeAction.convertTapToAngle(closestTap - 1);
            if (Math.signum(angleOfTapMinus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) {
                otherTap = closestTap - 1;
            }
        }

        // Default case
        if (otherTap == null) {
            return Map.of(closestTap, Double.MAX_VALUE);
        }

        double otherAngle = pstRangeAction.convertTapToAngle(otherTap);
        double approxLimitAngle = 0.5 * (closestAngle + otherAngle);
        if (Math.abs(angle - approxLimitAngle) / Math.abs(closestAngle - otherAngle) < 0.15) {
            // Angle is too close to the limit between two tap positions
            // Chose the tap that maximizes the margin on the most limiting element
            Pair<Double, Double> margins = computeMinMargins(pstRangeAction, mostLimitingCnecs, closestAngle, otherAngle);
            // Exception: if choosing the tap that is not the closest one to the optimal angle does not improve the margin
            // enough (current threshold of 10%), then only the closest tap is kept
            // This is actually a workaround that mitigates adverse effects of this rounding on virtual costs
            // TODO : we can remove it when we use cost evaluators directly here
            if (margins.getRight() > margins.getLeft() + 0.1 * Math.abs(margins.getLeft())) {
                return Map.of(closestTap, margins.getLeft(), otherTap, margins.getRight());
            }
        }

        // Default case
        return Map.of(closestTap, Double.MAX_VALUE);
    }

    /**
     * This method estimates the minimum margin upon a given set of cnecs, for two angles of a given PST
     * @param pstRangeAction: the PstRangeAction that we should test on two angles
     * @param cnecs: the set of cnecs to compute the minimum margin
     * @param angle1: the first angle for the PST
     * @param angle2: the second angle for the PST
     * @return a pair of two minimum margins (margin for angle1, margin for angle2)
     */
    Pair<Double, Double> computeMinMargins(PstRangeAction pstRangeAction, List<BranchCnec> cnecs, double angle1, double angle2) {
        double minMargin1 = Double.MAX_VALUE;
        double minMargin2 = Double.MAX_VALUE;
        for (BranchCnec cnec : cnecs) {
            double sensitivity = raoData.getSensitivity(cnec, pstRangeAction);
            double currentSetPoint = pstRangeAction.getCurrentValue(raoData.getNetwork());
            double referenceFlow = raoData.getReferenceFlow(cnec);

            double flow1 = sensitivity * (angle1 - currentSetPoint) + referenceFlow;
            double flow2 = sensitivity * (angle2 - currentSetPoint) + referenceFlow;

            Optional<Double> minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            if (minFlow.isPresent()) {
                minMargin1 = Math.min(minMargin1, flow1 - minFlow.get());
                minMargin2 = Math.min(minMargin2, flow2 - minFlow.get());
            }
            Optional<Double> maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
            if (maxFlow.isPresent()) {
                minMargin1 = Math.min(minMargin1, maxFlow.get() - flow1);
                minMargin2 = Math.min(minMargin2, maxFlow.get() - flow2);
            }
        }
        return Pair.of(minMargin1, minMargin2);
    }

    /**
     * This method returns a set of State which are equal or after a given state.
     */
    private Set<State> getStatesAfter(State referenceState) {
        Optional<Contingency> referenceContingency = referenceState.getContingency();
        if (referenceContingency.isEmpty()) {
            return raoData.getCrac().getStates();
        } else {
            return raoData.getCrac().
                    getStates(referenceContingency.get()).stream().
                    filter(state -> state.getInstant().getSeconds() >= referenceState.getInstant().getSeconds())
                    .collect(Collectors.toSet());
        }
    }

    public void fillCracResultWithCosts(double functionalCost, double virtualCost) {
        raoData.getCracResult().setFunctionalCost(functionalCost);
        raoData.getCracResult().addVirtualCost(virtualCost);
        raoData.getCracResult().setNetworkSecurityStatus(functionalCost < 0 ?
                CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);
    }

    public void fillCnecResultWithFlows() {
        fillCnecResultWithFlows(raoData.getWorkingVariantId());
    }

    public void fillPreperimeterCnecResultWithFlows() {
        fillCnecResultWithFlows(raoData.getCrac().getExtension(ResultVariantManager.class).getPrePerimeterVariantId());
    }

    private void fillCnecResultWithFlows(String variantId) {
        raoData.getCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(variantId);
            cnecResult.setFlowInMW(raoData.getSystematicSensitivityResult().getReferenceFlow(cnec));
            cnecResult.setFlowInA(raoData.getSystematicSensitivityResult().getReferenceIntensity(cnec));
            cnecResult.setThresholds(cnec);
        });
    }

    public void fillCnecResultsWithLoopFlows(LoopFlowResult loopFlowResult) {
        raoData.getLoopflowCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))) {
                cnecResult.setLoopflowInMW(loopFlowResult.getLoopFlow(cnec));
                cnecResult.setLoopflowThresholdInMW(cnec.getExtension(CnecLoopFlowExtension.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT));
                cnecResult.setCommercialFlowInMW(loopFlowResult.getCommercialFlow(cnec));
            }
        });
    }

    public void fillCnecResultsWithApproximatedLoopFlows() {
        raoData.getLoopflowCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))) {
                double loopFLow = raoData.getSystematicSensitivityResult().getReferenceFlow(cnec) - cnecResult.getCommercialFlowInMW();
                cnecResult.setLoopflowInMW(loopFLow);
                cnecResult.setLoopflowThresholdInMW(cnec.getExtension(CnecLoopFlowExtension.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT));
            }
        });
    }

    public void copyCommercialFlowsBetweenVariants(String originVariant, String destinationVariant) {
        raoData.getLoopflowCnecs().forEach(cnec ->
                cnec.getExtension(CnecResultExtension.class).getVariant(destinationVariant)
                        .setCommercialFlowInMW(cnec.getExtension(CnecResultExtension.class).getVariant(originVariant).getCommercialFlowInMW())
        );
    }

    /**
     * This method copies absolute PTDF sums from a variant's CNEC result extension to another variant's
     *
     * @param originVariant:      the origin variant containing the PTDF sums
     * @param destinationVariant: the destination variant
     */
    public void copyAbsolutePtdfSumsBetweenVariants(String originVariant, String destinationVariant) {
        raoData.getCnecs().forEach(cnec ->
                cnec.getExtension(CnecResultExtension.class).getVariant(destinationVariant).setAbsolutePtdfSum(
                        cnec.getExtension(CnecResultExtension.class).getVariant(originVariant).getAbsolutePtdfSum()
                ));
    }
}
