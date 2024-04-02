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
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.craciojson.JsonExport;
import com.powsybl.openrao.data.craciojson.JsonImport;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

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
        return launchFilteredRao(raoInput, parameters, targetEndInstant);
    }

    private CompletableFuture<RaoResult> launchFilteredRao(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {

        try {
            // 1. Retrieve input data
            Crac crac = raoInput.getCrac();
            Collection<String> initialNetworkVariants = new HashSet<>(raoInput.getNetwork().getVariantManager().getVariantIds());
            String startingVariant = raoInput.getNetwork().getVariantManager().getWorkingVariantId();

            System.out.println("**************************INITIAL SENSITIVITY*******************************");
            ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, parameters);

            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                raoInput.getCrac().getFlowCnecs(),
                raoInput.getCrac().getRangeActions(),
                parameters,
                toolProvider);
            OpenRaoLogger logger = new RaoBusinessLogs();

            // 3. Run RA-free RAO and get 20 worst CNECs
            PrePerimeterResult initialResult = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac());
            PrePerimeterResult ofResult = initialResult;
            RaoLogger.logMostLimitingElementsResults(logger, initialResult, parameters.getObjectiveFunctionParameters().getType(), 5);
            //computeAvailableRangeActions(initialResult, crac, network, parameters);

            Set<FlowCnec> worstCnecs = new HashSet<>();
            FlowCnec worstCnec;
            FastRaoResultImpl raoResult;

            com.powsybl.openrao.data.cracapi.Instant lastInstant = raoInput.getCrac().getLastInstant();
            AbstractNetworkPool networkPool = AbstractNetworkPool.create(raoInput.getNetwork(), raoInput.getNetworkVariantId(), 3, true);
            do {
                worstCnecs.addAll(ofResult.getMostLimitingElements(20));
                worstCnecs.addAll(getUnsecureFunctionalCnecs(ofResult, parameters.getObjectiveFunctionParameters().getType().getUnit()));
                worstCnecs.addAll(getCostlyVirtualCnecs(ofResult));
                worstCnecs.add(getWorstPreventiveCnec(ofResult));
                cleanVariants(raoInput.getNetwork(), initialNetworkVariants);
                // run rao with filtered cnecs and rerun the sensi with all cnecs and applied ras
                raoResult = runFilteredRao(raoInput, parameters, targetEndInstant, worstCnecs, toolProvider, initialResult, networkPool);
                ofResult = raoResult.getAppropriateResult(lastInstant);
                RaoLogger.logMostLimitingElementsResults(logger, ofResult, parameters.getObjectiveFunctionParameters().getType(), 5);
                logVirtualCosts(logger, initialResult, ofResult);
                worstCnec = ofResult.getMostLimitingElements(1).get(0);
            } while (!(worstCnecs.contains(worstCnec) && worstCnecs.containsAll(getCostlyVirtualCnecs(ofResult))));

            return CompletableFuture.completedFuture(raoResult);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void logVirtualCosts(OpenRaoLogger logger, PrePerimeterResult initialResult, PrePerimeterResult ofResult) {
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

    private void cleanVariants(Network network, Collection<String> initialNetworkVariants) {
        VariantManager variantManager = network.getVariantManager();
        Set<String> variantsToRemove = new HashSet<>();
        variantManager.getVariantIds().stream()
            .filter(id -> !initialNetworkVariants.contains(id))
            .forEach(variantsToRemove::add);
        variantsToRemove.forEach(variantManager::removeVariant);
    }

    private FlowCnec getWorstPreventiveCnec(ObjectiveFunctionResult ofResult) {
        List<FlowCnec> orderedCnecs = ofResult.getMostLimitingElements(Integer.MAX_VALUE);
        return orderedCnecs.stream().filter(cnec -> cnec.getState().isPreventive()).findFirst().orElse(
            ofResult.getObjectiveFunction().getFlowCnecs().stream().filter(flowCnec -> flowCnec.getState().isPreventive()).findFirst().orElseThrow()
        );
    }

    private Set<FlowCnec> getUnsecureFunctionalCnecs(PrePerimeterResult prePerimeterResult, Unit unit) {
        List<FlowCnec> orderedCnecs = prePerimeterResult.getMostLimitingElements(1000);
        Set<FlowCnec> flowCnecs = new HashSet<>();
        for (FlowCnec cnec : orderedCnecs) {
            if (prePerimeterResult.getMargin(cnec, unit) < 5) {
                flowCnecs.add(cnec);
            }
        }
        return flowCnecs;
    }

    private Set<FlowCnec> getCostlyVirtualCnecs(ObjectiveFunctionResult ofResult) {
        Set<FlowCnec> flowCnecs = new HashSet<>();
        ofResult.getVirtualCostNames().forEach(name -> flowCnecs.addAll(ofResult.getCostlyElements(name, Integer.MAX_VALUE)));
        return flowCnecs;
    }

    private FastRaoResultImpl runFilteredRao(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, Set<FlowCnec> flowCnecsToKeep, ToolProvider toolProvider, PrePerimeterResult initialResult, AbstractNetworkPool networkPool) throws IOException, InterruptedException {
        Crac crac = raoInput.getCrac();
        // 4. Filter CRAC to only keep the worst CNECs
        Crac filteredCrac = copyCrac(crac);
        removeFlowCnecsFromCrac(filteredCrac, flowCnecsToKeep);
        // 5. Run filtered RAO
        System.out.println("**************************FILTERED RAO*******************************");
        RaoInput filteredRaoInput = createFilteredRaoInput(raoInput, filteredCrac);
        RaoResult raoResult;
        try {
            raoResult = new CastorFullOptimization(filteredRaoInput, parameters, targetEndInstant).run().get();
            List<List<String>> predefinedCombinations = parameters.getTopoOptimizationParameters().getPredefinedCombinations();
            predefinedCombinations.add(
                raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).stream()
                    .map(Identifiable::getId)
                    .toList()
            );
            parameters.getTopoOptimizationParameters().setPredefinedCombinations(predefinedCombinations);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        String finalVariantId = raoInput.getNetwork().getVariantManager().getWorkingVariantId();
        raoInput.getNetwork().getVariantManager().setWorkingVariant(raoInput.getNetworkVariantId());
        // 6. Apply / Force optimal RAs found on filter RAO
        // 7. Run RAO with applied/forced RAs
        System.out.println("**************************FULL SENSI WITH RAS*******************************");
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
                    postPraSensi.set(runBasedOnInitialAndPrePerimResults(toolProvider, raoInput, networkCopy, parameters, crac.getFlowCnecs(), new AppliedRemedialActions(), initialResult, initialResult));
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
        networkPool.submit(() -> {
            try {
                if (anyActionActivatedDuringInstantKind(raoResult, InstantKind.AUTO, crac)) {
                    Network networkCopy = networkPool.getAvailableNetwork();
                    applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                    AppliedRemedialActions appliedAutoRemedialActions = createAutoAppliedRemedialActionsFromRaoResult(filteredCrac, raoResult);
                    postAraSensi.set(runBasedOnInitialAndPrePerimResults(toolProvider, raoInput, networkCopy, parameters, crac.getFlowCnecs(), appliedAutoRemedialActions, initialResult, postPraSensi.get()));
                    networkPool.releaseUsedNetwork(networkCopy);
                    autoSemaphore.release();
                } else {
                    preventiveSemaphore.acquire();
                    postAraSensi.set(postPraSensi.get());
                    preventiveSemaphore.release();
                    autoSemaphore.release();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        curativeSemaphore.acquire();
        if (anyActionActivatedDuringInstantKind(raoResult, InstantKind.CURATIVE, crac)) {
            networkPool.submit(() -> {
                try {
                    Network networkCopy = networkPool.getAvailableNetwork();
                    applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
                    AppliedRemedialActions appliedRemedialActions = createAppliedRemedialActionsFromRaoResult(filteredCrac, raoResult);
                    postCraSensi.set(runBasedOnInitialAndPrePerimResults(toolProvider, raoInput, networkCopy, parameters, raoInput.getCrac().getFlowCnecs(), appliedRemedialActions, initialResult, postAraSensi.get()));
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

        preventiveSemaphore.acquire();
        autoSemaphore.acquire();
        curativeSemaphore.acquire();
        raoInput.getNetwork().getVariantManager().setWorkingVariant(finalVariantId);
        return new FastRaoResultImpl(initialResult, postPraSensi.get(), postAraSensi.get(), postCraSensi.get(), raoResult, raoInput.getCrac());
    }

    private RaoInput createFilteredRaoInput(RaoInput raoInput, Crac filteredCrac) {
        return RaoInput.build(raoInput.getNetwork(), filteredCrac)
            .withPerimeter(raoInput.getPerimeter())
            .withGlskProvider(raoInput.getGlskProvider())
            .withRefProg(raoInput.getReferenceProgram())
            .withNetworkVariantId(raoInput.getNetworkVariantId())
            .build();
    }

    public static Crac copyCrac(Crac crac) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonExport().exportCrac(crac, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return new JsonImport().importCrac(inputStream);
    }

    public static void removeFlowCnecsFromCrac(Crac crac, Collection<FlowCnec> flowCnecsToKeep) {
        List<FlowCnec> flowCnecsToRemove = crac.getFlowCnecs().stream().filter(fc -> !flowCnecsToKeep.contains(fc)).toList();
        // Remove FlowCNECs
        Set<String> flowCnecsToRemoveIds = new HashSet<>();
        flowCnecsToRemove.forEach(cnec -> flowCnecsToRemoveIds.add(cnec.getId()));
        crac.removeFlowCnecs(flowCnecsToRemoveIds);

        //TODO: remove associated on constraint usage rules
    }

    private void applyOptimalPreventiveRemedialActions(Network networkCopy, State state, RaoResult raoResult) {
        raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> rangeAction.apply(networkCopy, raoResult.getOptimizedSetPointOnState(state, rangeAction)));
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(networkCopy));
    }

    private AppliedRemedialActions createAppliedRemedialActionsFromRaoResult(Crac crac, RaoResult raoResult) {
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

    private AppliedRemedialActions createAutoAppliedRemedialActionsFromRaoResult(Crac crac, RaoResult raoResult) {
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

    private PrePerimeterResult runBasedOnInitialAndPrePerimResults(ToolProvider toolProvider,
                                                                   RaoInput raoInput,
                                                                   Network network,
                                                                   RaoParameters raoParameters,
                                                                   Set<FlowCnec> flowCnecs,
                                                                   AppliedRemedialActions appliedRemedialActions,
                                                                   PrePerimeterResult initialFlowResult,
                                                                   PrePerimeterResult prePerimeterResult) {
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

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(flowCnecs, toolProvider.getLoopFlowCnecs(flowCnecs), initialFlowResult, prePerimeterResult, initialFlowResult, crac, new StateTree(crac).getOperatorsNotSharingCras(), raoParameters);

        return runAndGetResult(network, sensitivityComputer, objectiveFunction, crac.getRangeActions());
    }

    private PrePerimeterResult runAndGetResult(Network network, SensitivityComputer sensitivityComputer, ObjectiveFunction objectiveFunction, Set<RangeAction<?>> rangeActions) {
        sensitivityComputer.compute(network);
        FlowResult flowResult = sensitivityComputer.getBranchResult(network);
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();
        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, rangeActions);
        RangeActionActivationResult rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityResult.getSensitivityStatus());
        return new PrePerimeterSensitivityResultImpl(
            flowResult,
            sensitivityResult,
            rangeActionSetpointResult,
            objectiveFunctionResult
        );
    }

    private boolean anyActionActivatedDuringInstantKind(RaoResult raoResult, InstantKind instantKind, Crac crac) {
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
