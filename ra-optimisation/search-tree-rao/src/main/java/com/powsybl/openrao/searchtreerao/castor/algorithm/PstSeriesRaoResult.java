/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OneStateOnlyRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PreventiveAndCurativesRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyRemedialActions;

/**
 * Given a RaoResult and a collection of pairs RangeAction-SetPoint, the aim of this class is to recompute a new RaoResult
 * that has overridden its corresponding setpoints by the ones provided.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
public class PstSeriesRaoResult {
    //TODO Attention en sortie du traitement initial les variants existent déjà après optimisation
    private static final String INITIAL_SCENARIO = "InitialScenario";
    private static final String PREVENTIVE_SCENARIO = "PreventiveScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;

    private final RaoInput raoInput;
    private final Crac crac;
    private final Network network;
    private final RaoParameters raoParameters;
    // The RaoResult to extract actions + optimized setpoints from
    private final RaoResult initialRaoResult;

    public PstSeriesRaoResult(final RaoInput raoInput,
                              final RaoParameters raoParameters,
                              final RaoResult initialRaoResult) {
        this.raoInput = raoInput;
        this.crac = raoInput.getCrac();
        this.network = raoInput.getNetwork();
        this.raoParameters = raoParameters;
        this.initialRaoResult = initialRaoResult;
    }

    public CompletableFuture<RaoResult> regenRaoResultAndForceSetPoints(final Map<State, Map<RangeAction<?>, Double>> forcedSetPointsByState) {
        String currentStep = "data initialization";

        try {
            RaoUtil.initData(raoInput, raoParameters);
            final StateTree stateTree = new StateTree(crac);
            final ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);

            currentStep = "initial sensitivity analysis";
            // ----- INITIAL SENSI -----
            // compute initial sensitivity on all CNECs
            // (this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters)
            final PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                    crac.getFlowCnecs(),
                    crac.getRangeActions(),
                    raoParameters,
                    toolProvider);

            final PrePerimeterResult initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
            if (initialOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                BUSINESS_LOGS.error("Initial sensitivity analysis failed");
                return CompletableFuture.completedFuture(new FailedRaoResultImpl("Initial sensitivity analysis failed"));
            }
            RaoLogger.logSensitivityAnalysisResults("Initial sensitivity analysis: ",
                    prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                    RemedialActionActivationResultImpl.empty(initialOutput),
                    initialOutput,
                    raoParameters,
                    NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            // ----- PREVENTIVE PERIMETER OPTIMIZATION -----
            // run search tree on preventive perimeter
            currentStep = "first preventive";

            //TODO récupérer l'initial variant et non le working variant pour repartir du réseau initial
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

            final State preventiveState = crac.getPreventiveState();
            if (stateTree.getContingencyScenarios().isEmpty()) {
                final OneStateOnlyRaoResultImpl result = RemedialActionsApplier.applyOptimizedRemedialActions(initialOutput,
                        crac.getFlowCnecs(), preventiveState, network, initialRaoResult, toolProvider, raoParameters, crac, forcedSetPointsByState.getOrDefault(preventiveState, Map.of()));
                return CompletableFuture.completedFuture(result);
            }

            final Set<FlowCnec> preventivePerimeterCnecs = RemedialActionsApplier.getFlowCnecsOfPerimeter(stateTree.getBasecaseScenario(), crac);
            final OptimizationResult preventiveResult = RemedialActionsApplier.applyOptimizedRemedialActions(initialOutput, preventivePerimeterCnecs, preventiveState, network, initialRaoResult, toolProvider, raoParameters, crac, forcedSetPointsByState.getOrDefault(preventiveState, Map.of()))
                    .getOptimizationResult(preventiveState);

            // ----- SENSI POST-PRA -----
            currentStep = "post-PRA sensitivity analysis";
            // mutualise the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs

            network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO, true);
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
            applyRemedialActions(network, preventiveResult, preventiveState);

            final PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, Collections.emptySet(), null);
            if (preCurativeSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions failed");
                return CompletableFuture.completedFuture(new FailedRaoResultImpl("Systematic sensitivity analysis after preventive remedial actions failed"));
            }

            final RaoResult mergedRaoResults;

            // ----- CURATIVE PERIMETERS OPTIMIZATION -----
            currentStep = "contingency scenarios";
            // optimize contingency scenarios (auto + curative instants)

            // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
            // (however RAO could continue depending on parameter optimize-curative-if-basecase-unsecure)

            final double preventiveOptimalCost = preventiveResult.getCost();
            if (shouldStopOptimisationIfPreventiveUnsecure(preventiveOptimalCost)) {
                BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
                mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(preventiveState, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, crac, raoParameters.getObjectiveFunctionParameters());
                return CompletableFuture.completedFuture(mergedRaoResults);
            }


            final RemedialActionsApplier remedialActionsApplier = new RemedialActionsApplier(crac,
                    raoParameters, toolProvider, stateTree, initialRaoResult);
            final Map<State, OptimizationResult> stateOptimizationResultMap = remedialActionsApplier.optimizeContingencyScenarios(network, preCurativeSensitivityAnalysisOutput, forcedSetPointsByState);

            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, stateOptimizationResultMap, crac, raoParameters.getObjectiveFunctionParameters());
            return CompletableFuture.completedFuture(mergedRaoResults);
        } catch (final RuntimeException e) {
            BUSINESS_LOGS.error("{} \n {}", e.getMessage(), ExceptionUtils.getStackTrace(e));
            return CompletableFuture.completedFuture(new FailedRaoResultImpl(String.format("RAO failed during %s : %s", currentStep, e.getMessage())));
        }
    }

    private boolean shouldStopOptimisationIfPreventiveUnsecure(final double preventiveOptimalCost) {
        return raoParameters.getObjectiveFunctionParameters().getType().equals(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW)
               && preventiveOptimalCost > 0
               && !raoParameters.getObjectiveFunctionParameters().getEnforceCurativeSecurity();
    }

}
