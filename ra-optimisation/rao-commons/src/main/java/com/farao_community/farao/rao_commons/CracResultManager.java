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

        Map<PstRangeAction, Integer> bestTaps = getBestTaps(linearProblem);

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
                    double rangeActionVal = linearProblem.getRangeActionSetPointVariable(sameNetworkElementRangeAction.get()).solutionValue();
                    PstRangeAction pstRangeAction = (PstRangeAction) sameNetworkElementRangeAction.get();
                    TwoWindingsTransformer transformer = raoData.getNetwork().getTwoWindingsTransformer(networkElementId);

                    //approximatedPostOptimTap = pstRangeAction.computeTapPosition(rangeActionVal);
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

    private Map<PstRangeAction, Integer> getBestTaps(LinearProblem linearProblem) {
        List<BranchCnec> mostLimitingElements = RaoUtil.getMostLimitingElements(raoData.getCnecs(), raoData.getPreOptimVariantId(), MEGAWATT, raoData.getRaoParameters().getObjectiveFunction().relativePositiveMargins(), 10);

        Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap = new HashMap<>();

        for (RangeAction rangeAction : raoData.getCrac().getRangeActions()) {
            if (rangeAction instanceof PstRangeAction) {
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();
                Optional<RangeAction> sameNetworkElementRangeAction = raoData.getAvailableRangeActions().stream()
                        .filter(ra -> ra.getNetworkElements().iterator().next().getId().equals(networkElementId) && linearProblem.getRangeActionSetPointVariable(ra) != null)
                        .findAny();
                if (sameNetworkElementRangeAction.isPresent()) {
                    double rangeActionVal = linearProblem.getRangeActionSetPointVariable(sameNetworkElementRangeAction.get()).solutionValue();
                    PstRangeAction pstRangeAction = (PstRangeAction) sameNetworkElementRangeAction.get();
                    minMarginPerTap.put(pstRangeAction, getMinMarginPerTap(pstRangeAction, rangeActionVal, mostLimitingElements));
                }
            }
        }

        Map<PstRangeAction, Integer> bestTaps = new HashMap<>();
        Set<String> pstGroups = minMarginPerTap.keySet().stream().map(PstRangeAction::getGroupId)
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        for (String pstGroup : pstGroups) {
            Set<PstRangeAction> pstsOfGroup = minMarginPerTap.keySet().stream()
                    .filter(pstRangeAction -> pstRangeAction.getGroupId().isPresent() && pstRangeAction.getGroupId().get().equals(pstGroup))
                    .collect(Collectors.toSet());
            Map<Integer, Double> groupMinMarginPerTap = new HashMap<>();
            for (PstRangeAction pstRangeAction : pstsOfGroup) {
                Map<Integer, Double> pstMinMarginPerTap = minMarginPerTap.get(pstRangeAction);
                for (Integer tap : pstMinMarginPerTap.keySet()) {
                    if (groupMinMarginPerTap.containsKey(tap)) {
                        groupMinMarginPerTap.put(tap, Math.min(pstMinMarginPerTap.get(tap), groupMinMarginPerTap.get(tap)));
                    } else {
                        groupMinMarginPerTap.put(tap, pstMinMarginPerTap.get(tap));
                    }
                }
            }
            int bestGroupTap = groupMinMarginPerTap.entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
            for (PstRangeAction pstRangeAction : pstsOfGroup) {
                bestTaps.put(pstRangeAction, bestGroupTap);
            }
        }
        for (PstRangeAction pstRangeAction : minMarginPerTap.keySet()) {
            if (pstRangeAction.getGroupId().isEmpty()) {
                int bestTap = minMarginPerTap.get(pstRangeAction).entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
                bestTaps.put(pstRangeAction, bestTap);
            }
        }
        return bestTaps;
    }

    private Map<Integer, Double> getMinMarginPerTap(PstRangeAction pstRangeAction, double angle, List<BranchCnec> mostLimitingCnecs) {
        int closestTap = pstRangeAction.computeTapPosition(angle);
        double closestAngle = pstRangeAction.convertTapToAngle(closestTap);
        int otherTap;

        if (closestAngle < pstRangeAction.getMaxValue(raoData.getNetwork(), pstRangeAction.getCurrentValue(raoData.getNetwork()))
                && closestAngle > pstRangeAction.getMinValue(raoData.getNetwork(), pstRangeAction.getCurrentValue(raoData.getNetwork()))) {
            double distance1 = Math.abs(pstRangeAction.convertTapToAngle(closestTap + 1) - angle);
            double distance2 = Math.abs(pstRangeAction.convertTapToAngle(closestTap - 1) - angle);
            otherTap = (distance1 < distance2) ? closestTap + 1 : closestTap - 1;
        } else if (closestAngle < pstRangeAction.getMaxValue(raoData.getNetwork(), pstRangeAction.getCurrentValue(raoData.getNetwork()))) {
            otherTap = closestTap + 1;
        } else if (closestAngle > pstRangeAction.getMinValue(raoData.getNetwork(), pstRangeAction.getCurrentValue(raoData.getNetwork()))) {
            otherTap = closestTap - 1;
        } else {
            return Map.of(closestTap, Double.MAX_VALUE);
        }

        double otherAngle = pstRangeAction.convertTapToAngle(otherTap);

        double approxLimitAngle = 0.5 * (closestAngle + otherAngle);

        if (Math.abs(angle - approxLimitAngle) / Math.abs(closestAngle - otherAngle) < 0.1) {
            double minMargin1 = Double.MAX_VALUE;
            double minMargin2 = Double.MAX_VALUE;
            for (BranchCnec cnec : mostLimitingCnecs) {
                // Angle is too close to the limit between two tap positions
                // Chose the tap that maximizes the margin on the most limiting element
                double sensitivity = raoData.getSensitivity(cnec, pstRangeAction);
                double currentSetPoint = pstRangeAction.getCurrentValue(raoData.getNetwork());
                double referenceFlow = raoData.getReferenceFlow(cnec);

                double flow1 = sensitivity * (closestAngle - currentSetPoint) + referenceFlow;
                double flow2 = sensitivity * (otherAngle - currentSetPoint) + referenceFlow;

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
            return Map.of(closestTap, minMargin1, otherTap, minMargin2);
        }
        return Map.of(closestTap, Double.MAX_VALUE);
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
