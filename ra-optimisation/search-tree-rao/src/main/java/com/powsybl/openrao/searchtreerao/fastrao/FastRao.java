/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.fastrao;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.json.JsonExport;
import com.powsybl.openrao.data.crac.io.json.JsonImport;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorFullOptimization;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PostPerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class FastRao implements RaoProvider {
    private static final String FAST_RAO = "FastRao";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run

    @Override
    public String getName() {
        return FAST_RAO;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        return run(raoInput, parameters, null);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
        RaoUtil.initData(raoInput, parameters);
        return CompletableFuture.completedFuture(launchFilteredRao(raoInput, parameters, targetEndInstant, new HashSet<>()));
    }

    public static RaoResult launchFilteredRao(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, Set<FlowCnec> consideredCnecs) {

        if (!parameters.hasExtension(FastRaoParameters.class)) {
            BUSINESS_LOGS.error("Fast Rao requires FastRaoParameters");
            return new FailedRaoResultImpl("Fast Rao requires FastRaoParameters");
        }

        if (raoInput.getOptimizedState() != null) {
            BUSINESS_LOGS.error("Fast Rao does not support optimization on one given state only");
            return new FailedRaoResultImpl("Fast Rao does not support optimization on one given state only");
        }

        if (raoInput.getCrac().getInstants(InstantKind.CURATIVE).size() > 1) {
            BUSINESS_LOGS.error("Fast Rao does not support multi-curative optimization");
            return new FailedRaoResultImpl("Fast Rao does not support multi-curative optimization");
        }

        try {
            // Retrieve input data
            Crac crac = raoInput.getCrac();
            Collection<String> initialNetworkVariants = new HashSet<>(raoInput.getNetwork().getVariantManager().getVariantIds());

            ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, parameters);

            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                crac,
                crac.getFlowCnecs(),
                crac.getRangeActions(),
                parameters,
                toolProvider);

            // Run initial sensi (for initial values, and to know which cnecs to put in the first rao)
            PrePerimeterResult initialResult = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork());

            if (initialResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
                BUSINESS_LOGS.error("Initial sensitivity analysis failed");
                return new FailedRaoResultImpl("Initial sensitivity analysis failed");
            }

            RaoLogger.logSensitivityAnalysisResults("[FAST RAO] Initial sensitivity analysis: ",
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                RemedialActionActivationResultImpl.empty(initialResult),
                initialResult,
                parameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            PrePerimeterResult stepResult = initialResult;
            FlowCnec worstCnec;
            FastRaoResultImpl raoResult;
            com.powsybl.openrao.data.crac.api.Instant lastInstant = raoInput.getCrac().getLastInstant();
            AbstractNetworkPool networkPool = AbstractNetworkPool.create(raoInput.getNetwork(), raoInput.getNetworkVariantId(), 3, true);
            int counter = 1;

            do {
                addWorstCnecs(consideredCnecs, parameters.getExtension(FastRaoParameters.class).getNumberOfCnecsToAdd(), stepResult);
                if (parameters.getExtension(FastRaoParameters.class).getAddUnsecureCnecs()) {
                    consideredCnecs.addAll(getUnsecureFunctionalCnecs(stepResult, parameters.getObjectiveFunctionParameters().getUnit(), parameters.getExtension(FastRaoParameters.class).getMarginLimit()));
                }
                consideredCnecs.addAll(getCostlyVirtualCnecs(stepResult));
                // Add worst preventive cnec to considered cencs to ensure preventive state is defined
                consideredCnecs.add(getWorstPreventiveCnec(stepResult, crac));
                // TODO pourquoi clean variants, ne nettoie t on pas au fur et à mesure les variantes ?
                cleanVariants(raoInput.getNetwork(), initialNetworkVariants, raoInput.getNetworkVariantId());

                raoResult = runFilteredRao(raoInput, parameters, targetEndInstant, consideredCnecs, toolProvider, initialResult, networkPool, counter);
                stepResult = raoResult.getAppropriateResult(lastInstant);

                RaoLogger.logObjectiveFunctionResult(String.format("[FAST RAO] Iteration %d: sensitivity analysis: ", counter),
                    stepResult,
                    stepResult,
                    parameters,
                    NUMBER_LOGGED_ELEMENTS_DURING_RAO);

                worstCnec = stepResult.getMostLimitingElements(1).get(0);
                counter++;
            } while (!(consideredCnecs.contains(worstCnec) && consideredCnecs.containsAll(getCostlyVirtualCnecs(stepResult))));

            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);

            RaoLogger.logObjectiveFunctionResult("[FAST RAO] Final Result: ",
                stepResult,
                stepResult,
                parameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            raoResult.addExtension(
                CriticalCnecsResult.class,
                new CriticalCnecsResult(consideredCnecs.stream().map(FlowCnec::getId).collect(Collectors.toSet()))
            );
            return raoResult;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenRaoException("Error while running full FAST RAO loop", e);
        }
    }

    private static void addWorstCnecs(Set<FlowCnec> consideredCnecs, int numberOfCnecsToAdd, PrePerimeterResult ofResult) {
        List<FlowCnec> orderedCnecs = ofResult.getMostLimitingElements(Integer.MAX_VALUE);
        int counter = 0;
        for (FlowCnec cnec : orderedCnecs) {
            if (counter >= numberOfCnecsToAdd) {
                return;
            }
            if (consideredCnecs.add(cnec)) {
                counter++;
            }
        }
    }

    private static void cleanVariants(Network network, Collection<String> initialNetworkVariants, String initialNetworkVariantId) {
        VariantManager variantManager = network.getVariantManager();
        Set<String> variantsToRemove = new HashSet<>();
        variantManager.getVariantIds().stream()
            .filter(id -> !initialNetworkVariants.contains(id))
            .forEach(variantsToRemove::add);
        variantsToRemove.forEach(variantManager::removeVariant);
        network.getVariantManager().setWorkingVariant(initialNetworkVariantId);
    }

    private static FlowCnec getWorstPreventiveCnec(ObjectiveFunctionResult ofResult, Crac crac) {
        List<FlowCnec> orderedCnecs = ofResult.getMostLimitingElements(Integer.MAX_VALUE);
        return orderedCnecs.stream().filter(cnec -> cnec.getState().isPreventive()).findFirst().orElse(
            // If only MNECs are present previous list will be empty
            crac.getFlowCnecs(crac.getPreventiveState()).stream().findFirst().orElseThrow()
        );
    }

    private static Set<FlowCnec> getUnsecureFunctionalCnecs(PrePerimeterResult prePerimeterResult, Unit unit, Double marginLimit) {
        List<FlowCnec> orderedCnecs = prePerimeterResult.getMostLimitingElements(Integer.MAX_VALUE);
        Set<FlowCnec> flowCnecs = new HashSet<>();
        for (FlowCnec cnec : orderedCnecs) {
            if (prePerimeterResult.getMargin(cnec, unit) < marginLimit) {
                flowCnecs.add(cnec);
            }
        }
        return flowCnecs;
    }

    private static Set<FlowCnec> getCostlyVirtualCnecs(ObjectiveFunctionResult ofResult) {
        Set<FlowCnec> flowCnecs = new HashSet<>();
        ofResult.getVirtualCostNames().forEach(name -> flowCnecs.addAll(ofResult.getCostlyElements(name, Integer.MAX_VALUE)));
        return flowCnecs;
    }

    private static FastRaoResultImpl runFilteredRao(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, Set<FlowCnec> flowCnecsToKeep, ToolProvider toolProvider, PrePerimeterResult initialResult, AbstractNetworkPool networkPool, int counter) {
        Crac crac = raoInput.getCrac();

        // Filter CRAC to only keep flowCnecsToKeep
        Crac filteredCrac = copyCrac(crac, raoInput.getNetwork());
        removeFlowCnecsFromCrac(filteredCrac, flowCnecsToKeep);

        BUSINESS_LOGS.info("[FAST RAO] Iteration {}: Run filtered RAO [start]", counter);

        RaoInput filteredRaoInput = createFilteredRaoInput(raoInput, filteredCrac);
        RaoResult raoResult;
        try {
            raoResult = new CastorFullOptimization(filteredRaoInput, parameters, targetEndInstant).run().get();
            List<String> preventiveNetworkActions = raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).stream()
                .map(Identifiable::getId)
                .toList();
            // Define combinations to improve performance
            if (preventiveNetworkActions.size() >= 2) {
                List<List<String>> predefinedCombinations = parameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().getPredefinedCombinations();
                if (!predefinedCombinations.contains(preventiveNetworkActions)) {
                    predefinedCombinations.add(preventiveNetworkActions);
                }
                parameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().setPredefinedCombinations(predefinedCombinations);
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new OpenRaoException("Error while running filtered RAO", e);

        }

        BUSINESS_LOGS.info("[FAST RAO] Iteration {}: Run filtered RAO [end]", counter);

        String finalVariantId = raoInput.getNetwork().getVariantManager().getWorkingVariantId();
        raoInput.getNetwork().getVariantManager().setWorkingVariant(raoInput.getNetworkVariantId());

        BUSINESS_LOGS.info("[FAST RAO] Iteration {}: Run full sensitivity analysis [start]", counter);

        // Compute sensitivity analyses after PRA, after ARA, after CRA to build RaoResult
        StateTree stateTree = new StateTree(crac);

        // TODO PostPerimeterSensitivtyAnalysis's runBasedOnInitialPreviousAndOptimizationResults
        // could wait for a Future<PostPerimeterResult>. On n'aurait plus à caster puis recréer un Future ici
        // 1) Post PRA
        AtomicReference<PrePerimeterResult> postPraSensi = new AtomicReference<>();
        ForkJoinTask<?> computePostPraSensi = networkPool.submit(() -> {
            try {
                Network networkCopy = networkPool.getAvailableNetwork();
                // Apply PRAs
                applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                // Generate optimization result based on previous result, i.e initial result
                RemedialActionActivationResult postPraRemedialActionActivationResult = createRemedialActionsResults(InstantKind.PREVENTIVE, raoResult, filteredCrac, initialResult);
                ObjectiveFunctionResult objectiveFunctionResult = generatePreviousObjectiveFunctionResult(toolProvider, raoInput,
                    parameters, crac.getFlowCnecs(), initialResult, CompletableFuture.completedFuture(initialResult), postPraRemedialActionActivationResult, InstantKind.PREVENTIVE).get();
                OptimizationResult postPraOptimizationResult = new OptimizationResultImpl(objectiveFunctionResult, initialResult,
                    initialResult, postPraRemedialActionActivationResult, postPraRemedialActionActivationResult);
                // Run sensi. Objective function result is updated right after with latest sensi results.
                PostPerimeterSensitivityAnalysis postPraPerimeterSensiAnalysis = new PostPerimeterSensitivityAnalysis(crac, crac.getStates(), parameters, toolProvider);
                postPraSensi.set(postPraPerimeterSensiAnalysis.runBasedOnInitialPreviousAndOptimizationResults(
                    networkCopy,
                    initialResult,
                    CompletableFuture.completedFuture(initialResult),
                    stateTree.getOperatorsNotSharingCras(),
                    postPraOptimizationResult,
                    new AppliedRemedialActions()).get().getPrePerimeterResultForAllFollowingStates());
                networkPool.releaseUsedNetwork(networkCopy);
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new OpenRaoException("Error while running post pra sensi", e);
            }
        });

        // Post ARA sensi is based on post PRA results for optimization result.
        computePostPraSensi.join();

        // 2) Post ARA
        AtomicReference<PrePerimeterResult> postAraSensi = new AtomicReference<>();
        ForkJoinTask<?> computePostAraSensi = networkPool.submit(() -> {
            try {
                Network networkCopy = networkPool.getAvailableNetwork();
                // Apply PRAs
                applyOptimalPreventiveRemedialActions(networkCopy, crac.getPreventiveState(), raoResult);
                // Generate optimization result based on previous result, i.e postPraSensi
                RemedialActionActivationResult postAraRemedialActionActivationResult = createRemedialActionsResults(InstantKind.AUTO, raoResult, crac, initialResult);
                AppliedRemedialActions appliedAutoRemedialActions = createAppliedRemedialActions(crac, raoResult, InstantKind.AUTO);
                ObjectiveFunctionResult objectiveFunctionResult = generatePreviousObjectiveFunctionResult(toolProvider, raoInput,
                    parameters, crac.getFlowCnecs(), initialResult, CompletableFuture.completedFuture(postPraSensi.get()), postAraRemedialActionActivationResult, InstantKind.AUTO).get();
                OptimizationResult postAraOptimizationResult = new OptimizationResultImpl(objectiveFunctionResult, initialResult,
                    initialResult, postAraRemedialActionActivationResult, postAraRemedialActionActivationResult);
                // Run sensi with auto remedial actions. Objective function result is updated right after with latest sensi results.
                PostPerimeterSensitivityAnalysis postPerimeterSensiAnalysis = new PostPerimeterSensitivityAnalysis(crac, crac.getStates(), parameters, toolProvider);
                postAraSensi.set(postPerimeterSensiAnalysis.runBasedOnInitialPreviousAndOptimizationResults(
                    networkCopy,
                    initialResult,
                    CompletableFuture.completedFuture(postPraSensi.get()),
                    stateTree.getOperatorsNotSharingCras(),
                    postAraOptimizationResult,
                    appliedAutoRemedialActions).get().getPrePerimeterResultForAllFollowingStates());
                networkPool.releaseUsedNetwork(networkCopy);
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new OpenRaoException("Error while running post ara sensi", e);
            }
        });

        // Post CRA sensi is based on post ARA results for optimization result.
        computePostAraSensi.join();

        // 3) Post CRA
        AtomicReference<PrePerimeterResult> postCraSensi = new AtomicReference<>();
        ForkJoinTask<?> computePostCraSensi = networkPool.submit(() -> {
            try {
                Network networkCopy = networkPool.getAvailableNetwork();
                // Apply PRAs
                applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                // Generate optimization result based on previous result, i.e postAraSensi
                RemedialActionActivationResult postCraRemedialActionActivationResult = createRemedialActionsResults(InstantKind.CURATIVE, raoResult, crac, initialResult);
                AppliedRemedialActions appliedRemedialActions = createAppliedRemedialActions(filteredCrac, raoResult, InstantKind.CURATIVE);
                Future<ObjectiveFunctionResult> objectiveFunctionResult = generatePreviousObjectiveFunctionResult(toolProvider, raoInput,
                    parameters, crac.getFlowCnecs(), initialResult, CompletableFuture.completedFuture(postAraSensi.get()), postCraRemedialActionActivationResult, InstantKind.CURATIVE);
                OptimizationResult postCraOptimizationResult = new OptimizationResultImpl(objectiveFunctionResult.get(), initialResult,
                    initialResult, postCraRemedialActionActivationResult, postCraRemedialActionActivationResult);
                // Run sensi with curative remedial actions. Objective function result is updated right after with latest sensi results.
                PostPerimeterSensitivityAnalysis postPerimeterSensiAnalysis = new PostPerimeterSensitivityAnalysis(crac, crac.getStates(), parameters, toolProvider);
                postCraSensi.set(postPerimeterSensiAnalysis.runBasedOnInitialPreviousAndOptimizationResults(
                    networkCopy,
                    initialResult,
                    CompletableFuture.completedFuture(postAraSensi.get()),
                    stateTree.getOperatorsNotSharingCras(),
                    postCraOptimizationResult,
                    appliedRemedialActions).get().getPrePerimeterResultForAllFollowingStates());
                networkPool.releaseUsedNetwork(networkCopy);
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new OpenRaoException("Error while running post cra sensi", e);
            }
        });

        computePostCraSensi.join();
        raoInput.getNetwork().getVariantManager().setWorkingVariant(finalVariantId);

        BUSINESS_LOGS.info("[FAST RAO] Iteration {}: Run full sensitivity analysis [end]", counter);

        return new FastRaoResultImpl(initialResult, postPraSensi.get(), postAraSensi.get(), postCraSensi.get(), raoResult, raoInput.getCrac());

    }

    private static RemedialActionActivationResult createRemedialActionsResults(InstantKind instantKind, RaoResult raoResult, Crac crac, PrePerimeterResult initialResult) {

        Map<State, Set<NetworkAction>> networkActionsActivated = new HashMap<>();
        RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(initialResult.getRangeActionSetpointResult());
        if (raoResult instanceof OneStateOnlyRaoResultImpl) {
            State preventiveState = crac.getPreventiveState();
            networkActionsActivated.put(preventiveState, raoResult.getActivatedNetworkActionsDuringState(preventiveState));
            if (!raoResult.getActivatedRangeActionsDuringState(preventiveState).isEmpty()) {
                raoResult.getOptimizedSetPointsOnState(preventiveState).entrySet().forEach(entry ->
                    rangeActionActivationResult.putResult(entry.getKey(), preventiveState, entry.getValue())
                );
            }
            return new RemedialActionActivationResultImpl(rangeActionActivationResult, new NetworkActionsResultImpl(networkActionsActivated));
        }

        // Get all the network
        crac.getStates().stream()
            .filter(state -> state.getInstant().getKind().ordinal() <= instantKind.ordinal())
            .forEach(state -> {
                if (state.getInstant().getKind().ordinal() <= instantKind.ordinal()) {
                    networkActionsActivated.put(state, raoResult.getActivatedNetworkActionsDuringState(state));
                    if (!raoResult.getActivatedRangeActionsDuringState(state).isEmpty()) {
                        raoResult.getOptimizedSetPointsOnState(state).entrySet().forEach(entry ->
                            rangeActionActivationResult.putResult(entry.getKey(), state, entry.getValue())
                        );
                    }
                }
            });
        NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(networkActionsActivated);
        return new RemedialActionActivationResultImpl(rangeActionActivationResult, networkActionsResult);
    }

    private static RaoInput createFilteredRaoInput(RaoInput raoInput, Crac filteredCrac) {
        return RaoInput.build(raoInput.getNetwork(), filteredCrac)
            .withPerimeter(raoInput.getPerimeter())
            .withGlskProvider(raoInput.getGlskProvider())
            .withRefProg(raoInput.getReferenceProgram())
            .withNetworkVariantId(raoInput.getNetworkVariantId())
            .build();
    }

    public static Crac copyCrac(Crac crac, Network network) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonExport().exportData(crac, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return new JsonImport().importData(inputStream, new CracCreationParameters(), network).getCrac();
    }

    // Caution: We might remove CNECs associated to RAs with onConstraint usageRule
    public static void removeFlowCnecsFromCrac(Crac crac, Collection<FlowCnec> flowCnecsToKeep) {
        List<FlowCnec> flowCnecsToRemove = crac.getFlowCnecs().stream().filter(fc -> !flowCnecsToKeep.contains(fc)).toList();
        // Remove FlowCNECs
        Set<String> flowCnecsToRemoveIds = new HashSet<>();
        flowCnecsToRemove.forEach(cnec -> flowCnecsToRemoveIds.add(cnec.getId()));
        crac.removeFlowCnecs(flowCnecsToRemoveIds);
    }

    private static void applyOptimalPreventiveRemedialActions(Network networkCopy, State state, RaoResult raoResult) {
        raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> rangeAction.apply(networkCopy, raoResult.getOptimizedSetPointOnState(state, rangeAction)));
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(networkCopy));
    }

    private static Future<ObjectiveFunctionResult> generatePreviousObjectiveFunctionResult(ToolProvider toolProvider,
                                                                           RaoInput raoInput,
                                                                           RaoParameters raoParameters,
                                                                           Set<FlowCnec> flowCnecs,
                                                                           PrePerimeterResult initialFlowResult,
                                                                           Future<PrePerimeterResult> previousPrePerimeterResult,
                                                                           RemedialActionActivationResult remedialActionsResult,
                                                                           InstantKind instantKind) {

        Crac crac = raoInput.getCrac();
        return Executors.newSingleThreadExecutor().submit(() -> {
            ObjectiveFunction objectiveFunction = ObjectiveFunction.build(flowCnecs,
                toolProvider.getLoopFlowCnecs(flowCnecs),
                initialFlowResult,
                previousPrePerimeterResult.get().getFlowResult(),
                new StateTree(crac).getOperatorsNotSharingCras(),
                raoParameters,
                crac.getStates().stream().filter(state -> state.getInstant().getKind().ordinal() <= instantKind.ordinal()).collect(Collectors.toSet())
            );

            return objectiveFunction.evaluate(
                previousPrePerimeterResult.get().getFlowResult(),
                remedialActionsResult
            );
        });
    }

    private static AppliedRemedialActions createAppliedRemedialActions(Crac crac, RaoResult raoResult, InstantKind instantKind) {
        if (raoResult instanceof OneStateOnlyRaoResultImpl) {
            return new AppliedRemedialActions();
        }
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        crac.getStates().stream().filter(state -> state.getInstant().getKind().ordinal() > 1 && state.getInstant().getKind().ordinal() <= instantKind.ordinal()).forEach(state -> {
                appliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
                if (!raoResult.getActivatedRangeActionsDuringState(state).isEmpty()) {
                    appliedRemedialActions.addAppliedRangeActions(state, raoResult.getOptimizedSetPointsOnState(state));
                }
            }
        );
        return appliedRemedialActions;
    }

}
