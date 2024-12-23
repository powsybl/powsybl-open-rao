/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.faorao;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.json.JsonExport;
import com.powsybl.openrao.data.crac.io.json.JsonImport;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorFullOptimization;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
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
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;
    private static final int NUMBER_OF_CNECS_TO_ADD = 20;
    private static final boolean ADD_UNSECURE_CNECS = false;
    private static final double MARGIN_LIMIT = 5;
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

        try {
            // 1. Retrieve input data
            Crac crac = raoInput.getCrac();
            Collection<String> initialNetworkVariants = new HashSet<>(raoInput.getNetwork().getVariantManager().getVariantIds());

            ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, parameters);

            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                crac.getFlowCnecs(),
                crac.getRangeActions(),
                parameters,
                toolProvider);

            // 3. Run initial sensi (for initial values, and to know which cnecs to put in the first rao)
            PrePerimeterResult initialResult = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac());

            if (initialResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
                BUSINESS_LOGS.error("Initial sensitivity analysis failed");
                return new FailedRaoResultImpl("Initial sensitivity analysis failed");
            }

            RaoLogger.logSensitivityAnalysisResults("Initial sensitivity analysis: ",
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                RemedialActionActivationResultImpl.empty(initialResult),
                initialResult,
                parameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            PrePerimeterResult stepResult = initialResult;
            //computeAvailableRangeActions(initialResult, crac, network, parameters);

            FlowCnec worstCnec;
            FastRaoResultImpl raoResult;

            com.powsybl.openrao.data.crac.api.Instant lastInstant = raoInput.getCrac().getLastInstant();
            AbstractNetworkPool networkPool = AbstractNetworkPool.create(raoInput.getNetwork(), raoInput.getNetworkVariantId(), 3, true);
            int counter = 1;
            do {
                addWorstCnecs(consideredCnecs, NUMBER_OF_CNECS_TO_ADD, stepResult);
                if (ADD_UNSECURE_CNECS) {
                    consideredCnecs.addAll(getUnsecureFunctionalCnecs(stepResult, parameters.getObjectiveFunctionParameters().getType().getUnit()));
                }
                consideredCnecs.addAll(getCostlyVirtualCnecs(stepResult));
                consideredCnecs.add(getWorstPreventiveCnec(stepResult, crac));
                cleanVariants(raoInput.getNetwork(), initialNetworkVariants);

                raoResult = runFilteredRao(raoInput, parameters, targetEndInstant, consideredCnecs, toolProvider, initialResult, networkPool, counter);
                stepResult = raoResult.getAppropriateResult(lastInstant);

                RaoLogger.logObjectifFunctionResult(String.format("Iteration %d: sensitivity analysis: ", counter),
                    stepResult, //TODO: Find the right remedialActionActivationResult if we want to use costly objective function not needed otherwise
                    stepResult,
                    parameters,
                    NUMBER_LOGGED_ELEMENTS_DURING_RAO);

                worstCnec = stepResult.getMostLimitingElements(1).get(0);
                counter++;
            } while (!(consideredCnecs.contains(worstCnec) && consideredCnecs.containsAll(getCostlyVirtualCnecs(stepResult))));
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);

            RaoLogger.logObjectifFunctionResult("Final Result: ",
                stepResult, //TODO: Find the right remedialActionActivationResult if we want to use costly objective function not needed otherwise
                stepResult,
                parameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            return raoResult;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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

    private static void cleanVariants(Network network, Collection<String> initialNetworkVariants) {
        VariantManager variantManager = network.getVariantManager();
        Set<String> variantsToRemove = new HashSet<>();
        variantManager.getVariantIds().stream()
            .filter(id -> !initialNetworkVariants.contains(id))
            .forEach(variantsToRemove::add);
        variantsToRemove.forEach(variantManager::removeVariant);
    }

    private static FlowCnec getWorstPreventiveCnec(ObjectiveFunctionResult ofResult, Crac crac) {
        List<FlowCnec> orderedCnecs = ofResult.getMostLimitingElements(Integer.MAX_VALUE);
        return orderedCnecs.stream().filter(cnec -> cnec.getState().isPreventive()).findFirst().orElse(
            // If only MNECs are present previous list will be empty
            crac.getFlowCnecs(crac.getPreventiveState()).stream().findFirst().orElseThrow()
        );
    }

    private static Set<FlowCnec> getUnsecureFunctionalCnecs(PrePerimeterResult prePerimeterResult, Unit unit) {
        List<FlowCnec> orderedCnecs = prePerimeterResult.getMostLimitingElements(Integer.MAX_VALUE);
        Set<FlowCnec> flowCnecs = new HashSet<>();
        for (FlowCnec cnec : orderedCnecs) {
            if (prePerimeterResult.getMargin(cnec, unit) < MARGIN_LIMIT) {
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

    private static FastRaoResultImpl runFilteredRao(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, Set<FlowCnec> flowCnecsToKeep, ToolProvider toolProvider, PrePerimeterResult initialResult, AbstractNetworkPool networkPool, int counter) throws IOException, InterruptedException {
        Crac crac = raoInput.getCrac();
        // 4. Filter CRAC to only keep the worst CNECs
        Crac filteredCrac = copyCrac(crac, raoInput.getNetwork());
        removeFlowCnecsFromCrac(filteredCrac, flowCnecsToKeep);

        BUSINESS_LOGS.info("***** Iteration {}: Run filtered RAO [start]", counter);

        RaoInput filteredRaoInput = createFilteredRaoInput(raoInput, filteredCrac);
        RaoResult raoResult;
        try {
            raoResult = new CastorFullOptimization(filteredRaoInput, parameters, targetEndInstant).run().get();
            List<String> preventiveNetworkActions = raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).stream()
                .map(Identifiable::getId)
                .toList();
            if (preventiveNetworkActions.size() >= 2) {
                List<List<String>> predefinedCombinations = parameters.getTopoOptimizationParameters().getPredefinedCombinations();
                if (!preventiveNetworkActions.contains(preventiveNetworkActions)) {
                    predefinedCombinations.add(preventiveNetworkActions);
                }
                parameters.getTopoOptimizationParameters().setPredefinedCombinations(predefinedCombinations);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        BUSINESS_LOGS.info("***** Iteration {}: Run filtered RAO [end]", counter);

        String finalVariantId = raoInput.getNetwork().getVariantManager().getWorkingVariantId();
        raoInput.getNetwork().getVariantManager().setWorkingVariant(raoInput.getNetworkVariantId());
        // 6. Apply / Force optimal RAs found on filter RAO
        // 7. Run RAO with applied/forced RAs
        BUSINESS_LOGS.info("***** Iteration {}: Run full sensitivity analysis [start]", counter);
        //TODO: Semaphores won't quite work with the current implementation to parallelize the loadflows:
        // eg if we applied PRAs, we need to give the post PRA result to the autoSensi etc, so the sensitivities are not parallelized
        // Some options:
        // - have a runBasedOnInitialAndPrePerimResults that takes a semaphore to be able to run a sensi, and then wait for the semaphore to build the result
        // - ideally use the security analysis for faster runs anyways => rewrite the method completely
        // (for now option 1)
        // Solution selected : Do the three Sensitivity computation in parallel, wait for all of them to finish then compute objective function sequentially

        AtomicReference<PrePerimeterResult> postPraSensi = new AtomicReference<>();
        AtomicReference<PrePerimeterResult> postAraSensi = new AtomicReference<>();
        AtomicReference<PrePerimeterResult> postCraSensi = new AtomicReference<>();

        ForkJoinTask<?> task1 = networkPool.submit(() -> {
            try {
                Network networkCopy = networkPool.getAvailableNetwork();
                applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                postPraSensi.set(runBasedOnInitialResults(toolProvider, raoInput, networkCopy, parameters, crac.getFlowCnecs(), new AppliedRemedialActions(), initialResult));
                networkPool.releaseUsedNetwork(networkCopy);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // TODO: Use list of tasks like everywhere else in the codebase
        ForkJoinTask<?> task2 = networkPool.submit(() -> {
            try {
                Network networkCopy = networkPool.getAvailableNetwork();
                applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                AppliedRemedialActions appliedAutoRemedialActions = createAutoAppliedRemedialActionsFromRaoResult(filteredCrac, raoResult);
                postAraSensi.set(runBasedOnInitialResults(toolProvider, raoInput, networkCopy, parameters, crac.getFlowCnecs(), appliedAutoRemedialActions, initialResult));
                networkPool.releaseUsedNetwork(networkCopy);

            } catch (OpenRaoException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        ForkJoinTask<?> task3 = networkPool.submit(() -> {
            try {
                Network networkCopy = networkPool.getAvailableNetwork();
                applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                AppliedRemedialActions appliedRemedialActions = createAppliedRemedialActionsFromRaoResult(filteredCrac, raoResult);
                postCraSensi.set(runBasedOnInitialResults(toolProvider, raoInput, networkCopy, parameters, raoInput.getCrac().getFlowCnecs(), appliedRemedialActions, initialResult));
                networkPool.releaseUsedNetwork(networkCopy);
            } catch (InterruptedException | OpenRaoException e) {
                throw new RuntimeException(e);
            }
        });

        task1.join();
        task2.join();
        task3.join();

        raoInput.getNetwork().getVariantManager().setWorkingVariant(finalVariantId);

        postPraSensi.set(getCompletePrePerimeterSensitivityResultImpl(toolProvider, raoInput, parameters, raoInput.getCrac().getFlowCnecs(), initialResult, initialResult, postPraSensi.get()));
        postAraSensi.set(getCompletePrePerimeterSensitivityResultImpl(toolProvider, raoInput, parameters, raoInput.getCrac().getFlowCnecs(), initialResult, postPraSensi.get(), postAraSensi.get()));
        postCraSensi.set(getCompletePrePerimeterSensitivityResultImpl(toolProvider, raoInput, parameters, raoInput.getCrac().getFlowCnecs(), initialResult, postAraSensi.get(), postCraSensi.get()));

        BUSINESS_LOGS.info("***** Iteration {}: Run full sensitivity analysis [end]", counter);

        return new FastRaoResultImpl(initialResult, postPraSensi.get(), postAraSensi.get(), postCraSensi.get(), raoResult, raoInput.getCrac());
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
        return new JsonImport().importData(inputStream, new CracCreationParameters(), network, null).getCrac();
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

    private static AppliedRemedialActions createAppliedRemedialActionsFromRaoResult(Crac crac, RaoResult raoResult) {
        if (raoResult instanceof OneStateOnlyRaoResultImpl) {
            return new AppliedRemedialActions();
        }
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        crac.getStates().stream().filter(state -> !state.isPreventive() && !state.getInstant().getKind().equals(InstantKind.OUTAGE)).forEach(state -> {
                appliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
                if (!raoResult.getActivatedRangeActionsDuringState(state).isEmpty()) {
                    appliedRemedialActions.addAppliedRangeActions(state, raoResult.getOptimizedSetPointsOnState(state));
                }
            }
        );
        return appliedRemedialActions;
    }

    private static AppliedRemedialActions createAutoAppliedRemedialActionsFromRaoResult(Crac crac, RaoResult raoResult) {
        if (raoResult instanceof OneStateOnlyRaoResultImpl) {
            return new AppliedRemedialActions();
        }
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();

        crac.getStates().stream().filter(state -> state.getInstant().getKind().equals(InstantKind.AUTO)).forEach(state -> {
            appliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
            appliedRemedialActions.addAppliedRangeActions(state, raoResult.getOptimizedSetPointsOnState(state));
        });
        return appliedRemedialActions;
    }

    private static PrePerimeterSensitivityResultImpl runBasedOnInitialResults(ToolProvider toolProvider,
                                                                                         RaoInput raoInput,
                                                                                         Network network,
                                                                                         RaoParameters raoParameters,
                                                                                         Set<FlowCnec> flowCnecs,
                                                                                         AppliedRemedialActions appliedRemedialActions,
                                                                                         PrePerimeterResult initialFlowResult) throws InterruptedException {
        Crac crac = raoInput.getCrac();
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create()
            .withToolProvider(toolProvider)
            .withCnecs(flowCnecs)
            .withRangeActions(crac.getRangeActions())
            .withOutageInstant(raoInput.getCrac().getOutageInstant());

        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            if (raoParameters.getExtension(LoopFlowParametersExtension.class).getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
            } else {
                sensitivityComputerBuilder.withCommercialFlowsResults(initialFlowResult);
            }
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (raoParameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
            } else {
                sensitivityComputerBuilder.withPtdfsResults(initialFlowResult);
            }
        }
        if (appliedRemedialActions != null) {
            // for 2nd preventive initial sensi
            sensitivityComputerBuilder.withAppliedRemedialActions(appliedRemedialActions);
        }
        SensitivityComputer sensitivityComputer = sensitivityComputerBuilder.build();
        sensitivityComputer.compute(network);

        FlowResult flowResult = sensitivityComputer.getBranchResult(network);
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();
        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, crac.getRangeActions());

        return new PrePerimeterSensitivityResultImpl(
            flowResult,
            sensitivityResult,
            rangeActionSetpointResult,
            null //complete later
        );
    }

    private static PrePerimeterResult getCompletePrePerimeterSensitivityResultImpl(ToolProvider toolProvider,
                                                                                   RaoInput raoInput,
                                                                                   RaoParameters raoParameters,
                                                                                   Set<FlowCnec> flowCnecs,
                                                                                   PrePerimeterResult initialFlowResult,
                                                                                   PrePerimeterResult prePerimeterResult,
                                                                                   PrePerimeterResult currentPrePerimeterResult) {

        Crac crac = raoInput.getCrac();
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(flowCnecs,
            toolProvider.getLoopFlowCnecs(flowCnecs),
            initialFlowResult,
            prePerimeterResult.getFlowResult(),
            new StateTree(crac).getOperatorsNotSharingCras(),
            raoParameters,
            Set.of()); //TODO: To complete later if we want to use costly objective function not needed otherwise

        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(
            currentPrePerimeterResult.getFlowResult(),
            null //TODO: Find the right remedialActionActivationResult if we want to use costly objective function not needed otherwise
        );

        return new PrePerimeterSensitivityResultImpl(
            currentPrePerimeterResult.getFlowResult(),
            currentPrePerimeterResult.getSensitivityResult(),
            currentPrePerimeterResult.getRangeActionSetpointResult(),
            objectiveFunctionResult
        );
    }

    private static boolean anyActionActivatedDuringInstantKind(RaoResult raoResult, InstantKind instantKind, Crac crac) {
        if (instantKind.equals(InstantKind.PREVENTIVE)) {
            State preventiveState = crac.getPreventiveState();
            return !(raoResult.getActivatedNetworkActionsDuringState(preventiveState).isEmpty() && raoResult.getActivatedRangeActionsDuringState(preventiveState).isEmpty());
        }
        if (raoResult instanceof OneStateOnlyRaoResultImpl) {
            return false;
        }

        return crac.getStates().stream()
            .filter(state -> state.getInstant().getKind().equals(instantKind))
            .anyMatch(state ->
                !(raoResult.getActivatedNetworkActionsDuringState(state).isEmpty() && raoResult.getActivatedRangeActionsDuringState(state).isEmpty())
            );
    }
}
