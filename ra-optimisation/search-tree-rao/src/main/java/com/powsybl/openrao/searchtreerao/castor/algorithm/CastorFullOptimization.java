/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.*;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.formatDoubleBasedOnMargin;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyRemedialActions;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CastorFullOptimization {
    private static final String INITIAL_SCENARIO = "InitialScenario";
    private static final String PREVENTIVE_SCENARIO = "PreventiveScenario";
    private static final String SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT = "SecondPreventiveScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    private final RaoInput raoInput;
    private final Crac crac;
    private final Network network;
    private final RaoParameters raoParameters;
    private final java.time.Instant targetEndInstant;

    public CastorFullOptimization(RaoInput raoInput, RaoParameters raoParameters, java.time.Instant targetEndInstant) {
        this.raoInput = raoInput;
        this.crac = raoInput.getCrac();
        this.network = raoInput.getNetwork();
        this.raoParameters = raoParameters;
        this.targetEndInstant = targetEndInstant;
    }

    public CompletableFuture<RaoResult> run() {

        RaoUtil.initData(raoInput, raoParameters);
        StateTree stateTree = new StateTree(crac);
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);

        // ----- INITIAL SENSI -----
        // compute initial sensitivity on all CNECs
        // (this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters)
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac.getFlowCnecs(),
            crac.getRangeActions(),
            raoParameters,
            toolProvider);

        PrePerimeterResult initialOutput;
        initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        if (initialOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Initial sensitivity analysis failed");
            return CompletableFuture.completedFuture(new FailedRaoResultImpl("Initial sensitivity analysis failed"));
        }
        RaoLogger.logSensitivityAnalysisResults("Initial sensitivity analysis: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            initialOutput,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // ----- PREVENTIVE PERIMETER OPTIMIZATION -----
        // run search tree on preventive perimeter
        java.time.Instant preventiveRaoStartInstant = java.time.Instant.now();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [start]");

        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

        if (stateTree.getContingencyScenarios().isEmpty()) {
            CompletableFuture<RaoResult> result = CompletableFuture.completedFuture(optimizePreventivePerimeter(stateTree, toolProvider, initialOutput));
            BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
            return result;
        }

        OptimizationResult preventiveResult = optimizePreventivePerimeter(stateTree, toolProvider, initialOutput).getOptimizationResult(crac.getPreventiveState());
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
        java.time.Instant preventiveRaoEndInstant = java.time.Instant.now();
        long preventiveRaoTime = ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant);

        // ----- SENSI POST-PRA -----
        // mutualise the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs

        double preventiveOptimalCost = preventiveResult.getCost();
        TreeParameters automatonTreeParameters = TreeParameters.buildForAutomatonPerimeter(raoParameters);
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, preventiveOptimalCost);
        network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO, true);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        applyRemedialActions(network, preventiveResult, crac.getPreventiveState());

        PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, Collections.emptySet(), null);
        if (preCurativeSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions failed");
            return CompletableFuture.completedFuture(new FailedRaoResultImpl("Systematic sensitivity analysis after preventive remedial actions failed"));
        }
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after preventive remedial actions: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            preCurativeSensitivityAnalysisOutput,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        RaoResult mergedRaoResults;

        // ----- CURATIVE PERIMETERS OPTIMIZATION -----
        // optimize contingency scenarios (auto + curative instants)

        // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
        // (however RAO could continue depending on parameter optimize-curative-if-basecase-unsecure)
        if (shouldStopOptimisationIfPreventiveUnsecure(preventiveOptimalCost)) {
            BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(crac.getPreventiveState(), initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, crac);
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, preCurativeSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);

            return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters());
        }

        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [start]");
        CastorContingencyScenarios castorContingencyScenarios = new CastorContingencyScenarios(crac, raoParameters, toolProvider, stateTree, automatonTreeParameters, curativeTreeParameters, initialOutput);
        Map<State, OptimizationResult> postContingencyResults = castorContingencyScenarios.optimizeContingencyScenarios(network, preCurativeSensitivityAnalysisOutput, false);
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");

        // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----
        mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, postContingencyResults, crac);
        boolean logFinalResultsOutsideOfSecondPreventive = true;
        // Run second preventive when necessary
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, raoParameters, network, stateTree, toolProvider, targetEndInstant);
        if (castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, postContingencyResults.values(), mergedRaoResults, preventiveRaoTime)) {
            RaoResult secondPreventiveRaoResults = castorSecondPreventive.runSecondPreventiveAndAutoRao(castorContingencyScenarios, prePerimeterSensitivityAnalysis, initialOutput, preventiveResult, postContingencyResults);
            if (secondPreventiveImprovesResults(secondPreventiveRaoResults, mergedRaoResults)) {
                mergedRaoResults = secondPreventiveRaoResults;
                mergedRaoResults.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST);
                logFinalResultsOutsideOfSecondPreventive = false;
            } else {
                mergedRaoResults.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION);
            }
        }
        // Log final results
        if (logFinalResultsOutsideOfSecondPreventive) {
            BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, stateTree.getBasecaseScenario(), preventiveResult, stateTree.getContingencyScenarios(), postContingencyResults, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        }

        return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters());
    }

    private boolean shouldStopOptimisationIfPreventiveUnsecure(double preventiveOptimalCost) {
        return raoParameters.getObjectiveFunctionParameters().getPreventiveStopCriterion().equals(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE)
            && preventiveOptimalCost > 0
            && !raoParameters.getObjectiveFunctionParameters().getEnforceCurativeSecurity();
    }

    /**
     * Return true if 2P has decreased cost
     */
    private boolean secondPreventiveImprovesResults(RaoResult secondPreventiveRaoResults, RaoResult mergedRaoResults) {
        if (secondPreventiveRaoResults instanceof FailedRaoResultImpl) {
            BUSINESS_LOGS.info("Second preventive failed. Falling back to previous solution:");
            return false;
        }
        if (mergedRaoResults.getComputationStatus() == ComputationStatus.FAILURE && secondPreventiveRaoResults.getComputationStatus() != ComputationStatus.FAILURE) {
            BUSINESS_LOGS.info("RAO has succeeded thanks to second preventive step when first preventive step had failed");
            return true;
        }
        Instant curativeInstant = crac.getLastInstant();
        double firstPreventiveCost = mergedRaoResults.getCost(curativeInstant);
        double secondPreventiveCost = secondPreventiveRaoResults.getCost(curativeInstant);
        if (secondPreventiveCost > firstPreventiveCost) {
            BUSINESS_LOGS.info("Second preventive step has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to previous solution:",
                formatDoubleBasedOnMargin(firstPreventiveCost, -firstPreventiveCost), formatDoubleBasedOnMargin(mergedRaoResults.getFunctionalCost(curativeInstant), -firstPreventiveCost), formatDoubleBasedOnMargin(mergedRaoResults.getVirtualCost(curativeInstant), -firstPreventiveCost),
                formatDoubleBasedOnMargin(secondPreventiveCost, -secondPreventiveCost), formatDoubleBasedOnMargin(secondPreventiveRaoResults.getFunctionalCost(curativeInstant), -secondPreventiveCost), formatDoubleBasedOnMargin(secondPreventiveRaoResults.getVirtualCost(curativeInstant), -secondPreventiveCost));
            return false;
        }
        return true;
    }

    /**
     * Return initial result if RAO has increased cost
     */
    private CompletableFuture<RaoResult> postCheckResults(RaoResult raoResult, PrePerimeterResult initialResult, ObjectiveFunctionParameters objectiveFunctionParameters) {
        RaoResult finalRaoResult = raoResult;

        double initialCost = initialResult.getCost();
        double initialFunctionalCost = initialResult.getFunctionalCost();
        double initialVirtualCost = initialResult.getVirtualCost();
        Instant lastInstant = crac.getLastInstant();
        double finalCost = finalRaoResult.getCost(lastInstant);
        double finalFunctionalCost = finalRaoResult.getFunctionalCost(lastInstant);
        double finalVirtualCost = finalRaoResult.getVirtualCost(lastInstant);

        if (finalCost > initialCost) {
            BUSINESS_LOGS.info("RAO has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to initial solution:",
                formatDoubleBasedOnMargin(initialCost, -initialCost), formatDoubleBasedOnMargin(initialFunctionalCost, -initialCost), formatDoubleBasedOnMargin(initialVirtualCost, -initialCost),
                formatDoubleBasedOnMargin(finalCost, -finalCost), formatDoubleBasedOnMargin(finalFunctionalCost, -finalCost), formatDoubleBasedOnMargin(finalVirtualCost, -finalCost));
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, initialResult, objectiveFunctionParameters.getType(), objectiveFunctionParameters.getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);
            finalRaoResult = new UnoptimizedRaoResultImpl(initialResult);
            finalCost = initialCost;
            finalFunctionalCost = initialFunctionalCost;
            finalVirtualCost = initialVirtualCost;
            if (raoResult.getOptimizationStepsExecuted().equals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY)) {
                finalRaoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
            } else {
                finalRaoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
            }
        }

        // Log costs before and after RAO
        BUSINESS_LOGS.info("Cost before RAO = {} (functional: {}, virtual: {}), cost after RAO = {} (functional: {}, virtual: {})",
            formatDoubleBasedOnMargin(initialCost, -initialCost), formatDoubleBasedOnMargin(initialFunctionalCost, -initialCost), formatDoubleBasedOnMargin(initialVirtualCost, -initialCost),
            formatDoubleBasedOnMargin(finalCost, -finalCost), formatDoubleBasedOnMargin(finalFunctionalCost, -finalCost), formatDoubleBasedOnMargin(finalVirtualCost, -finalCost));

        return CompletableFuture.completedFuture(finalRaoResult);
    }

    private OneStateOnlyRaoResultImpl optimizePreventivePerimeter(StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialResult) {

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(stateTree.getBasecaseScenario(), crac, network, raoParameters, initialResult);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(TreeParameters.buildForPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()))
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(initialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResult, initialResult, Collections.emptySet(), raoParameters))
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult optResult = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();
        applyRemedialActions(network, optResult, crac.getPreventiveState());
        return new OneStateOnlyRaoResultImpl(crac.getPreventiveState(), initialResult, optResult, searchTreeInput.getOptimizationPerimeter().getFlowCnecs());
    }
}
