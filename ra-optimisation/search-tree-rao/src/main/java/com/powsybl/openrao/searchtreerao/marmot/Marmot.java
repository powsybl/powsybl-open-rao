/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.*;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.FastRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.logCost;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.*;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Marmot implements InterTemporalRaoProvider {

    private static final String INTER_TEMPORAL_RAO = "InterTemporalRao";
    private static final String VERSION = "1.0.0";

    @Override
    public CompletableFuture<InterTemporalRaoResult> run(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths, RaoParameters raoParameters) {
        // MEMORY ISSUES
        // Modifications done:
        // - use network paths for runTopologicalOptimization
        // - configure VM options with -Xmx16g
        // - reduce topologicalOptimizationResults size via FastRao
        // - continue reducing topologicalOptimizationResults by keeping only initial flows, applied RAs
        // TODO :
        // - reduce FastRaoResultImpl attributes to strictly necessary

        // Run independent RAOs to compute optimal preventive topological remedial actions
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [start]");
        TemporalData<Set<String>> consideredCnecs = new TemporalDataImpl<>();
        TemporalData<RaoResult> topologicalOptimizationResults = runTopologicalOptimization(interTemporalRaoInputWithNetworkPaths.getRaoInputs(), consideredCnecs, raoParameters);
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [end]");

        // Get the initial results from the various independent results to avoid recomputing them
        TemporalData<PrePerimeterResult> initialResults = buildInitialResults(topologicalOptimizationResults);
        TemporalData<RaoInput> raoInputsWithImportedNetworks = new TemporalDataImpl<>();
        // Import all the networks and create the InitialScenario variant
        interTemporalRaoInputWithNetworkPaths.getRaoInputs().getDataPerTimestamp().forEach((datetime, individualRaoInputWithNetworkPath) -> {
            Network network = Network.read(individualRaoInputWithNetworkPath.getPostIcsImportNetworkPath());
            RaoInput individualRaoInput = RaoInput
                .build(network, individualRaoInputWithNetworkPath.getCrac())
                .build();
            RaoUtil.initData(individualRaoInput, raoParameters);
            network.getVariantManager().cloneVariant(individualRaoInput.getNetworkVariantId(), "InitialScenario");
            raoInputsWithImportedNetworks.add(datetime, individualRaoInput);
        });
        InterTemporalRaoInput interTemporalRaoInput = new InterTemporalRaoInput(raoInputsWithImportedNetworks, interTemporalRaoInputWithNetworkPaths.getPowerGradients());

        // Apply preventive topological remedial actions
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Applying optimal topological actions on networks");
        applyPreventiveTopologicalActionsOnNetwork(interTemporalRaoInput.getRaoInputs(), topologicalOptimizationResults);

        // Get the curative ations applied in the individual results to be able to apply them during sensitivity computations
        TemporalData<AppliedRemedialActions> curativeRemedialActions = MarmotUtils.getAppliedRemedialActionsInCurative(interTemporalRaoInput.getRaoInputs(), topologicalOptimizationResults);

        // Create an objective function that takes into account ALL the cnecs
        ObjectiveFunction fullObjectiveFunction = buildGlobalObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), raoParameters);
        ObjectiveFunctionResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction);

        // if no inter-temporal constraints are defined, the results can be returned
        // TODO : Add intertemporal constraint check if none violated then return
        if (interTemporalRaoInputWithNetworkPaths.getPowerGradients().isEmpty()) {
            ObjectiveFunctionResult finalObjectiveFunctionResult = getIndependantRaoGlobalObjectiveFunctionResult(topologicalOptimizationResults, initialResults, raoInputsWithImportedNetworks, fullObjectiveFunction);
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] No inter-temporal constraint provided; no need to re-optimize range actions");
            return CompletableFuture.completedFuture(new InterTemporalRaoResultImpl(initialObjectiveFunctionResult, finalObjectiveFunctionResult, topologicalOptimizationResults));
        }

        // Create some variables that will be used in the MIP loops and will still be needed after the loop
        TemporalData<PrePerimeterResult> loadFlowResults;
        LinearOptimizationResult linearOptimizationResults;
        GlobalLinearOptimizationResult fullResults;
        do {
            // Clone the PostTopoScenario variant to make sure we work on a clean variant every time
            interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().values().forEach(raoInput -> {
                raoInput.getNetwork().getVariantManager().cloneVariant("PostTopoScenario", "PreMipScenario", true);
                raoInput.getNetwork().getVariantManager().setWorkingVariant("PreMipScenario");
            });

            // Run post topo sensitivity analysis on all timestamps ON CONSIDERED CNECS ONLY (which is why we do it every loop)
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [start]");
            TemporalData<PrePerimeterResult> postTopoResults = runAllInitialPrePerimeterSensitivityAnalysis(interTemporalRaoInput.getRaoInputs(), curativeRemedialActions, initialResults, consideredCnecs, raoParameters);
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [end]");

            // Build objective function with ONLY THE CONSIDERED CNECS
            ObjectiveFunction filteredObjectiveFunction = buildGlobalObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), consideredCnecs, raoParameters);
            TemporalData<NetworkActionsResult> preventiveTopologicalActions = getPreventiveTopologicalActions(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), topologicalOptimizationResults);

            // Create and iteratively solve MIP to find optimal range actions' set-points FOR THE CONSIDERED CNECS
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start]");
            linearOptimizationResults = optimizeLinearRemedialActions(interTemporalRaoInput, initialResults, postTopoResults, raoParameters, preventiveTopologicalActions, curativeRemedialActions, consideredCnecs, filteredObjectiveFunction);
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end]");

            // Compute the flows on ALL the cnecs to check if the worst cnecs have changed and were considered in the MIP or not
            loadFlowResults = applyActionsAndRunFullLoadflow(interTemporalRaoInput.getRaoInputs(), curativeRemedialActions, linearOptimizationResults, initialResults, raoParameters);

            // Create a global result with the flows on ALL cnecs and the actions applied during MIP
            TemporalData<RangeActionActivationResult> rangeActionActivationResultTemporalData = ((GlobalRangeActionActivationResult) linearOptimizationResults.getRangeActionActivationResult()).getRangeActionActivationPerTimestamp();
            fullResults = new GlobalLinearOptimizationResult(loadFlowResults, postTopoResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResultTemporalData, preventiveTopologicalActions, fullObjectiveFunction, LinearProblemStatus.OPTIMAL);

            logCost("[MARMOT] next iteration of MIP: ", fullResults, raoParameters, 10);
        } while (!shouldStop(loadFlowResults, consideredCnecs)); // Stop if the worst element of each TS has been considered during MIP

        // Merge topological and linear result
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");
        InterTemporalRaoResultImpl mergedRaoResults = mergeTopologicalAndLinearOptimizationResults(interTemporalRaoInput.getRaoInputs(), initialResults, initialObjectiveFunctionResult, fullResults, topologicalOptimizationResults, raoParameters);

        // Log initial and final results
        //logCost("[MARMOT] Before global linear optimization: ", initialLinearOptimizationResult, raoParameters, 10);
        logCost("[MARMOT] After global linear optimization: ", linearOptimizationResults, raoParameters, 10);

        LinearOptimizationResult finalLinearOptimizationResults = linearOptimizationResults;
        interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) ->
            exportUctNetwork(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(dateTime).orElseThrow(), raoInput, finalLinearOptimizationResults));

        return CompletableFuture.completedFuture(mergedRaoResults);
    }

    private boolean shouldStop(TemporalData<PrePerimeterResult> loadFlowResults, TemporalData<Set<String>> consideredCnecs) {
        AtomicBoolean shouldStop = new AtomicBoolean(true);
        // For every TS, for all the virtual costs, go through all the costly cnecs in order.
        // If the cnec has already been considered, go to the next virtual cost
        // If not, add it to the considered cnecs, set shouldStop to false, and go to the next cnec
        loadFlowResults.getTimestamps().forEach(timestamp -> {
            PrePerimeterResult loadFlowResult = loadFlowResults.getData(timestamp).orElseThrow();
            Set<String> cnecs = consideredCnecs.getData(timestamp).orElseThrow();
            loadFlowResult.getVirtualCostNames().forEach(vcName -> {
                for (FlowCnec cnec : loadFlowResult.getCostlyElements(vcName, Integer.MAX_VALUE)) {
                    if (cnecs.contains(cnec.getId())) {
                        break;
                    } else {
                        shouldStop.set(false);
                        cnecs.add(cnec.getId());
                    }
                }
            });
        });
        return shouldStop.get();
    }

    private static void exportUctNetwork(RaoInputWithNetworkPaths raoInputWithNetworkPaths, RaoInput raoInput, LinearOptimizationResult linearOptimizationResults) {
        // RAs have already been applied on network.
        // But injection RAs have been applied on fictitious generators created during import. Their setpoint
        // needs to be transposed on initial network's original generators.
        Network modifiedNetwork = raoInput.getNetwork();
        String initialNetworkPath = raoInputWithNetworkPaths.getInitialNetworkPath();
        Network initialNetwork = Network.read(initialNetworkPath);
        State preventiveState = raoInput.getCrac().getPreventiveState();

        linearOptimizationResults.getActivatedRangeActions(preventiveState)
            .forEach(rangeAction -> {
                if (rangeAction instanceof InjectionRangeAction) {
                    double optimizedSetpoint = linearOptimizationResults.getOptimizedSetpoint(rangeAction, preventiveState);
                    double initialSetpoint = ((InjectionRangeAction) rangeAction).getInitialSetpoint();
                    for (NetworkElement networkElement : rangeAction.getNetworkElements().stream().collect(Collectors.toSet())) {
                        String busId = modifiedNetwork.getGenerator(networkElement.getId()).getTerminal().getBusBreakerView().getBus().getId();
                        Bus busInInitialNetwork = initialNetwork.getBusBreakerView().getBus(busId);
                        // If no generator defined in initial network, create one
                        // For now, minimumPermissibleReactivePowerGeneration and maximumPermissibleReactivePowerGeneration are hardcoded to -999 and 999
                        // to prevent infinite values that generate a UCT writer crash. TODO : compute realistic values
                        String generatorId = busId + "_generator";
                        if (initialNetwork.getGenerator(generatorId) == null) {
                            Generator generator = busInInitialNetwork.getVoltageLevel().newGenerator()
                                .setBus(busId)
                                .setEnsureIdUnicity(true)
                                .setId(generatorId)
                                .setMaxP(999999)
                                .setMinP(0)
                                .setTargetP(0)
                                .setTargetQ(0)
                                .setTargetV(busInInitialNetwork.getVoltageLevel().getNominalV())
                                .setVoltageRegulatorOn(false)
                                .add();
                            generator.setFictitious(true);
                            generator.newMinMaxReactiveLimits().setMinQ(-999).setMaxQ(999).add();
                        }
                        for (Generator generator : busInInitialNetwork.getGenerators()) {
                            generator.setTargetP(generator.getTargetP()
                                + (optimizedSetpoint - initialSetpoint) * ((InjectionRangeAction) rangeAction).getInjectionDistributionKeys().get(networkElement));
                        }
                    }
                }
            });

        String path = raoInputWithNetworkPaths.getPostIcsImportNetworkPath().split(".jiidm")[0].concat(".uct");
        initialNetwork.write("UCTE", new Properties(), Path.of(path));
    }

    private TemporalData<PrePerimeterResult> buildInitialResults(TemporalData<RaoResult> topologicalOptimizationResults) {
        TemporalData<PrePerimeterResult> initialResults = new TemporalDataImpl<>();
        topologicalOptimizationResults.getDataPerTimestamp().forEach((timestamp, raoResult) ->
            initialResults.add(timestamp, ((FastRaoResultImpl) raoResult).getInitialResult()));
        return initialResults;
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(TemporalData<RaoInputWithNetworkPaths> raoInputs, TemporalData<Set<String>> consideredCnecs, RaoParameters raoParameters) {
        TemporalData<RaoResult> individualResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((datetime, individualRaoInputWithNetworkPath) -> {
            RaoInput individualRaoInput = RaoInput
                .build(Network.read(individualRaoInputWithNetworkPath.getPostIcsImportNetworkPath()), individualRaoInputWithNetworkPath.getCrac())
                .build();
            Set<String> cnecs = new HashSet<>();
            String logMessage = "[MARMOT] Running RAO for timestamp %s [{}]".formatted(individualRaoInput.getCrac().getTimestamp().orElseThrow());
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info(logMessage, "start");
            RaoResult raoResult = FastRao.launchFilteredRao(individualRaoInput, raoParameters, null, cnecs);
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info(logMessage, "end");
            // generate a mock of the result to avoid storing unneeded information (memory reasons)
            // (initial results, activated network actions,
            individualResults.add(datetime, raoResult);
            consideredCnecs.add(datetime, cnecs);
        });
        return individualResults; /**/
        /*TemporalData<RaoResult> individualResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((dateTime, individualRaoInputWithNetworkPath) -> {
            RaoInput individualRaoInput = RaoInput
                .build(Network.read(individualRaoInputWithNetworkPath.getPostIcsImportNetworkPath()), individualRaoInputWithNetworkPath.getCrac())
                .build();
            RaoUtil.initData(individualRaoInput, raoParameters);
            individualRaoInput.getNetwork().getVariantManager().cloneVariant(individualRaoInput.getNetworkVariantId(), "InitialScenario");
            individualResults.add(dateTime, generateMockRaoResult(individualRaoInput, raoParameters, consideredCnecs));
        });
        return individualResults; /**/
    }

    private static RaoResult generateMockRaoResult(RaoInput individualRaoInput, RaoResult raoResult) {
        //TODO: create a proper object (record?) for this instead of using Mockito
        FastRaoResultImpl mockedRaoResult = Mockito.mock(FastRaoResultImpl.class);
        Crac crac = individualRaoInput.getCrac();
        State preventiveState = crac.getPreventiveState();
        when(mockedRaoResult.getActivatedNetworkActionsDuringState(preventiveState)).thenReturn(raoResult.getActivatedNetworkActionsDuringState(preventiveState));
        when(mockedRaoResult.getInitialResult()).thenReturn(((FastRaoResultImpl) raoResult).getInitialResult());

        for (State state : crac.getStates(crac.getLastInstant())) {
            // This is to get around the cases where the result is a OneStateOnlyRaoResultImpl in the cases where the FastRao filtered rao only optimized the preventive state
            // (If we catch an error with message "Trying to access perimeter result for the wrong state.", we know no actions were applied in curative)
            boolean errorCaught = false;
            try {
                raoResult.getActivatedNetworkActionsDuringState(state);
                raoResult.getActivatedRangeActionsDuringState(state);
                raoResult.getActivatedRangeActionsDuringState(state).forEach(ra ->
                    raoResult.getOptimizedSetPointOnState(state, ra)
                );
            } catch (OpenRaoException e) {
                errorCaught = true;
                if (!e.getMessage().equals("Trying to access perimeter result for the wrong state.")) {
                    throw e;
                } else {
                    when(mockedRaoResult.getActivatedNetworkActionsDuringState(state)).thenReturn(Collections.emptySet());
                    when(mockedRaoResult.getActivatedRangeActionsDuringState(state)).thenReturn(Collections.emptySet());
                }
            }
            if (!errorCaught) {
                when(mockedRaoResult.getActivatedNetworkActionsDuringState(state)).thenReturn(raoResult.getActivatedNetworkActionsDuringState(state));
                when(mockedRaoResult.getActivatedRangeActionsDuringState(state)).thenReturn(raoResult.getActivatedRangeActionsDuringState(state));
                raoResult.getActivatedRangeActionsDuringState(state).forEach(ra ->
                    when(mockedRaoResult.getOptimizedSetPointOnState(state, ra)).thenReturn(raoResult.getOptimizedSetPointOnState(state, ra))
                );
            }
        }
        return raoResult;
    }

    // TODO: delete this, it is just used for manual testing purposes if we want to run the MIP part only without running the independent RAOs
    private static RaoResult generateMockRaoResult(RaoInput individualRaoInput, RaoParameters raoParameters, TemporalData<Set<String>> consideredCnecs) {
        FastRaoResultImpl raoResult = Mockito.mock(FastRaoResultImpl.class);
        Crac crac = individualRaoInput.getCrac();
        if (crac.getTimestamp().orElseThrow().getHour() == 2) {
            Set<String> actionNames = Set.of("TOP_2NV_DOEL_PRA", "TOP_2N_AUBAN_PRA");
            Set<NetworkAction> actions = crac.getNetworkActions().stream()
                .filter(na -> actionNames.contains(na.getName()))
                .collect(Collectors.toSet());
            when(raoResult.getActivatedNetworkActionsDuringState(individualRaoInput.getCrac().getPreventiveState())).thenReturn(actions);
        } else if (crac.getTimestamp().orElseThrow().getHour() == 1) {
            Set<String> actionNames = Set.of("TOP_2N_BRUEG_PRA");
            Set<NetworkAction> actions = crac.getNetworkActions().stream()
                .filter(na -> actionNames.contains(na.getName()))
                .collect(Collectors.toSet());
            when(raoResult.getActivatedNetworkActionsDuringState(individualRaoInput.getCrac().getPreventiveState())).thenReturn(actions);
        } else {
            when(raoResult.getActivatedNetworkActionsDuringState(individualRaoInput.getCrac().getPreventiveState())).thenReturn(new HashSet<>());
        }
        PrePerimeterResult initialResult = MarmotUtils.runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(individualRaoInput, raoParameters);
        when(raoResult.getInitialResult()).thenReturn(initialResult);
        consideredCnecs.add(crac.getTimestamp().orElseThrow(), initialResult.getCostlyElements("min-margin-violation-evaluator", 300).stream().map(Cnec::getId).collect(Collectors.toSet()));
        return raoResult;
    }

    private static void applyPreventiveTopologicalActionsOnNetwork(TemporalData<RaoInput> raoInputs, TemporalData<RaoResult> topologicalOptimizationResults) {
        // duplicate the initial scenario to keep it clean
        raoInputs.getDataPerTimestamp().values().forEach(raoInput -> {
            raoInput.getNetwork().getVariantManager().cloneVariant("InitialScenario", "PostTopoScenario");
            raoInput.getNetwork().getVariantManager().setWorkingVariant("PostTopoScenario");
        });
        getTopologicalOptimizationResult(raoInputs, topologicalOptimizationResults)
            .getDataPerTimestamp()
            .values()
            .forEach(TopologicalOptimizationResult::applyTopologicalActions);
    }

    private static TemporalData<PrePerimeterResult> runAllInitialPrePerimeterSensitivityAnalysis(TemporalData<RaoInput> raoInputs, TemporalData<AppliedRemedialActions> curativeRemedialActions, TemporalData<PrePerimeterResult> initialResults, TemporalData<Set<String>> consideredCnecs, RaoParameters raoParameters) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getTimestamps().forEach(timestamp ->
            prePerimeterResults.add(timestamp, runInitialPrePerimeterSensitivityAnalysis(
                raoInputs.getData(timestamp).orElseThrow(),
                curativeRemedialActions.getData(timestamp).orElseThrow(),
                initialResults.getData(timestamp).orElseThrow(),
                consideredCnecs.getData(timestamp).orElseThrow(),
                raoParameters)
            ));
        return prePerimeterResults;
    }

    private static TemporalData<PrePerimeterResult> applyActionsAndRunFullLoadflow(TemporalData<RaoInput> raoInputs, TemporalData<AppliedRemedialActions> curativeRemedialActions, LinearOptimizationResult filteredResult, TemporalData<PrePerimeterResult> initialResults, RaoParameters raoParameters) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            // duplicate the postTopoScenario variant and switch to the new clone
            raoInput.getNetwork().getVariantManager().cloneVariant("PostTopoScenario", "PostPreventiveScenario", true);
            raoInput.getNetwork().getVariantManager().setWorkingVariant("PostPreventiveScenario");
            State preventiveState = raoInput.getCrac().getPreventiveState();
            raoInput.getCrac().getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), filteredResult.getOptimizedSetpoint(rangeAction, preventiveState)));
            prePerimeterResults.add(timestamp, runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(
                raoInputs.getData(timestamp).orElseThrow(),
                curativeRemedialActions.getData(timestamp).orElseThrow(),
                initialResults.getData(timestamp).orElseThrow(),
                raoParameters));
            // switch back to the postTopoScenario to avoid keeping applied range actions when entering the MIP
            raoInput.getNetwork().getVariantManager().setWorkingVariant("PostTopoScenario");
        });
        return prePerimeterResults;
    }

    private static TemporalData<NetworkActionsResult> getPreventiveTopologicalActions(TemporalData<Crac> cracs, TemporalData<RaoResult> raoResults) {
        Map<OffsetDateTime, NetworkActionsResult> preventiveTopologicalActions = new HashMap<>();
        cracs.getTimestamps().forEach(timestamp -> preventiveTopologicalActions.put(timestamp, new NetworkActionsResultImpl(raoResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(cracs.getData(timestamp).orElseThrow().getPreventiveState()))));
        return new TemporalDataImpl<>(preventiveTopologicalActions);
    }

    private static LinearOptimizationResult optimizeLinearRemedialActions(
        InterTemporalRaoInput raoInput,
        TemporalData<PrePerimeterResult> initialResults,
        TemporalData<PrePerimeterResult> postTopoResults,
        RaoParameters parameters,
        TemporalData<NetworkActionsResult> preventiveTopologicalActions,
        TemporalData<AppliedRemedialActions> curativeRemedialActions,
        TemporalData<Set<String>> consideredCnecs,
        ObjectiveFunction objectiveFunction) {

        // -- Build IteratingLinearOptimizerInterTemporalInput
        // Need to use postTopoResults to build perimeter because initialResults does not contain range action setpoints
        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInput::getCrac), postTopoResults, consideredCnecs);
        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global
        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
        raoInput.getRaoInputs().getTimestamps().forEach(timestamp -> linearOptimizerInputPerTimestamp.put(timestamp, IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getNetwork())
            .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow())
            .withInitialFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPrePerimeterFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPreOptimizationFlowResult(postTopoResults.getData(timestamp).orElseThrow())
            .withPrePerimeterSetpoints(postTopoResults.getData(timestamp).orElseThrow()) //use postTopoResults because initial does not contain setpoints
            .withPreOptimizationSensitivityResult(postTopoResults.getData(timestamp).orElseThrow())
            .withPreOptimizationAppliedRemedialActions(curativeRemedialActions.getData(timestamp).orElseThrow())
            .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
            .withOutageInstant(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getOutageInstant())
            .withAppliedNetworkActionsInPrimaryState(preventiveTopologicalActions.getData(timestamp).orElseThrow())
            .build()));
        InterTemporalIteratingLinearOptimizerInput interTemporalLinearOptimizerInput = new InterTemporalIteratingLinearOptimizerInput(new TemporalDataImpl<>(linearOptimizerInputPerTimestamp), objectiveFunction, raoInput.getPowerGradients());

        // Build parameters
        // Unoptimized cnec parameters ignored because only PRAs
        // TODO: define static method to define Ra Limitation Parameters from crac and topos (mutualize with search tree) : SearchTreeParameters::decreaseRemedialActionsUsageLimits
        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder linearOptimizerParametersBuilder = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(parameters.getObjectiveFunctionParameters().getType())
            .withObjectiveFunctionUnit(parameters.getObjectiveFunctionParameters().getUnit())
            .withRangeActionParameters(parameters.getRangeActionsOptimizationParameters())
            .withRangeActionParametersExtension(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters())
            .withMaxNumberOfIterations(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()) || SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()))
            .withSolverParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxMinRelativeMarginParameters(parameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class))
            .withRaLimitationParameters(new RangeActionLimitationParameters());
        parameters.getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParameters);
        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();

        return InterTemporalIteratingLinearOptimizer.optimize(interTemporalLinearOptimizerInput, linearOptimizerParameters);
    }

    private static boolean doesPrePerimeterSetpointRespectRange(RangeAction<?> rangeAction, RangeActionSetpointResult prePerimeterSetpoints) {
        double preperimeterSetPoint = prePerimeterSetpoints.getSetpoint(rangeAction);
        double minSetPoint = rangeAction.getMinAdmissibleSetpoint(preperimeterSetPoint);
        double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(preperimeterSetPoint);

        if (preperimeterSetPoint < minSetPoint || preperimeterSetPoint > maxSetPoint) {
            BUSINESS_WARNS.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.",
                rangeAction.getId(), preperimeterSetPoint, minSetPoint, maxSetPoint);
            return false;
        } else {
            return true;
        }
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs, TemporalData<? extends RangeActionSetpointResult> prePerimeterSetpointResults, TemporalData<Set<String>> consideredCnecs) {
        TemporalData<OptimizationPerimeter> optimizationPerimeters = new TemporalDataImpl<>();
        cracs.getTimestamps().forEach(timestamp -> {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            RangeActionSetpointResult prePerimeterSetpointResult = prePerimeterSetpointResults.getData(timestamp).orElseThrow();
            optimizationPerimeters.add(timestamp, new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                MarmotUtils.getFilteredCnecs(crac, consideredCnecs.getData(timestamp).orElseThrow()),
                new HashSet<>(),
                new HashSet<>(),
                crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE).stream().filter(rangeAction -> doesPrePerimeterSetpointRespectRange(rangeAction, prePerimeterSetpointResult)).collect(Collectors.toSet())
            ));
        });
        return optimizationPerimeters;
    }

    private static InterTemporalRaoResultImpl mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> initialResults, ObjectiveFunctionResult initialLinearOptimizationResult, GlobalLinearOptimizationResult globalLinearOptimizationResult, TemporalData<RaoResult> topologicalOptimizationResults, RaoParameters raoParameters) {
        return new InterTemporalRaoResultImpl(initialLinearOptimizationResult, globalLinearOptimizationResult, getPostOptimizationResults(raoInputs, initialResults, globalLinearOptimizationResult, topologicalOptimizationResults, raoParameters).map(PostOptimizationResult::merge));
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult initialResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(Crac::getFlowCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            initialResult,
            initialResult,
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult initialResult, TemporalData<Set<String>> consideredCnecs, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.getDataPerTimestamp().forEach((dateTime, crac) -> allFlowCnecs.addAll(MarmotUtils.getFilteredCnecs(crac, consideredCnecs.getData(dateTime).orElseThrow())));
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            initialResult,
            initialResult,
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private ObjectiveFunctionResult getInitialObjectiveFunctionResult(TemporalData<PrePerimeterResult> initialResults, ObjectiveFunction objectiveFunction) {
        GlobalFlowResult globalFlowResult = new GlobalFlowResult(initialResults);
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = initialResults.map(RangeActionActivationResultImpl::new);
        TemporalData<NetworkActionsResult> networkActionsResults = initialResults.map(result -> new NetworkActionsResultImpl(new HashSet<>()));
        return objectiveFunction.evaluate(globalFlowResult, new GlobalRemedialActionActivationResult(rangeActionActivationResults, networkActionsResults));
    }

    private ObjectiveFunctionResult getIndependantRaoGlobalObjectiveFunctionResult(TemporalData<RaoResult> independentRaoResults, TemporalData<PrePerimeterResult> initialResults, TemporalData<RaoInput> raoInputs, ObjectiveFunction objectiveFunction) {
        TemporalData<FlowResult> independentFlowResults = independentRaoResults.map(raoResult -> ((FastRaoResultImpl) raoResult).getFinalResult());
        GlobalFlowResult globalFlowResult = new GlobalFlowResult(independentFlowResults);

        TemporalData<NetworkActionsResult> independentNetworkActionResults = new TemporalDataImpl<>();
        TemporalData<RangeActionActivationResult> independentRangeActionResults = new TemporalDataImpl<>();
        raoInputs.getTimestamps().forEach(timestamp -> {
            RaoResult independentRaoResult = independentRaoResults.getData(timestamp).orElseThrow();

            Set<NetworkAction> activatedNetworkActions = new HashSet<>();
            RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(initialResults.getData(timestamp).orElseThrow());
            for (State state : raoInputs.getData(timestamp).orElseThrow().getCrac().getStates()) {
                activatedNetworkActions.addAll(independentRaoResult.getActivatedNetworkActionsDuringState(state));
                independentRaoResult.getOptimizedSetPointsOnState(state).forEach((rangeAction, setpoint) -> rangeActionActivationResult.putResult(rangeAction, state, setpoint));
            }
            independentNetworkActionResults.add(timestamp, new NetworkActionsResultImpl(activatedNetworkActions));
            independentRangeActionResults.add(timestamp, rangeActionActivationResult);
        });

        return objectiveFunction.evaluate(globalFlowResult, new GlobalRemedialActionActivationResult(independentRangeActionResults, independentNetworkActionResults));
    }

    @Override
    public String getName() {
        return INTER_TEMPORAL_RAO;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
