/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.marmot.results.TimeCoupledRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.logCost;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(TimeCoupledRaoProvider.class)
public class Marmot implements TimeCoupledRaoProvider {

    private static final String TIME_COUPLED_RAO = "TimeCoupledRao";
    private static final String INITIAL_SCENARIO = "InitialScenario";
    private static final String POST_TOPO_SCENARIO = "PostTopoScenario";
    private static final String MIP_SCENARIO = "MipScenario";
    private static final String MIN_MARGIN_VIOLATION_EVALUATOR = "min-margin-violation-evaluator";

    @Override
    public CompletableFuture<TimeCoupledRaoResult> run(TimeCoupledRaoInputWithNetworkPaths timeCoupledRaoInputWithNetworkPaths, RaoParameters raoParameters) {
        // 1. Run independent RAOs to compute optimal preventive topological remedial actions
        TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [start]");
        TemporalData<Set<FlowCnec>> consideredCnecs = new TemporalDataImpl<>();
        TemporalData<RaoResult> topologicalOptimizationResults = runTopologicalOptimization(timeCoupledRaoInputWithNetworkPaths.getRaoInputs(), consideredCnecs, raoParameters);
        TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [end]");

        // 2. Get the initial results from the various independent results to avoid recomputing them
        TemporalData<PrePerimeterResult> initialResults = buildInitialResults(topologicalOptimizationResults);

        // TODO : Add time-coupled constraint check if none violated then return
        boolean noTimeCoupledConstraints = timeCoupledRaoInputWithNetworkPaths.getTimeCoupledConstraints().getGeneratorConstraints().isEmpty();

        // 3. Apply independent topological remedial actions (and preventive range actions if there are no time-coupled constraints)
        TimeCoupledRaoInput timeCoupledRaoInput = importNetworksFromTimeCoupledRaoInputWithNetworkPaths(timeCoupledRaoInputWithNetworkPaths);
        TECHNICAL_LOGS.info("[MARMOT] Applying optimal topological actions on networks");
        ObjectiveFunction fullObjectiveFunction = buildGlobalObjectiveFunction(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction);

        // 4. Evaluate objective function after independent optimizations
        TECHNICAL_LOGS.info("[MARMOT] Evaluating global result after independent optimizations");
        TemporalData<PrePerimeterResult> postTopologicalActionsResults = topologicalOptimizationResults.map(
            raoResult -> ((FastRaoResultImpl) raoResult).getFinalResult()
        );
        TemporalData<RangeActionSetpointResult> initialSetpointResults = getInitialSetpointResults(topologicalOptimizationResults, timeCoupledRaoInput.getRaoInputs());
        LinearOptimizationResult postTopologicalOptimizationResult = getPostTopologicalOptimizationResult(
            initialSetpointResults,
            postTopologicalActionsResults,
            fullObjectiveFunction,
            topologicalOptimizationResults,
            timeCoupledRaoInput.getRaoInputs().map(individualRaoInput -> individualRaoInput.getCrac().getPreventiveState()));

        // if no time-coupled constraints are defined, the results can be returned
        // TODO
//        if (noTimeCoupledConstraints) {
//            TECHNICAL_LOGS.info("[MARMOT] No time-coupled constraint provided; no need to re-optimize range actions");
//            return CompletableFuture.completedFuture(new TimeCoupledRaoResultImpl(initialObjectiveFunctionResult, postTopologicalOptimizationResult, topologicalOptimizationResults));
//        }

        // 5. Get and apply topological actions applied in independent optimizations
        TemporalData<NetworkActionsResult> preventiveTopologicalActions = getPreventiveTopologicalActions(timeCoupledRaoInputWithNetworkPaths.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), topologicalOptimizationResults);
        applyPreventiveTopologicalActionsOnNetworks(timeCoupledRaoInput.getRaoInputs(), preventiveTopologicalActions);

