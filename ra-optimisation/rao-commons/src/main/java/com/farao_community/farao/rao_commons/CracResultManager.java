/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
            double setPointValueInNetwork = rangeAction.getCurrentSetpoint(raoData.getNetwork());
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(raoData.getWorkingVariantId());
            statesAfterOptimizedState.forEach(state -> rangeActionResult.setSetPoint(state.getId(), setPointValueInNetwork));
            if (rangeAction instanceof PstRangeAction) {
                PstRangeResult pstRangeResult = (PstRangeResult) rangeActionResult;
                int tapValueInNetwork = ((PstRangeAction) rangeAction).convertAngleToTap(setPointValueInNetwork);
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
     * This method returns a set of State which are equal or after a given state.
     */
    private Set<State> getStatesAfter(State referenceState) {
        Optional<Contingency> referenceContingency = referenceState.getContingency();
        if (referenceContingency.isEmpty()) {
            return raoData.getCrac().getStates();
        } else {
            return raoData.getCrac().
                    getStates(referenceContingency.get()).stream().
                    filter(state -> state.getInstant().getOrder() >= referenceState.getInstant().getOrder())
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
            CnecResult cnecResult = ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(variantId);
            cnecResult.setFlowInMW(raoData.getSystematicSensitivityResult().getReferenceFlow(cnec));
            cnecResult.setFlowInA(raoData.getSystematicSensitivityResult().getReferenceIntensity(cnec));
            cnecResult.setThresholds((FlowCnec) cnec);
        });
    }

    public void fillCnecResultsWithLoopFlows(LoopFlowResult loopFlowResult) {
        raoData.getLoopflowCnecs().forEach(cnec -> {
            CnecResult cnecResult = ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            if (!Objects.isNull(cnec.getExtension(LoopFlowThreshold.class))) {
                cnecResult.setLoopflowInMW(loopFlowResult.getLoopFlow(cnec));
                cnecResult.setLoopflowThresholdInMW(((FlowCnec) cnec).getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT));
                cnecResult.setCommercialFlowInMW(loopFlowResult.getCommercialFlow(cnec));
            }
        });
    }

    public void fillCnecResultsWithApproximatedLoopFlows() {
        raoData.getLoopflowCnecs().forEach(cnec -> {
            CnecResult cnecResult = ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            if (!Objects.isNull(cnec.getExtension(LoopFlowThreshold.class))) {
                double loopFLow = raoData.getSystematicSensitivityResult().getReferenceFlow(cnec) - cnecResult.getCommercialFlowInMW();
                cnecResult.setLoopflowInMW(loopFLow);
                cnecResult.setLoopflowThresholdInMW(((FlowCnec) cnec).getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT));
            }
        });
    }

    public void copyCommercialFlowsBetweenVariants(String originVariant, String destinationVariant) {
        raoData.getLoopflowCnecs().forEach(cnec ->
                ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(destinationVariant)
                        .setCommercialFlowInMW(((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(originVariant).getCommercialFlowInMW())
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
                ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(destinationVariant).setAbsolutePtdfSum(
                        ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(originVariant).getAbsolutePtdfSum()
                ));
    }

    public void fillResultsFromIteratingLinearOptimizerOutput(IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput, String variantId) {
        //flows
        SensitivityAndLoopflowResults sensitivityAndLoopflowResults = iteratingLinearOptimizerOutput.getSensitivityAndLoopflowResults();
        raoData.getCnecs().forEach(cnec -> {
            CnecResult cnecResult = ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(variantId);
            cnecResult.setFlowInMW(sensitivityAndLoopflowResults.getSystematicSensitivityResult().getReferenceFlow(cnec));
            cnecResult.setFlowInA(sensitivityAndLoopflowResults.getSystematicSensitivityResult().getReferenceIntensity(cnec));
            cnecResult.setThresholds((FlowCnec) cnec);
        });
        raoData.getLoopflowCnecs().forEach(cnec -> {
            CnecResult cnecResult = ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(variantId);
            if (!Objects.isNull(cnec.getExtension(LoopFlowThreshold.class))) {
                cnecResult.setLoopflowInMW(sensitivityAndLoopflowResults.getLoopflow(cnec));
                cnecResult.setLoopflowThresholdInMW(((FlowCnec) cnec).getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT));
                cnecResult.setCommercialFlowInMW(sensitivityAndLoopflowResults.getCommercialFlow(cnec));
            }
        });

        //range actions
        fillRangeActionResultsWithLinearProblem(iteratingLinearOptimizerOutput, variantId);

        //costs
        raoData.getCracResult(variantId).setFunctionalCost(iteratingLinearOptimizerOutput.getFunctionalCost());
        raoData.getCracResult(variantId).setVirtualCost(iteratingLinearOptimizerOutput.getVirtualCost());
        raoData.getCracResult(variantId).setNetworkSecurityStatus(iteratingLinearOptimizerOutput.getFunctionalCost() < 0 ?
                CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);

    }

    public void fillRangeActionResultsWithLinearProblem(IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput, String variantId) {
        Set<State> statesAfterOptimizedState = getStatesAfter(raoData.getOptimizedState());

        for (RangeAction rangeAction : raoData.getCrac().getRangeActions()) {
            if (rangeAction instanceof PstRangeAction) {
                RangeActionResultExtension pstRangeResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
                int approximatedPostOptimTap;
                double approximatedPostOptimAngle;
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();
                Optional<RangeAction> sameNetworkElementRangeAction = raoData.getAvailableRangeActions().stream()
                        .filter(ra -> ra.getNetworkElements().iterator().next().getId().equals(networkElementId) && iteratingLinearOptimizerOutput.getRangeActionSetpoints().containsKey(ra))
                        .findAny();
                if (sameNetworkElementRangeAction.isPresent()) {
                    PstRangeAction pstRangeAction = (PstRangeAction) sameNetworkElementRangeAction.get();

                    approximatedPostOptimTap = iteratingLinearOptimizerOutput.getPstRangeActionTap(pstRangeAction);
                    approximatedPostOptimAngle = iteratingLinearOptimizerOutput.getRangeActionSetpoint(pstRangeAction);

                    LOGGER.debug("Range action {} has been set to tap {}", pstRangeAction.getName(), approximatedPostOptimTap);
                } else {
                    // For range actions that are not available in the perimeter or filtered out of optimization, copy their setpoint from the initial variant
                    approximatedPostOptimTap = ((PstRangeResult) pstRangeResultMap.getVariant(raoData.getPreOptimVariantId())).getTap(raoData.getOptimizedState().getId());
                    approximatedPostOptimAngle = pstRangeResultMap.getVariant(raoData.getPreOptimVariantId()).getSetPoint(raoData.getOptimizedState().getId());
                }

                PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(variantId);
                statesAfterOptimizedState.forEach(state -> {
                    pstRangeResult.setSetPoint(state.getId(), approximatedPostOptimAngle);
                    pstRangeResult.setTap(state.getId(), approximatedPostOptimTap);
                });
            }
        }
    }
}
