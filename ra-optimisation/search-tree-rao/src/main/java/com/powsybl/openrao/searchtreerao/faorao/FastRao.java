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
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.json.JsonExport;
import com.powsybl.openrao.data.crac.io.json.JsonImport;
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
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;
import org.apache.commons.lang3.exception.ExceptionUtils;

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
        int numberOfCnecsToAdd = 20; //to add in param

        try {
            // 1. Retrieve input data
            Crac crac = raoInput.getCrac();
            Collection<String> initialNetworkVariants = new HashSet<>(raoInput.getNetwork().getVariantManager().getVariantIds());

            System.out.println("**************************INITIAL SENSITIVITY*******************************");
            ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, parameters);

            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                    crac.getFlowCnecs(),
                    crac.getRangeActions(),
                    parameters,
                    toolProvider);
            OpenRaoLogger logger = new RaoBusinessLogs();

            // 3. Run initial sensi (for initial values, and to know which cnecs to put in the first rao)
            PrePerimeterResult initialResult = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac());
            PrePerimeterResult ofResult = initialResult; //meaning of of ?
            RaoLogger.logMostLimitingElementsResults(logger, initialResult, parameters.getObjectiveFunctionParameters().getType(), 5);
            //computeAvailableRangeActions(initialResult, crac, network, parameters);

            FlowCnec worstCnec;
            FastRaoResultImpl raoResult;

            // standard de faire import comme ça ?
            com.powsybl.openrao.data.crac.api.Instant lastInstant = raoInput.getCrac().getLastInstant();
            AbstractNetworkPool networkPool = AbstractNetworkPool.create(raoInput.getNetwork(), raoInput.getNetworkVariantId(), 3, true);
            do {
                addWorstCnecs(consideredCnecs, numberOfCnecsToAdd, ofResult);
                //consideredCnecs.addAll(getUnsecureFunctionalCnecs(ofResult, parameters.getObjectiveFunctionParameters().getType().getUnit()));
                consideredCnecs.addAll(getCostlyVirtualCnecs(ofResult));
                consideredCnecs.add(getWorstPreventiveCnec(ofResult));
                cleanVariants(raoInput.getNetwork(), initialNetworkVariants);
                // run rao with filtered cnecs and rerun the sensi with all cnecs and applied ras
                raoResult = runFilteredRao(raoInput, parameters, targetEndInstant, consideredCnecs, toolProvider, initialResult, networkPool);
                ofResult = raoResult.getAppropriateResult(lastInstant);
                RaoLogger.logMostLimitingElementsResults(logger, ofResult, parameters.getObjectiveFunctionParameters().getType(), 5);
                logVirtualCosts(logger, initialResult, ofResult);
                worstCnec = ofResult.getMostLimitingElements(1).get(0);
            } while (!(consideredCnecs.contains(worstCnec) && consideredCnecs.containsAll(getCostlyVirtualCnecs(ofResult))));
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
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

    private static void logVirtualCosts(OpenRaoLogger logger, PrePerimeterResult initialResult, PrePerimeterResult ofResult) {
        if (ofResult.getVirtualCost() > 1e-6) {
            ofResult.getVirtualCostNames().forEach(name -> ofResult.getCostlyElements(name, 10).forEach(flowCnec -> {
                String stringBuilder = name +
                        " costly element " +
                        flowCnec.getId() +
                        "; margin before RAO " +
                        initialResult.getMargin(flowCnec, Unit.MEGAWATT) +
                        "MW; margin after RAO " +
                        ofResult.getMargin(flowCnec, Unit.MEGAWATT) +
                        "MW.";
                logger.info(stringBuilder);
            }));
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

    private static FlowCnec getWorstPreventiveCnec(ObjectiveFunctionResult ofResult) {
        List<FlowCnec> orderedCnecs = ofResult.getMostLimitingElements(Integer.MAX_VALUE);
        return orderedCnecs.stream().filter(cnec -> cnec.getState().isPreventive()).findFirst().orElse(
                ofResult.getObjectiveFunction().getFlowCnecs().stream().filter(flowCnec -> flowCnec.getState().isPreventive()).findFirst().orElseThrow()
        );
    }

    private static Set<FlowCnec> getUnsecureFunctionalCnecs(PrePerimeterResult prePerimeterResult, Unit unit) {
        List<FlowCnec> orderedCnecs = prePerimeterResult.getMostLimitingElements(1000);
        Set<FlowCnec> flowCnecs = new HashSet<>();
        for (FlowCnec cnec : orderedCnecs) {
            if (prePerimeterResult.getMargin(cnec, unit) < 5) {
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

    private static FastRaoResultImpl runFilteredRao(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, Set<FlowCnec> flowCnecsToKeep, ToolProvider toolProvider, PrePerimeterResult initialResult, AbstractNetworkPool networkPool) throws IOException, InterruptedException {
        Crac crac = raoInput.getCrac();
        // 4. Filter CRAC to only keep the worst CNECs
        Crac filteredCrac = copyCrac(crac, raoInput.getNetwork());
        removeFlowCnecsFromCrac(filteredCrac, flowCnecsToKeep);
        // 5. Run filtered RAO
        System.out.println("**************************FILTERED RAO*******************************");
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

        String finalVariantId = raoInput.getNetwork().getVariantManager().getWorkingVariantId();
        raoInput.getNetwork().getVariantManager().setWorkingVariant(raoInput.getNetworkVariantId());
        // 6. Apply / Force optimal RAs found on filter RAO
        // 7. Run RAO with applied/forced RAs
        // slide 13 found a new solution (apply RA found that will secure the first few CNEC then see if new usecure CNEC appear by doing a sensi computation on all the CNECs)
        System.out.println("**************************FULL SENSI WITH RAS*******************************");
        //TODO: Semaphores won't quite work with the current implementation to parallelize the loadflows:
        // eg if we applied PRAs, we need to give the post PRA result to the autoSensi etc, so the sensitivities are not parallelized
        // Some options:
        // - have a runBasedOnInitialAndPrePerimResults that takes a semaphore to be able to run a sensi, and then wait for the semaphore to build the result
        // - ideally use the security analysis for faster runs anyways => rewrite the method completely
        // (for now option 1)

        // Initialize Preventive perimeter result as atomic reference why ?
        AtomicReference<PrePerimeterResult> postPraSensi = new AtomicReference<>();
        AtomicReference<PrePerimeterResult> postAraSensi = new AtomicReference<>();
        AtomicReference<PrePerimeterResult> postCraSensi = new AtomicReference<>();

        Semaphore preventiveSemaphore = new Semaphore(1);
        Semaphore autoSemaphore = new Semaphore(1);
        Semaphore curativeSemaphore = new Semaphore(1);

        preventiveSemaphore.acquire();
        if (anyActionActivatedDuringInstantKind(raoResult, InstantKind.PREVENTIVE, crac)) {
            networkPool.submit(() -> {
                try {
                    Network networkCopy = networkPool.getAvailableNetwork();
                    applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                    postPraSensi.set(runBasedOnInitialAndPrePerimResults(toolProvider, raoInput, networkCopy, parameters, crac.getFlowCnecs(), new AppliedRemedialActions(), initialResult, new AtomicReference<>(initialResult), new Semaphore(1)));
                    networkPool.releaseUsedNetwork(networkCopy);
                    preventiveSemaphore.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            postPraSensi.set(initialResult);
            preventiveSemaphore.release();
        }

        autoSemaphore.acquire();
        if (anyActionActivatedDuringInstantKind(raoResult, InstantKind.AUTO, crac)) {
            // TODO: Use list of tasks like everywhere else in the codebase

            ForkJoinTask<?> task = networkPool.submit(() -> {
                try {
                    Network networkCopy = networkPool.getAvailableNetwork();
                    applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                    AppliedRemedialActions appliedAutoRemedialActions = createAutoAppliedRemedialActionsFromRaoResult(filteredCrac, raoResult);
                    postAraSensi.set(runBasedOnInitialAndPrePerimResults(toolProvider, raoInput, networkCopy, parameters, crac.getFlowCnecs(), appliedAutoRemedialActions, initialResult, postPraSensi, preventiveSemaphore));
                    networkPool.releaseUsedNetwork(networkCopy);
                    autoSemaphore.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (OpenRaoException e) {
                    throw new OpenRaoException(e);
                }
            });

            try {
                task.get();
            } catch (OpenRaoException e) {
                //throw new RuntimeException(e);
                BUSINESS_LOGS.error("{} \n {}", e.getMessage(), ExceptionUtils.getStackTrace(e));
                return new FastRaoResultImpl(initialResult, postPraSensi.get(), postAraSensi.get(), postCraSensi.get(), raoResult, raoInput.getCrac());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        } else {
            preventiveSemaphore.acquire();
            postAraSensi.set(postPraSensi.get());
            preventiveSemaphore.release();
            autoSemaphore.release();
        }

        curativeSemaphore.acquire();
        if (anyActionActivatedDuringInstantKind(raoResult, InstantKind.CURATIVE, crac)) {
            networkPool.submit(() -> {
                try {
                    Network networkCopy = networkPool.getAvailableNetwork();
                    applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                    AppliedRemedialActions appliedRemedialActions = createAppliedRemedialActionsFromRaoResult(filteredCrac, raoResult);
                    postCraSensi.set(runBasedOnInitialAndPrePerimResults(toolProvider, raoInput, networkCopy, parameters, raoInput.getCrac().getFlowCnecs(), appliedRemedialActions, initialResult, postAraSensi, autoSemaphore));
                    networkPool.releaseUsedNetwork(networkCopy);
                    curativeSemaphore.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            autoSemaphore.acquire();
            postCraSensi.set(postAraSensi.get());
            autoSemaphore.release();
            curativeSemaphore.release();
        }

        curativeSemaphore.acquire();
        autoSemaphore.acquire();
        preventiveSemaphore.acquire();
        raoInput.getNetwork().getVariantManager().setWorkingVariant(finalVariantId);
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
        //how to get the cracCreationParameters from crac ?
        return new JsonImport().importData(inputStream, new CracCreationParameters(), network, null).getCrac();
    }

    public static void removeFlowCnecsFromCrac(Crac crac, Collection<FlowCnec> flowCnecsToKeep) {
        List<FlowCnec> flowCnecsToRemove = crac.getFlowCnecs().stream().filter(fc -> !flowCnecsToKeep.contains(fc)).toList();
        // Remove FlowCNECs
        Set<String> flowCnecsToRemoveIds = new HashSet<>();
        flowCnecsToRemove.forEach(cnec -> flowCnecsToRemoveIds.add(cnec.getId()));
        crac.removeFlowCnecs(flowCnecsToRemoveIds);

        //TODO: remove associated on constraint usage rules
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
            appliedRemedialActions.addAppliedRangeActions(state, raoResult.getOptimizedSetPointsOnState(state));
        });
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

            //Map<RangeAction<?>, Double> debug = raoResult.getOptimizedSetPointsOnState(state);
            // TODO : Issue here for test US 12.15.2.1 call get optimizedSetPointsOnState of a SkippedOptimizationResultImpl which lead to an error that is not handle
            //raoResult.getActivatedRangeActionsDuringState(state).forEach(l ->
              //  {appliedRemedialActions.addAppliedRangeActions(state, raoResult.getOptimizedSetPointsOnState(state));}
            //);

        });
        return appliedRemedialActions;
    }

    private static PrePerimeterResult runBasedOnInitialAndPrePerimResults(ToolProvider toolProvider,
                                                                          RaoInput raoInput,
                                                                          Network network,
                                                                          RaoParameters raoParameters,
                                                                          Set<FlowCnec> flowCnecs,
                                                                          AppliedRemedialActions appliedRemedialActions,
                                                                          PrePerimeterResult initialFlowResult,
                                                                          AtomicReference<PrePerimeterResult> prePerimeterResult,
                                                                          Semaphore semaphore) throws InterruptedException {
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

        semaphore.acquire();
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(flowCnecs,
                toolProvider.getLoopFlowCnecs(flowCnecs),
                initialFlowResult,
                prePerimeterResult.get().getFlowResult(),
                new StateTree(crac).getOperatorsNotSharingCras(),
                raoParameters);

        semaphore.release();

        FlowResult flowResult = sensitivityComputer.getBranchResult(network);
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();
        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, crac.getRangeActions());
        RangeActionActivationResult rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(flowResult);
        //(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityResult.getSensitivityStatus());
        return new PrePerimeterSensitivityResultImpl(
                flowResult,
                sensitivityResult,
                rangeActionSetpointResult,
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
