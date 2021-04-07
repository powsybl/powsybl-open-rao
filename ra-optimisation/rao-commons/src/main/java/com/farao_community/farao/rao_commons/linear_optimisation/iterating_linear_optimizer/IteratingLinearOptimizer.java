/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.LoopFlowUtil;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.*;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(IteratingLinearOptimizer.class);

    private IteratingLinearOptimizer() {

    }

    private static LinearOptimizerInput createLinearOptimizerInput(IteratingLinearOptimizerInput iteratingLinearOptimizerInput) {
        return LinearOptimizerInput.create()
                .withCnecs(iteratingLinearOptimizerInput.getCnecs())
                .withLoopflowCnecs(iteratingLinearOptimizerInput.getLoopflowCnecs())
                .withInitialCnecResults(iteratingLinearOptimizerInput.getInitialCnecResults())
                .withNetwork(iteratingLinearOptimizerInput.getNetwork())
                .withPrePerimeterCnecMarginsInAbsoluteMW(iteratingLinearOptimizerInput.getPrePerimeterCnecMarginsInAbsoluteMW())
                .withPreperimeterSetpoints(iteratingLinearOptimizerInput.getPreperimeterSetpoints())
                .withRangeActions(iteratingLinearOptimizerInput.getRangeActions())
                .withMostLimitingElements(iteratingLinearOptimizerInput.getObjectiveFunctionEvaluator().getMostLimitingElements(iteratingLinearOptimizerInput.getPreOptimSensitivityResults(), 10))
                .build();
    }

    private static LinearOptimizerParameters createLinearOptimizerParameters(IteratingLinearOptimizerParameters iteratingLinearOptimizerParameters) {
        return LinearOptimizerParameters.create()
                .withObjectiveFunction(iteratingLinearOptimizerParameters.getObjectiveFunction())
                .withMaxPstPerTso(iteratingLinearOptimizerParameters.getMaxPstPerTso())
                .withPstSensitivityThreshold(iteratingLinearOptimizerParameters.getPstSensitivityThreshold())
                .withOperatorsNotToOptimize(iteratingLinearOptimizerParameters.getOperatorsNotToOptimize())
                .withMnecAcceptableMarginDiminution(iteratingLinearOptimizerParameters.getMnecAcceptableMarginDiminution())
                .withLoopFlowApproximationLevel(iteratingLinearOptimizerParameters.getLoopFlowApproximationLevel())
                .withLoopFlowConstraintAdjustmentCoefficient(iteratingLinearOptimizerParameters.getLoopFlowConstraintAdjustmentCoefficient())
                .withLoopFlowViolationCost(iteratingLinearOptimizerParameters.getLoopFlowViolationCost())
                .withLoopFlowAcceptableAugmentation(iteratingLinearOptimizerParameters.getLoopFlowAcceptableAugmentation())
                .withMnecViolationCost(iteratingLinearOptimizerParameters.getMnecViolationCost())
                .withMnecConstraintAdjustmentCoefficient(iteratingLinearOptimizerParameters.getMnecConstraintAdjustmentCoefficient())
                .withNegativeMarginObjectiveCoefficient(iteratingLinearOptimizerParameters.getNegativeMarginObjectiveCoefficient())
                .withPstPenaltyCost(iteratingLinearOptimizerParameters.getPstPenaltyCost())
                .withPtdfSumLowerBound(iteratingLinearOptimizerParameters.getPtdfSumLowerBound())
                .withRaoWithLoopFlowLimitation(iteratingLinearOptimizerParameters.isRaoWithLoopFlowLimitation())
                .build();
    }

    private static IteratingLinearOptimizerOutput createOutputFromPreOptimSituation(IteratingLinearOptimizerInput iteratingLinearOptimizerInput) {
        ObjectiveFunctionEvaluator objectiveFunctionEvaluator = iteratingLinearOptimizerInput.getObjectiveFunctionEvaluator();
        SensitivityAndLoopflowResults sensitivityAndLoopflowResults = iteratingLinearOptimizerInput.getPreOptimSensitivityResults();
        Network network = iteratingLinearOptimizerInput.getNetwork();

        IteratingLinearOptimizerOutput.SolveStatus solveStatus = IteratingLinearOptimizerOutput.SolveStatus.NOT_SOLVED;
        double functionalCost = objectiveFunctionEvaluator.computeFunctionalCost(sensitivityAndLoopflowResults);
        double virtualCost = objectiveFunctionEvaluator.computeVirtualCost(sensitivityAndLoopflowResults);
        Map<RangeAction, Double> rangeActionSetPoints = new HashMap<>();
        Map<PstRangeAction, Integer> pstTaps = new HashMap<>();
        for (RangeAction rangeAction : iteratingLinearOptimizerInput.getRangeActions()) {
            rangeActionSetPoints.put(rangeAction, rangeAction.getCurrentValue(network));
            if (rangeAction instanceof PstRangeAction) {
                PstRangeAction pstRangeAction = (PstRangeAction) rangeAction;
                pstTaps.put(pstRangeAction, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.CENTERED_ON_ZERO));
            }
        }

        return new IteratingLinearOptimizerOutput(solveStatus, functionalCost, virtualCost, rangeActionSetPoints, pstTaps, sensitivityAndLoopflowResults);
    }

    public static IteratingLinearOptimizerOutput optimize(IteratingLinearOptimizerInput iteratingLinearOptimizerInput, IteratingLinearOptimizerParameters iteratingLinearOptimizerParameters) {

        Network network = iteratingLinearOptimizerInput.getNetwork();
        SystematicSensitivityInterface systematicSensitivityInterface = iteratingLinearOptimizerInput.getSystematicSensitivityInterface();
        ObjectiveFunctionEvaluator objectiveFunctionEvaluator = iteratingLinearOptimizerInput.getObjectiveFunctionEvaluator();
        SensitivityAndLoopflowResults preOptimSensitivityResults = iteratingLinearOptimizerInput.getPreOptimSensitivityResults();

        LinearOptimizer linearOptimizer = new LinearOptimizer(createLinearOptimizerInput(iteratingLinearOptimizerInput), createLinearOptimizerParameters(iteratingLinearOptimizerParameters));

        IteratingLinearOptimizerOutput bestIteratingLinearOptimizerOutput = createOutputFromPreOptimSituation(iteratingLinearOptimizerInput);
        SensitivityAndLoopflowResults updatedSensitivityAndLoopflowResults = preOptimSensitivityResults;

        for (int iteration = 1; iteration <= iteratingLinearOptimizerParameters.getMaxIterations(); iteration++) {
            LinearOptimizerOutput linearOptimizerOutput;
            try {
                LOGGER.debug("Iteration {} - linear optimization [start]", iteration);
                linearOptimizerOutput = linearOptimizer.optimize(updatedSensitivityAndLoopflowResults);
                LOGGER.debug("Iteration {} - linear optimization [end]", iteration);
            } catch (LinearOptimisationException e) {
                LOGGER.error("Linear optimization failed at iteration {}: {}", iteration, e.getMessage());
                bestIteratingLinearOptimizerOutput.setStatus(IteratingLinearOptimizerOutput.SolveStatus.ABNORMAL);
                return bestIteratingLinearOptimizerOutput;
            }

            if (!linearOptimizerOutput.getSolveStatus().equals(LinearOptimizerOutput.SolveStatus.OPTIMAL)) {
                LOGGER.warn("Iteration {} - linear optimization cannot find OPTIMAL solution", iteration);
                if (iteration > 1) {
                    bestIteratingLinearOptimizerOutput.setStatus(IteratingLinearOptimizerOutput.SolveStatus.FEASIBLE);
                } else {
                    bestIteratingLinearOptimizerOutput.setStatus(getFirstIterationSolveStatusFromLinear(linearOptimizerOutput.getSolveStatus()));
                }
                return bestIteratingLinearOptimizerOutput;
            } else if (!hasRemedialActionsChanged(bestIteratingLinearOptimizerOutput, linearOptimizerOutput, iteration)) {
                // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
                LOGGER.info("Iteration {} - same results as previous iterations, optimal solution found", iteration);
                bestIteratingLinearOptimizerOutput.setStatus(IteratingLinearOptimizerOutput.SolveStatus.OPTIMAL);
                return bestIteratingLinearOptimizerOutput;
            } else {
                updatedSensitivityAndLoopflowResults = applyRangeActionsAndRunSensitivityComputation(iteratingLinearOptimizerInput, linearOptimizerOutput.getRangeActionSetpoints(), updatedSensitivityAndLoopflowResults, iteration, iteratingLinearOptimizerParameters.getLoopFlowApproximationLevel());
                double functionalCost = objectiveFunctionEvaluator.computeFunctionalCost(updatedSensitivityAndLoopflowResults);
                double virtualCost = objectiveFunctionEvaluator.computeVirtualCost(updatedSensitivityAndLoopflowResults);
                if (functionalCost + virtualCost < bestIteratingLinearOptimizerOutput.getCost()) {
                    LOGGER.info("Iteration {} - Better solution found with a minimum margin of {} {} (optimisation criterion : {})",
                            iteration, -functionalCost, objectiveFunctionEvaluator.getUnit(), functionalCost + virtualCost);
                    bestIteratingLinearOptimizerOutput = new IteratingLinearOptimizerOutput(IteratingLinearOptimizerOutput.SolveStatus.FEASIBLE, functionalCost, virtualCost,
                            linearOptimizerOutput, updatedSensitivityAndLoopflowResults);
                } else {
                    LOGGER.info("Iteration {} - Linear Optimization found a worse result than previous iteration, with a minimum margin from {} to {} {} (optimisation criterion : from {} to {})",
                            iteration, -bestIteratingLinearOptimizerOutput.getFunctionalCost(), -functionalCost, objectiveFunctionEvaluator.getUnit(), bestIteratingLinearOptimizerOutput.getCost(), functionalCost + virtualCost);
                    bestIteratingLinearOptimizerOutput.setStatus(IteratingLinearOptimizerOutput.SolveStatus.OPTIMAL);
                    return bestIteratingLinearOptimizerOutput;
                }
            }
        }
        return bestIteratingLinearOptimizerOutput;
    }

    private static IteratingLinearOptimizerOutput.SolveStatus getFirstIterationSolveStatusFromLinear(LinearOptimizerOutput.SolveStatus solveStatus) {
        switch (solveStatus) {
            case OPTIMAL:
                return IteratingLinearOptimizerOutput.SolveStatus.OPTIMAL;
            case FEASIBLE:
                return IteratingLinearOptimizerOutput.SolveStatus.FEASIBLE;
            case INFEASIBLE:
                return IteratingLinearOptimizerOutput.SolveStatus.INFEASIBLE;
            case UNBOUNDED:
                return IteratingLinearOptimizerOutput.SolveStatus.UNBOUNDED;
            case NOT_SOLVED:
                return IteratingLinearOptimizerOutput.SolveStatus.NOT_SOLVED;
            case ABNORMAL:
            default:
                return IteratingLinearOptimizerOutput.SolveStatus.ABNORMAL;
        }
    }

    private static boolean hasRemedialActionsChanged(IteratingLinearOptimizerOutput bestIteratingLinearOptimizerOutput, LinearOptimizerOutput linearOptimizerOutput, int iteration) {
        Map<RangeAction, Double> newSetPoints = linearOptimizerOutput.getRangeActionSetpoints();
        Map<RangeAction, Double> bestSetPoints = bestIteratingLinearOptimizerOutput.getRangeActionSetpoints();

        // TODO : verify if this is necessary
        /* if(!bestSetPoints.keySet().equals(newSetPoints.keySet())) {
            return true;
        }*/

        for (Map.Entry<RangeAction, Double> newSetPointEntry : newSetPoints.entrySet()) {
            if (Math.abs(newSetPointEntry.getValue() - bestSetPoints.get(newSetPointEntry.getKey())) > Math.max(1e-2 * Math.abs(bestSetPoints.get(newSetPointEntry.getKey())), 1e-3)) {
                return true;
            }
        }

        return false;
    }

    private static SensitivityAndLoopflowResults applyRangeActionsAndRunSensitivityComputation(IteratingLinearOptimizerInput iteratingLinearOptimizerInput,
                                                                                               Map<RangeAction, Double> rangeActionSetPoints,
                                                                                               SensitivityAndLoopflowResults sensitivityAndLoopflowResults,
                                                                                               int iteration,
                                                                                               RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel) throws SensitivityAnalysisException {
        Network network = iteratingLinearOptimizerInput.getNetwork();
        SystematicSensitivityInterface systematicSensitivityInterface = iteratingLinearOptimizerInput.getSystematicSensitivityInterface();
        rangeActionSetPoints.keySet().forEach(rangeAction -> rangeAction.apply(network, rangeActionSetPoints.get(rangeAction)));

        LOGGER.debug("Iteration {} - systematic analysis [start]", iteration);
        try {
            SystematicSensitivityResult updatedSensiResult = systematicSensitivityInterface.run(network);
            LOGGER.debug("Iteration {} - systematic analysis [end]", iteration);

            if (loopFlowApproximationLevel.shouldUpdatePtdfWithPstChange()) {
                return new SensitivityAndLoopflowResults(updatedSensiResult, LoopFlowUtil.computeCommercialFlows(network, iteratingLinearOptimizerInput.getLoopflowCnecs(), iteratingLinearOptimizerInput.getGlskProvider(), iteratingLinearOptimizerInput.getReferenceProgram(), updatedSensiResult));
            } else {
                return new SensitivityAndLoopflowResults(updatedSensiResult, sensitivityAndLoopflowResults.getCommercialFlows());
            }
        } catch (SensitivityAnalysisException e) {
            LOGGER.error("Sensitivity computation failed at iteration {} on {} mode: {}", iteration, systematicSensitivityInterface.isFallback() ? "Fallback" : "Default", e.getMessage());
            return sensitivityAndLoopflowResults;
        }
    }
}
