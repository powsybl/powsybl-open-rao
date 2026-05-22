/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.StandardRangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.io.JsonTimeCoupledConstraints;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class MarmotUtils {

    private MarmotUtils() {
    }

    public static PrePerimeterResult runInitialSensitivityAnalysis(RaoInput raoInput, RaoParameters raoParameters) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
        // do not use range actions for speed purposes
        return new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), new HashSet<>(), raoParameters, toolProvider, false)
            .runInitialSensitivityAnalysis(network);
    }

    public static PrePerimeterResult runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(RaoInput raoInput,
                                                                                                  AppliedRemedialActions curativeRemedialActions,
                                                                                                  PrePerimeterResult initialResult,
                                                                                                  RaoParameters raoParameters) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        return new PrePerimeterSensitivityAnalysis(
            crac,
            crac.getFlowCnecs(), // want results on all cnecs
            new HashSet<>(), // with no range actions for faster computations, only flow values are required
            raoParameters,
            ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters),
            false
        ).runBasedOnInitialResults(network, initialResult, null, curativeRemedialActions);
    }

    public static TemporalData<AppliedRemedialActions> getAppliedRemedialActionsInCurative(TemporalData<Crac> cracs, TemporalData<RaoResult> raoResults) {
        TemporalData<AppliedRemedialActions> curativeRemedialActions = new TemporalDataImpl<>();
        cracs.getTimestamps().forEach(timestamp -> {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            RaoResult raoResult = raoResults.getData(timestamp).orElseThrow();
            AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
            // TODO: maybe check it is indeed curative
            for (State state : crac.getStates(crac.getLastInstant())) {
                try {
                    appliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
                    raoResult.getActivatedRangeActionsDuringState(state).forEach(ra -> appliedRemedialActions.addAppliedRangeAction(state, ra, raoResult.getOptimizedSetPointOnState(state, ra)));
                } catch (OpenRaoException e) {
                    if (!e.getMessage().equals("Trying to access perimeter result for the wrong state.")) {
                        throw e;
                    }
                }
            }
            curativeRemedialActions.put(timestamp, appliedRemedialActions);
        });
        return curativeRemedialActions;
    }

    public static PrePerimeterResult runSensitivityAnalysisBasedOnInitialResult(RaoInput raoInput,
                                                                                AppliedRemedialActions curativeRemedialActions,
                                                                                FlowResult initialFlowResult,
                                                                                RaoParameters raoParameters,
                                                                                Set<FlowCnec> consideredCnecs) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        State preventiveState = crac.getPreventiveState();
        Set<RangeAction<?>> rangeActions = crac.getRangeActions(preventiveState);
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
        return new PrePerimeterSensitivityAnalysis(crac, consideredCnecs, rangeActions, raoParameters, toolProvider, false)
            .runBasedOnInitialResults(network, initialFlowResult, Set.of(), curativeRemedialActions);
    }

    public static TemporalData<PostOptimizationResult> getPostOptimizationResults(TemporalData<RaoInput> raoInputs,
                                                                                  TemporalData<PrePerimeterResult> initialResults,
                                                                                  GlobalLinearOptimizationResult globalLinearOptimizationResult,
                                                                                  TemporalData<RaoResult> topologicalOptimizationResults,
                                                                                  RaoParameters raoParameters) {
        List<OffsetDateTime> timestamps = raoInputs.getTimestamps();
        Map<OffsetDateTime, PostOptimizationResult> postOptimizationResults = new HashMap<>();
        timestamps.forEach(timestamp -> {
            PostOptimizationResult postOptimizationResult = new PostOptimizationResult(
                raoInputs.getData(timestamp).orElseThrow(),
                initialResults.getData(timestamp).orElseThrow(),
                globalLinearOptimizationResult,
                topologicalOptimizationResults.getData(timestamp).orElseThrow(),
                raoParameters
            );

            // The extension cannot be associated with two different RAO results so a copy is needed
            copyCriticalCnecsExtension(topologicalOptimizationResults.getData(timestamp).orElseThrow(), postOptimizationResult);
            postOptimizationResults.put(
                timestamp,
                postOptimizationResult
            );
        });
        return new TemporalDataImpl<>(postOptimizationResults);
    }

    public static <T> T getDataFromState(TemporalData<T> temporalData, State state) {
        return temporalData.getData(state.getTimestamp().orElseThrow()).orElseThrow();
    }

    /**
     * This function combines computation statuses from a Temporal Data.
     * If any &ltT&gt has ComputationStatus.FAILURE, return ComputationStatus.FAILURE
     * Else, if any &ltT&gt has  ComputationStatus.PARTIAL_FAILURE, return ComputationStatus.PARTIAL_FAILURE
     * Else, return ComputationStatus.DEFAULT
     */
    // TODO : add synchronized for multithreading ?
    public static <T> ComputationStatus getGlobalComputationStatus(TemporalData<T> temporalData, Function<T, ComputationStatus> computationStatusCalculator) {
        Set<ComputationStatus> allStatuses = new HashSet<>(temporalData.map(computationStatusCalculator).getDataPerTimestamp().values());
        if (allStatuses.contains(ComputationStatus.FAILURE)) {
            return ComputationStatus.FAILURE;
        }
        return allStatuses.contains(ComputationStatus.PARTIAL_FAILURE) ? ComputationStatus.PARTIAL_FAILURE : ComputationStatus.DEFAULT;
    }

    public static void applyPreventiveRemedialActions(RaoInput raoInput, NetworkActionsResult networkActionsResult, String initialVariantId, String newVariantId) {
        Network network = raoInput.getNetwork();
        Crac crac = raoInput.getCrac();

        State preventiveState = crac.getPreventiveState();
        if (!network.getVariantManager().getVariantIds().contains(initialVariantId)) {
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), initialVariantId);
        }
        network.getVariantManager().setWorkingVariant(initialVariantId);
        network.getVariantManager().cloneVariant(initialVariantId, newVariantId);
        network.getVariantManager().setWorkingVariant(newVariantId);
        Set<NetworkAction> networkActionsToBeApplied = networkActionsResult.getActivatedNetworkActionsPerState().get(preventiveState);
        if (networkActionsToBeApplied.isEmpty()) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] No preventive topological actions applied for timestamp {}", crac.getTimestamp().orElseThrow());
            MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
        } else {
            networkActionsToBeApplied.forEach(networkAction -> networkAction.apply(network));
            MarmotUtils.releaseNetwork(raoInput.getNetwork());
        }
    }

    // Use releaseNetwork : we don't want to delete networks.
    public static TemporalData<LazyNetwork> cloneNetworks(TemporalData<Network> networks) {
        TemporalData<LazyNetwork> lazyNetworks = new TemporalDataImpl<>();
        networks.getDataPerTimestamp().forEach((timestamp, network) -> {
            lazyNetworks.put(timestamp, new LazyNetwork(network));
            MarmotUtils.releaseNetworkWithoutOverwrite(network);
        });
        return lazyNetworks;
    }

    public static TemporalData<RaoInput> merge(TemporalData<LazyNetwork> networks, TemporalData<Crac> cracs) {
        Map<OffsetDateTime, RaoInput> raoInputs = new HashMap<>();
        for (OffsetDateTime timestamp : networks.getTimestamps()) {
            raoInputs.put(timestamp, RaoInput.build(networks.getData(timestamp).orElseThrow(), cracs.getData(timestamp).orElseThrow()).build());
            MarmotUtils.releaseNetworkWithoutOverwrite(networks.getData(timestamp).orElseThrow());
        }
        return new TemporalDataImpl<>(raoInputs);
    }

    public static <N extends Network> void releaseAllWithoutOverwrite(TemporalData<N> networks) {
        networks.getDataPerTimestamp().values().forEach(MarmotUtils::releaseNetworkWithoutOverwrite);
    }

    public static <N extends Network> void closeAll(TemporalData<N> networks) {
        networks.getDataPerTimestamp().values().forEach(MarmotUtils::closeNetwork);
    }

    public static <N extends Network> void releaseNetwork(N network) {
        if (network instanceof LazyNetwork lazyNetwork) {
            try {
                lazyNetwork.release();
            } catch (Exception e) {
                throw new OpenRaoException(e);
            }
        }
    }

    public static <N extends Network> void releaseNetworkWithoutOverwrite(N network) {
        if (network instanceof LazyNetwork lazyNetwork) {
            try {
                lazyNetwork.releaseWithOverwrite(false);
            } catch (Exception e) {
                throw new OpenRaoException(e);
            }
        }
    }

    public static <N extends Network> void closeNetwork(N network) {
        if (network instanceof LazyNetwork lazyNetwork) {
            try {
                lazyNetwork.close();
            } catch (Exception e) {
                throw new OpenRaoException(e);
            }
        }
    }

    public static OffsetDateTime getTimestamp(RaoInput raoInput) {
        return raoInput.getCrac().getTimestamp().orElseThrow();
    }

    public static RaoParameters cloneParameters(RaoParameters raoParameters) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            JsonRaoParameters.write(raoParameters, outputStream);
            try (InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                return JsonRaoParameters.read(inputStream);
            }
        } catch (Exception e) {
            throw new OpenRaoException(e);
        }
    }

    /**
     * Select the best TemporalData mapping strategy based on the number of threads.
     * Necessary not to create a pool for only one thread.
     */
    public static <A, B> TemporalData<B> smartMap(TemporalData<A> temporalData, Function<A, B> function, int threads) {
        if (threads == 1) {
            return temporalData.map(function);
        }
        return temporalData.mapMultiThreading(function, threads);
    }

    public static void copyCriticalCnecsExtension(RaoResult raoResultFrom, RaoResult raoResultTo) {
        CriticalCnecsResult criticalCnecsResultFrom = raoResultFrom.getExtension(CriticalCnecsResult.class);
        if (criticalCnecsResultFrom != null) {
            CriticalCnecsResult criticalCnecsResultTo = new CriticalCnecsResult();
            criticalCnecsResultTo.setCriticalCnecIds(criticalCnecsResultFrom.getCriticalCnecIds());
            raoResultTo.addExtension(CriticalCnecsResult.class, criticalCnecsResultTo);
        }
    }

    public static double getInitialSetPoint(RangeAction<?> rangeAction) {
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            return pstRangeAction.convertTapToAngle(pstRangeAction.getInitialTap());
        } else if (rangeAction instanceof StandardRangeAction<?> standardRangeAction) {
            return standardRangeAction.getInitialSetpoint();
        }
        return Double.NaN;
    }

    public static void exportInputs(TemporalData<RaoInput> raoInputs, TimeCoupledConstraints timeCoupledConstraints) {
        String businessDate = raoInputs.getTimestamps().getLast().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String exportPath = "sensitive/" + businessDate + "/";
        new File(exportPath).mkdirs();
        new File(exportPath.concat("time-coupled-constraints/")).mkdir();
        new File(exportPath.concat("networks/")).mkdir();
        new File(exportPath.concat("cracs/")).mkdir();

        int nTimestamps = raoInputs.getTimestamps().size();

        TECHNICAL_LOGS.info("----- Exporting input data in folder {} [start]", exportPath);

        // 1. Export time-coupled constraints
        TECHNICAL_LOGS.debug("----- Exporting time-coupled constraints [start]");
        try (FileOutputStream fileOutputStream = new FileOutputStream(exportPath.concat("time-coupled-constraints/time-coupled-constraints.json"))) {
            JsonTimeCoupledConstraints.write(timeCoupledConstraints, fileOutputStream);
        } catch (IOException e) {
            throw new OpenRaoException(e);
        }
        TECHNICAL_LOGS.debug("----- Exporting time-coupled constraints [end]");

        // 2. Export pre-processed networks
        TECHNICAL_LOGS.debug("----- Exporting pre-processed networks [start]");
        AtomicInteger i = new AtomicInteger(1);
        raoInputs.getDataPerTimestamp().forEach(
            (timestamp, raoInput) -> {
                String networkPath = exportPath.concat("networks/").concat(timestamp.toString()).concat(".jiidm");
                raoInput.getNetwork().write("JIIDM", new Properties(), Path.of(networkPath));
                TECHNICAL_LOGS.debug(">>> [{}/{}] Exported pre-processed network for timestamp: {}", i.get(), nTimestamps, timestamp);
                i.getAndIncrement();
            }
        );
        TECHNICAL_LOGS.debug("----- Exporting pre-processed networks [end]");

        // 3. Export pre-processed CRACs
        TECHNICAL_LOGS.debug("----- Exporting pre-processed CRACs [start]");
        i.set(1);
        raoInputs.getDataPerTimestamp().forEach(
            (timestamp, raoInput) -> {
                String cracPath = exportPath.concat("cracs/").concat(timestamp.toString()).concat(".json");
                try (FileOutputStream fileOutputStream = new FileOutputStream(cracPath)) {
                    raoInput.getCrac().write("JSON", fileOutputStream);
                } catch (IOException e) {
                    throw new OpenRaoException(e);
                }
                TECHNICAL_LOGS.debug(">>> [{}/{}] Exported pre-processed CRAC for timestamp: {}", i.get(), nTimestamps, timestamp);
                i.getAndIncrement();
            }
        );
        TECHNICAL_LOGS.debug("----- Exporting pre-processed CRACs [end]");

        TECHNICAL_LOGS.info("----- Exporting input data in folder {} [end]", exportPath);
    }

    public static void exportIntermediateRaoResults(TemporalData<RaoResult> raoResults, TemporalData<RaoInput> raoInputs) {
        String businessDate = raoResults.getTimestamps().getLast().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String exportPath = "sensitive/" + businessDate + "/";
        new File(exportPath.concat("intermediate-rao-results/")).mkdirs();

        int nTimestamps = raoResults.getTimestamps().size();

        TECHNICAL_LOGS.debug("----- Exporting intermediate RAO Results [start]");
        AtomicInteger i = new AtomicInteger(1);
        raoResults.getTimestamps().forEach(
            timestamp -> {
                RaoResult individualRaoResult = raoResults.getData(timestamp).orElseThrow();
                Crac crac = raoInputs.getData(timestamp).orElseThrow().getCrac();
                String raoResultPath = exportPath.concat("intermediate-rao-results/").concat(timestamp.toString()).concat(".json");
                try (FileOutputStream fileOutputStream = new FileOutputStream(raoResultPath)) {
                    Properties properties = new Properties();
                    properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
                    individualRaoResult.write("JSON", crac, properties, fileOutputStream);
                    TECHNICAL_LOGS.debug(">>> [{}/{}] Exported intermediate RAO Result for timestamp: {}", i.get(), nTimestamps, timestamp);
                    i.getAndIncrement();
                } catch (IOException e) {
                    throw new OpenRaoException(e);
                }
            }
        );
        TECHNICAL_LOGS.debug("----- Exporting intermediate RAO Results [end]");
    }

    public static void exportRaoResults(TimeCoupledRaoResult raoResult, TemporalData<RaoInput> raoInputs) {
        String businessDate = raoResult.getTimestamps().getLast().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String exportPath = "sensitive/" + businessDate + "/";
        new File(exportPath.concat("rao-results/")).mkdirs();

        int nTimestamps = raoResult.getTimestamps().size();

        TECHNICAL_LOGS.debug("----- Exporting independent RAO Results [start]");
        AtomicInteger i = new AtomicInteger(1);
        raoResult.getTimestamps().forEach(
            timestamp -> {
                RaoResult individualRaoResult = raoResult.getIndividualRaoResult(timestamp);
                Crac crac = raoInputs.getData(timestamp).orElseThrow().getCrac();
                String raoResultPath = exportPath.concat("rao-results/").concat(timestamp.toString()).concat(".json");
                try (FileOutputStream fileOutputStream = new FileOutputStream(raoResultPath)) {
                    Properties properties = new Properties();
                    properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
                    individualRaoResult.write("JSON", crac, properties, fileOutputStream);
                    TECHNICAL_LOGS.debug(">>> [{}/{}] Exported RAO Result for timestamp: {}", i.get(), nTimestamps, timestamp);
                    i.getAndIncrement();
                } catch (IOException e) {
                    throw new OpenRaoException(e);
                }
            }
        );
        TECHNICAL_LOGS.debug("----- Exporting independent RAO Results [end]");
    }
}