        // 6. Create and iteratively solve MIP to find optimal range actions' set-points
        // Get the curative ations applied in the individual results to be able to apply them during sensitivity computations
        TemporalData<AppliedRemedialActions> curativeRemedialActions = MarmotUtils.getAppliedRemedialActionsInCurative(timeCoupledRaoInput.getRaoInputs(), topologicalOptimizationResults);

        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start]");
        // make fast rao result lighter by keeping only initial flow result and filtered rao result for actions
        replaceFastRaoResultsWithLightVersions(topologicalOptimizationResults);

        //TODO: loop
        TemporalData<PrePerimeterResult> loadFlowResults;
        GlobalLinearOptimizationResult linearOptimizationResults;
        GlobalLinearOptimizationResult fullResults;
        int counter = 1;
        do {
            // Clone the PostTopoScenario variant to make sure we work on a clean variant every time
            timeCoupledRaoInput.getRaoInputs().getDataPerTimestamp().values().forEach(raoInput -> {
                raoInput.getNetwork().getVariantManager().cloneVariant(POST_TOPO_SCENARIO, MIP_SCENARIO, true);
                raoInput.getNetwork().getVariantManager().setWorkingVariant(MIP_SCENARIO);
            });

            // Run post topo sensitivity analysis on all timestamps ON CONSIDERED CNECS ONLY (which is why we do it every loop)
            TECHNICAL_LOGS.info("[MARMOT] Systematic time-coupled sensitivity analysis [start]");
            TemporalData<PrePerimeterResult> postTopoResults = runAllSensitivityAnalysesBasedOnInitialResult(timeCoupledRaoInput.getRaoInputs(), curativeRemedialActions, initialResults, raoParameters, consideredCnecs);
            TECHNICAL_LOGS.info("[MARMOT] Systematic time-coupled sensitivity analysis [end]");

            // Build objective function with ONLY THE CONSIDERED CNECS
            ObjectiveFunction filteredObjectiveFunction = buildFilteredObjectiveFunction(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), raoParameters, consideredCnecs);

            // Create and iteratively solve MIP to find optimal range actions' set-points FOR THE CONSIDERED CNECS
            TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start] for iteration {}", counter);
            linearOptimizationResults = optimizeLinearRemedialActions(timeCoupledRaoInput, initialResults, initialSetpointResults, postTopoResults, raoParameters, preventiveTopologicalActions, curativeRemedialActions, consideredCnecs, filteredObjectiveFunction);
            TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end] for iteration {}", counter);

            // Compute the flows on ALL the cnecs to check if the worst cnecs have changed and were considered in the MIP or not
            loadFlowResults = applyActionsAndRunFullLoadflow(timeCoupledRaoInput.getRaoInputs(), curativeRemedialActions, linearOptimizationResults, initialResults, raoParameters);

            // Create a global result with the flows on ALL cnecs and the actions applied during MIP
            TemporalData<RangeActionActivationResult> rangeActionActivationResultTemporalData = linearOptimizationResults.getRangeActionActivationResultTemporalData();
            fullResults = new GlobalLinearOptimizationResult(loadFlowResults, loadFlowResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResultTemporalData, preventiveTopologicalActions, fullObjectiveFunction, LinearProblemStatus.OPTIMAL);

            logCost("[MARMOT] next iteration of MIP: ", fullResults, raoParameters, 10);
            counter++;
        } while (shouldContinueAndAddCnecs(loadFlowResults, consideredCnecs, getFlowUnit(raoParameters)) && counter < 10); // Stop if the worst element of each TS has been considered during MIP
        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end]");

        // 7. Merge topological and linear result
        TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");
        TimeCoupledRaoResultImpl timeCoupledRaoResult = mergeTopologicalAndLinearOptimizationResults(timeCoupledRaoInput.getRaoInputs(), initialResults, initialObjectiveFunctionResult, fullResults, topologicalOptimizationResults, raoParameters);

        // 8. Log initial and final results
        logCost("[MARMOT] Before topological optimizations: ", initialObjectiveFunctionResult, raoParameters, 10);
        logCost("[MARMOT] Before global linear optimization: ", postTopologicalOptimizationResult, raoParameters, 10);
        logCost("[MARMOT] After global linear optimization: ", fullResults, raoParameters, 10);

        return CompletableFuture.completedFuture(timeCoupledRaoResult);
    }

    private TemporalData<RangeActionSetpointResult> getInitialSetpointResults(TemporalData<RaoResult> postTopologicalActionsResults, TemporalData<RaoInput> raoInputs) {
        TemporalData<RangeActionSetpointResult> initialSetpointResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            Map<RangeAction<?>, Double> setPointMap = new HashMap<>();
            raoInput.getCrac().getRangeActions().forEach(rangeAction ->
                setPointMap.put(rangeAction, postTopologicalActionsResults.getData(timestamp).orElseThrow()
                    .getPreOptimizationSetPointOnState(raoInput.getCrac().getPreventiveState(), rangeAction))
            );
            RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(
                setPointMap
            );
            initialSetpointResults.put(timestamp, rangeActionSetpointResult);
        });
        return initialSetpointResults;
    }

    private boolean shouldContinueAndAddCnecs(TemporalData<PrePerimeterResult> loadFlowResults, TemporalData<Set<FlowCnec>> consideredCnecs, Unit flowUnit) {
        int cnecsToAddPerVirtualCostName = 20;
        double minRelativeImprovementOnMargin = 0.1;
        double marginWindowToConsider = 5.0;

        AtomicBoolean shouldContinue = new AtomicBoolean(false);
        updateShouldContinue(loadFlowResults, consideredCnecs, minRelativeImprovementOnMargin, shouldContinue, flowUnit);

        if (shouldContinue.get()) {
            updateConsideredCnecs(loadFlowResults, consideredCnecs, marginWindowToConsider, cnecsToAddPerVirtualCostName, flowUnit);
        }
        return shouldContinue.get();
    }

    private static void updateShouldContinue(TemporalData<PrePerimeterResult> loadFlowResults, TemporalData<Set<FlowCnec>> consideredCnecs, double minRelativeImprovementOnMargin, AtomicBoolean shouldContinue, Unit flowUnit) {
        loadFlowResults.getTimestamps().forEach(timestamp -> {
            PrePerimeterResult loadFlowResult = loadFlowResults.getData(timestamp).orElseThrow();
            Set<FlowCnec> previousCnecs = consideredCnecs.getData(timestamp).orElseThrow();

            // for margin violation - need to compare to min improvement on margin
            // ordered list of cnecs with an overload
            List<FlowCnec> worstCnecsForMarginViolation = loadFlowResult.getCostlyElements(MIN_MARGIN_VIOLATION_EVALUATOR, Integer.MAX_VALUE);
            double worstConsideredMargin = worstCnecsForMarginViolation.stream()
                .filter(previousCnecs::contains)
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, flowUnit))
                .orElse(0.);
            double worstMarginOfAll = worstCnecsForMarginViolation.stream()
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, flowUnit))
                .orElse(0.);
            // if worst overload > worst considered overload *( 1 + minImprovementOnLoad)
            if (worstMarginOfAll < worstConsideredMargin * (1 + minRelativeImprovementOnMargin) - 1e-6) {
                shouldContinue.set(true);
            }

            // for other violations - just check if cnec was considered
            loadFlowResult.getVirtualCostNames().stream()
                .filter(vcName -> !vcName.equals(MIN_MARGIN_VIOLATION_EVALUATOR))
                .forEach(vcName -> {
                    Optional<FlowCnec> worstCnec = loadFlowResult.getCostlyElements(vcName, 1).stream().findFirst();
                    if (worstCnec.isPresent() && !previousCnecs.contains(worstCnec.get())) {
                        shouldContinue.set(true);
                    }
                });
        });
    }

    private static void updateConsideredCnecs(TemporalData<PrePerimeterResult> loadFlowResults, TemporalData<Set<FlowCnec>> consideredCnecs, double marginWindowToConsider, int cnecsToAddPerVirtualCostName, Unit flowUnit) {
        List<LoggingAddedCnecs> addedCnecsForLogging = new ArrayList<>();
        loadFlowResults.getTimestamps().forEach(timestamp -> {
            PrePerimeterResult loadFlowResult = loadFlowResults.getData(timestamp).orElseThrow();
            Set<FlowCnec> previousIterationCnecs = consideredCnecs.getData(timestamp).orElseThrow();
            Set<FlowCnec> nextIterationCnecs = new HashSet<>(previousIterationCnecs);

            double worstConsideredMargin = loadFlowResult.getCostlyElements(MIN_MARGIN_VIOLATION_EVALUATOR, Integer.MAX_VALUE)
                .stream()
                .filter(previousIterationCnecs::contains)
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, flowUnit))
                .orElse(0.);

            loadFlowResult.getVirtualCostNames().forEach(vcName -> {
                LoggingAddedCnecs currentLoggingAddedCnecs = new LoggingAddedCnecs(timestamp, vcName, new ArrayList<>(), new HashMap<>());
                int addedCnecsForVcName = 0;

                // for min margin violation take all cnecs
                if (vcName.equals(MIN_MARGIN_VIOLATION_EVALUATOR)) {
                    for (FlowCnec cnec : loadFlowResult.getCostlyElements(vcName, Integer.MAX_VALUE)) {
                        if (loadFlowResult.getMargin(cnec, flowUnit) > worstConsideredMargin + marginWindowToConsider && addedCnecsForVcName > cnecsToAddPerVirtualCostName) {
                            // stop if out of window and already added enough
                            break;
                        } else if (!previousIterationCnecs.contains(cnec)) {
                            // if in window or not added enough yet, add
                            nextIterationCnecs.add(cnec);
                            addedCnecsForVcName++;
                            currentLoggingAddedCnecs.addCnec(cnec.getId(), loadFlowResult.getMargin(cnec, flowUnit));
                        }
                    }
                } else if (loadFlowResult.getVirtualCost(vcName) > 1e-6) {
                    for (FlowCnec cnec : loadFlowResult.getCostlyElements(vcName, Integer.MAX_VALUE)) {
                        if (!previousIterationCnecs.contains(cnec)) {
                            nextIterationCnecs.add(cnec);
                            currentLoggingAddedCnecs.addCnec(cnec.getId());
                        }
                    }
                }
                addedCnecsForLogging.add(currentLoggingAddedCnecs);
            });
            consideredCnecs.put(timestamp, nextIterationCnecs);
        });
        logCnecs(addedCnecsForLogging);
    }

    private static void logCnecs(List<LoggingAddedCnecs> addedCnecsForLogging) {
        StringBuilder logMessage = new StringBuilder("[MARMOT] Proceeding to next iteration by adding:");
        for (LoggingAddedCnecs loggingAddedCnecs : addedCnecsForLogging) {
            if (!loggingAddedCnecs.addedCnecs().isEmpty()) {
                logMessage.append(" for timestamp ").append(loggingAddedCnecs.offsetDateTime().toString()).append(" and virtual cost ").append(loggingAddedCnecs.vcName()).append(" ");
                for (String cnec : loggingAddedCnecs.addedCnecs()) {
                    String cnecString = loggingAddedCnecs.vcName().equals(MIN_MARGIN_VIOLATION_EVALUATOR) ?
                        cnec + "(" + loggingAddedCnecs.margins().get(cnec) + ")" + "," :
                        cnec + ",";
                    logMessage.append(cnecString);
                }
            }
        }
        TECHNICAL_LOGS.info(logMessage.toString());
    }

    record LoggingAddedCnecs(OffsetDateTime offsetDateTime, String vcName, List<String> addedCnecs, Map<String, Double> margins) {
        private void addCnec(String cnec) {
            addedCnecs.add(cnec);
        }

        private void addCnec(String cnec, double margin) {
            addedCnecs.add(cnec);
            margins.put(cnec, margin);
        }
    }

    private static TemporalData<PrePerimeterResult> applyActionsAndRunFullLoadflow(TemporalData<RaoInput> raoInputs, TemporalData<AppliedRemedialActions> curativeRemedialActions, LinearOptimizationResult filteredResult, TemporalData<PrePerimeterResult> initialResults, RaoParameters raoParameters) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            // duplicate the postTopoScenario variant and switch to the new clone
            raoInput.getNetwork().getVariantManager().cloneVariant(POST_TOPO_SCENARIO, "PostPreventiveScenario", true);
            raoInput.getNetwork().getVariantManager().setWorkingVariant("PostPreventiveScenario");
            State preventiveState = raoInput.getCrac().getPreventiveState();
            raoInput.getCrac().getRangeActions(preventiveState).forEach(rangeAction ->
                rangeAction.apply(raoInput.getNetwork(), filteredResult.getOptimizedSetpoint(rangeAction, preventiveState))
            );
            prePerimeterResults.put(timestamp, runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(
                raoInputs.getData(timestamp).orElseThrow(),
                curativeRemedialActions.getData(timestamp).orElseThrow(),
                initialResults.getData(timestamp).orElseThrow(),
                raoParameters));
            // switch back to the postTopoScenario to avoid keeping applied range actions when entering the MIP
            raoInput.getNetwork().getVariantManager().setWorkingVariant(POST_TOPO_SCENARIO);
        });
        return prePerimeterResults;
    }

    private void replaceFastRaoResultsWithLightVersions(TemporalData<RaoResult> topologicalOptimizationResults) {
        topologicalOptimizationResults.getDataPerTimestamp().forEach((timestamp, raoResult) -> topologicalOptimizationResults.put(timestamp, new LightFastRaoResultImpl((FastRaoResultImpl) raoResult)));
    }

    private TimeCoupledRaoInput importNetworksFromTimeCoupledRaoInputWithNetworkPaths(TimeCoupledRaoInputWithNetworkPaths timeCoupledRaoInputWithNetworkPaths) {
        return new TimeCoupledRaoInput(
            timeCoupledRaoInputWithNetworkPaths.getRaoInputs().map(raoInputWithNetworksPath -> {
                RaoInput raoInput = raoInputWithNetworksPath.toRaoInputWithPostIcsImportNetworkPath();
                raoInput.getNetwork().getVariantManager().cloneVariant(raoInput.getNetworkVariantId(), INITIAL_SCENARIO);
                return raoInput;
            }),
            timeCoupledRaoInputWithNetworkPaths.getTimestampsToRun(),
            timeCoupledRaoInputWithNetworkPaths.getTimeCoupledConstraints()
        );
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(TemporalData<RaoInputWithNetworkPaths> raoInputs, TemporalData<Set<FlowCnec>> consideredCnecs, RaoParameters raoParameters) {
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolverSpecificParameters("MAXTIME 15");

        TemporalData<RaoResult> individualResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((datetime, individualRaoInputWithNetworkPath) -> {
            RaoInput individualRaoInput = RaoInput
                .build(Network.read(individualRaoInputWithNetworkPath.getPostIcsImportNetworkPath()), individualRaoInputWithNetworkPath.getCrac())
                .build();
            Set<FlowCnec> cnecs = new HashSet<>();
            String logMessage = "[MARMOT] Running RAO for timestamp %s [{}]".formatted(individualRaoInput.getCrac().getTimestamp().orElseThrow());
            TECHNICAL_LOGS.info(logMessage, "start");
            RaoResult raoResult = FastRao.launchFastRaoOptimization(individualRaoInput, raoParameters, null, cnecs);
            TECHNICAL_LOGS.info(logMessage, "end");
            consideredCnecs.put(datetime, cnecs);
            individualResults.put(datetime, raoResult);
        });
        return individualResults;
    }

    private static void applyPreventiveTopologicalActionsOnNetworks(TemporalData<RaoInput> raoInputs, TemporalData<NetworkActionsResult> preventiveTopologicalActionsResults) {
        raoInputs.getTimestamps().forEach(timestamp -> {
            RaoInput raoInput = raoInputs.getData(timestamp).orElseThrow();
            NetworkActionsResult networkActionsResult = preventiveTopologicalActionsResults.getData(timestamp).orElseThrow();
            MarmotUtils.applyPreventiveRemedialActions(raoInput, networkActionsResult, INITIAL_SCENARIO, POST_TOPO_SCENARIO);
        });
    }

    private TemporalData<PrePerimeterResult> buildInitialResults(TemporalData<RaoResult> topologicalOptimizationResults) {
        TemporalData<PrePerimeterResult> initialResults = new TemporalDataImpl<>();
        topologicalOptimizationResults.getDataPerTimestamp().forEach((timestamp, raoResult) ->
            initialResults.put(timestamp, ((FastRaoResultImpl) raoResult).getInitialResult()));
        return initialResults;
    }

    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalysesBasedOnInitialResult(TemporalData<RaoInput> raoInputs, TemporalData<AppliedRemedialActions> curativeRemedialActions, TemporalData<? extends FlowResult> initialFlowResults, RaoParameters raoParameters, TemporalData<Set<FlowCnec>> consideredCnecs) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getTimestamps().forEach(timestamp ->
            prePerimeterResults.put(timestamp, runSensitivityAnalysisBasedOnInitialResult(
                raoInputs.getData(timestamp).orElseThrow(),
                curativeRemedialActions.getData(timestamp).orElseThrow(),
                initialFlowResults.getData(timestamp).orElseThrow(),
                raoParameters,
                consideredCnecs.getData(timestamp).orElseThrow()
        )));
        return prePerimeterResults;
    }

    private static TemporalData<NetworkActionsResult> getPreventiveTopologicalActions(TemporalData<Crac> cracs, TemporalData<RaoResult> raoResults) {
        Map<OffsetDateTime, NetworkActionsResult> preventiveTopologicalActions = new HashMap<>();
        cracs.getTimestamps().forEach(timestamp -> {
            State preventiveState = cracs.getData(timestamp).orElseThrow().getPreventiveState();
            preventiveTopologicalActions.put(timestamp, new NetworkActionsResultImpl(Map.of(preventiveState, raoResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(preventiveState))));
        });
        return new TemporalDataImpl<>(preventiveTopologicalActions);
    }

    private static GlobalLinearOptimizationResult optimizeLinearRemedialActions(TimeCoupledRaoInput raoInput, TemporalData<PrePerimeterResult> initialResults, TemporalData<RangeActionSetpointResult> initialSetpoints, TemporalData<PrePerimeterResult> postTopologicalActionsResults, RaoParameters parameters, TemporalData<NetworkActionsResult> preventiveTopologicalActions, TemporalData<AppliedRemedialActions> curativeRemedialActions, TemporalData<Set<FlowCnec>> consideredCnecs, ObjectiveFunction objectiveFunction) {

        // -- Build IteratingLinearOptimizertimeCoupledInput
        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInput::getCrac), consideredCnecs);
        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global

        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
        raoInput.getRaoInputs().getTimestamps().forEach(timestamp -> linearOptimizerInputPerTimestamp.put(timestamp,
            IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getNetwork())
            .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow().copyWithFilteredAvailableHvdcRangeAction(raoInput.getRaoInputs().getData(timestamp).get().getNetwork()))
            .withInitialFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPrePerimeterFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPreOptimizationFlowResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
            .withPrePerimeterSetpoints(initialSetpoints.getData(timestamp).orElseThrow())
            .withPreOptimizationSensitivityResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
            .withPreOptimizationAppliedRemedialActions(curativeRemedialActions.getData(timestamp).orElseThrow())
            .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
            .withOutageInstant(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getOutageInstant())
            .withAppliedNetworkActionsInPrimaryState(preventiveTopologicalActions.getData(timestamp).orElseThrow())
            .build()));
        TimeCoupledIteratingLinearOptimizerInput timeCoupledLinearOptimizerInput = new TimeCoupledIteratingLinearOptimizerInput(new TemporalDataImpl<>(linearOptimizerInputPerTimestamp), objectiveFunction, raoInput.getTimeCoupledConstraints());

        // Build parameters
        // Unoptimized cnec parameters ignored because only PRAs
        // TODO: define static method to define Ra Limitation Parameters from crac and topos (mutualize with search tree) : SearchTreeParameters::decreaseRemedialActionsUsageLimits
        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder linearOptimizerParametersBuilder = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(parameters.getObjectiveFunctionParameters().getType())
            .withFlowUnit(getFlowUnit(parameters))
            .withRangeActionParameters(parameters.getRangeActionsOptimizationParameters())
            .withRangeActionParametersExtension(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters())
            .withMaxNumberOfIterations(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()) || SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()))
            .withSolverParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxMinRelativeMarginParameters(parameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class))
            .withRaLimitationParameters(new RangeActionLimitationParameters())
            .withMinMarginParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().orElse(new SearchTreeRaoCostlyMinMarginParameters()));
        parameters.getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParametersExtension);
        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParametersExtension);
        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();

        return TimeCoupledIteratingLinearOptimizer.optimize(timeCoupledLinearOptimizerInput, linearOptimizerParameters);
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs, TemporalData<Set<FlowCnec>> consideredCnecs) {
        TemporalData<OptimizationPerimeter> optimizationPerimeters = new TemporalDataImpl<>();
        cracs.getTimestamps().forEach(timestamp -> {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            optimizationPerimeters.put(timestamp, new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                consideredCnecs.getData(timestamp).orElseThrow(),
                new HashSet<>(), // no loopflows for now
                new HashSet<>(), // don't re-optimize topological actions in Marmot
                crac.getRangeActions(crac.getPreventiveState())
            ));
        });
        return optimizationPerimeters;
    }

    private static TimeCoupledRaoResultImpl mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> initialResults, ObjectiveFunctionResult initialLinearOptimizationResult, GlobalLinearOptimizationResult globalLinearOptimizationResult, TemporalData<RaoResult> topologicalOptimizationResults, RaoParameters raoParameters) {
        return new TimeCoupledRaoResultImpl(
            initialLinearOptimizationResult,
            globalLinearOptimizationResult,
            getPostOptimizationResults(
                raoInputs,
                initialResults,
                globalLinearOptimizationResult,
                topologicalOptimizationResults,
                raoParameters));
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(Crac::getFlowCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private static ObjectiveFunction buildFilteredObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, RaoParameters raoParameters, TemporalData<Set<FlowCnec>> consideredCnecs) {
        Set<FlowCnec> flatConsideredCnecs = new HashSet<>();
        consideredCnecs.getDataPerTimestamp().values().forEach(flatConsideredCnecs::addAll);

        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(
            flatConsideredCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private LinearOptimizationResult getInitialObjectiveFunctionResult(TemporalData<PrePerimeterResult> prePerimeterResults, ObjectiveFunction objectiveFunction) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = prePerimeterResults.map(RangeActionActivationResultImpl::new);
        TemporalData<NetworkActionsResult> networkActionsResults = new TemporalDataImpl<>();
        return new GlobalLinearOptimizationResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult), prePerimeterResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL);
    }

    private LinearOptimizationResult getPostTopologicalOptimizationResult(TemporalData<RangeActionSetpointResult> allInitialSetPoints, TemporalData<PrePerimeterResult> prePerimeterResults, ObjectiveFunction objectiveFunction, TemporalData<RaoResult> topologicalOptimizationResults, TemporalData<State> preventiveStates) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = getRangeActionActivationResults(allInitialSetPoints, topologicalOptimizationResults, preventiveStates);
        TemporalData<NetworkActionsResult> networkActionsResults = getNetworkActionActivationResults(topologicalOptimizationResults, preventiveStates);
        return new GlobalLinearOptimizationResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult), prePerimeterResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL);
    }

    private static TemporalData<RangeActionActivationResult> getRangeActionActivationResults(TemporalData<RangeActionSetpointResult> allInitialSetPoints, TemporalData<RaoResult> topologicalOptimizationResults, TemporalData<State> preventiveStates) {
        Map<OffsetDateTime, RangeActionActivationResult> rangeActionsResults = new HashMap<>();
        topologicalOptimizationResults.getTimestamps().forEach(
            timestamp -> {
                State preventiveState = preventiveStates.getData(timestamp).orElseThrow();
                RangeActionSetpointResult initialSetPoints = allInitialSetPoints.getData(timestamp).orElseThrow();
                RangeActionSetpointResult optimizedSetPoints = new RangeActionSetpointResultImpl(topologicalOptimizationResults.getData(timestamp).orElseThrow().getOptimizedSetPointsOnState(preventiveState));
                RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(initialSetPoints);
                optimizedSetPoints.getRangeActions().forEach(rangeAction -> rangeActionActivationResult.putResult(rangeAction, preventiveState, optimizedSetPoints.getSetpoint(rangeAction)));
                rangeActionsResults.put(timestamp, rangeActionActivationResult);
            }
        );
        return new TemporalDataImpl<>(rangeActionsResults);
    }

    private static TemporalData<NetworkActionsResult> getNetworkActionActivationResults(TemporalData<RaoResult> topologicalOptimizationResults, TemporalData<State> preventiveStates) {
        Map<OffsetDateTime, NetworkActionsResult> networkActionsResults = new HashMap<>();
        topologicalOptimizationResults.getTimestamps().forEach(
            timestamp -> {
                State preventiveState = preventiveStates.getData(timestamp).orElseThrow();
                Set<NetworkAction> activatedNetworkActions = topologicalOptimizationResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(preventiveState);
                networkActionsResults.put(timestamp, new NetworkActionsResultImpl(Map.of(preventiveState, activatedNetworkActions)));
            }
        );
        return new TemporalDataImpl<>(networkActionsResults);
    }

    @Override
    public String getName() {
        return TIME_COUPLED_RAO;
    }
}
