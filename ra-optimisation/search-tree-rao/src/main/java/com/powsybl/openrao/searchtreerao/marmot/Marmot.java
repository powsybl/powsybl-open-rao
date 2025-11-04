/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.marmot.scenariobuilder.GeneratorTargetPNetworkVariation;
import com.powsybl.openrao.searchtreerao.marmot.scenariobuilder.NetworkVariation;
import com.powsybl.openrao.searchtreerao.marmot.scenariobuilder.ScenarioRepo;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.marmot.results.InterTemporalRaoResultImpl;
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
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.logCost;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Marmot implements InterTemporalRaoProvider {

    private static final String INTER_TEMPORAL_RAO = "InterTemporalRao";
    private static final String VERSION = "1.0.0";

    private static final String INITIAL_SCENARIO = "InitialScenario";
    private static final String POST_TOPO_SCENARIO = "PostTopoScenario";
    private static final String MIP_SCENARIO = "MipScenario";
    private static final String MIN_MARGIN_VIOLATION_EVALUATOR = "min-margin-violation-evaluator";

    private static ScenarioRepo SCENARIO_REPO = null; // TODO this is a temp singleton ; make this cleaner

    public static ScenarioRepo getScenarioRepo() {
        return SCENARIO_REPO;
    }

    private static List<NetworkVariation> createNetworkVariationsForGenerator(String genId, TemporalData<Network> networks) {
        List<NetworkVariation> variations = new ArrayList<>();
        // Reference
        TemporalData<Double> valuesRef = new TemporalDataImpl<>();
        for (OffsetDateTime ts : networks.getTimestamps()) {
            valuesRef.put(ts, networks.getData(ts).orElseThrow().getGenerator(genId).getTargetP());
        }
        variations.add(new GeneratorTargetPNetworkVariation(genId + "_ref", genId, valuesRef));
        // MinP
        TemporalData<Double> valuesMin = new TemporalDataImpl<>();
        for (OffsetDateTime ts : networks.getTimestamps()) {
            valuesMin.put(ts, networks.getData(ts).orElseThrow().getGenerator(genId).getMinP());
        }
        variations.add(new GeneratorTargetPNetworkVariation(genId + "_min_P", genId, valuesMin));
        // MaxP
        TemporalData<Double> valuesMax = new TemporalDataImpl<>();
        for (OffsetDateTime ts : networks.getTimestamps()) {
            valuesMax.put(ts, networks.getData(ts).orElseThrow().getGenerator(genId).getMaxP());
        }
        variations.add(new GeneratorTargetPNetworkVariation(genId + "_max_P", genId, valuesMax));

        return variations;
    }

    private static ScenarioRepo createScenarios(TemporalData<Network> networks) {
        // TODO this should be done by various new input file imports
        Map<String, List<NetworkVariation>> networkVariations = new HashMap<>();
        Set<String> networkGenerators = networks.getData(networks.getTimestamps().get(0)).orElseThrow().getGeneratorStream().map(Identifiable::getId).filter(id -> !id.contains("_RA_")).collect(Collectors.toSet());
        for (String genId : networkGenerators) {
            networkVariations.put(genId, createNetworkVariationsForGenerator(genId, networks));
        }
        ScenarioRepo scenarioRepo = new ScenarioRepo(networkVariations.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
        int nScenarios = 10;
        Random rand = new Random();
        // Add reference scenario
        scenarioRepo.addScenario("REFERENCE", Set.of());
        while (scenarioRepo.getNumberOfScenarios() < nScenarios) {
            Set<String> scenarioVariations = new HashSet<>();
            for (String genId : networkGenerators) {
                NetworkVariation randomVariation = networkVariations.get(genId).get(rand.nextInt(networkVariations.get(genId).size()));
                scenarioVariations.add(randomVariation.getId());
            }
            //String scenarioId = scenarioVariations.stream().sorted().collect(Collectors.joining(" + "));
            String scenarioId = "scn_" + scenarioRepo.getNumberOfScenarios();
            scenarioRepo.addScenario(scenarioId, scenarioVariations);
        }

        return scenarioRepo;
    }

    @Override
    public CompletableFuture<InterTemporalRaoResult> run(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths, RaoParameters raoParameters) {
        // TODO make this cleaner
        InterTemporalRaoInput tmpInterTemporalRaoInput = importNetworksFromInterTemporalRaoInputWithNetworkPaths(interTemporalRaoInputWithNetworkPaths);
        TemporalData<Network> networks = tmpInterTemporalRaoInput.getRaoInputs().map(RaoInput::getNetwork);
        SCENARIO_REPO = createScenarios(networks);

        // 1. Run independent RAOs to compute optimal preventive topological remedial actions
        TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [start]");
        TemporalData<Set<FlowCnec>> consideredCnecs = new TemporalDataImpl<>();
        TemporalData<RaoResult> topologicalOptimizationResults = runTopologicalOptimization(interTemporalRaoInputWithNetworkPaths.getRaoInputs(), consideredCnecs, raoParameters);
        TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [end]");

        // 2. Get the initial results from the various independent results to avoid recomputing them
        Map<String, TemporalData<PrePerimeterResult>> initialResults = runInitialLoadFlow(tmpInterTemporalRaoInput.getRaoInputs(), raoParameters, false);

        // TODO : Add intertemporal constraint check if none violated then return
        boolean noInterTemporalConstraint = interTemporalRaoInputWithNetworkPaths.getGeneratorConstraints().isEmpty();

        // 3. Apply independent topological remedial actions (and preventive range actions if there are no inter-temporal constraints)
        InterTemporalRaoInput interTemporalRaoInput = importNetworksFromInterTemporalRaoInputWithNetworkPaths(interTemporalRaoInputWithNetworkPaths);
        TECHNICAL_LOGS.info("[MARMOT] Applying optimal topological actions on networks");
        Map<String, FlowResult> initialGlobalFlowResults = new HashMap<>();
        for (String scenario : SCENARIO_REPO.getScenarios()) {
            initialGlobalFlowResults.put(scenario, new GlobalFlowResult(initialResults.get(scenario)));
        }
        RobustObjectiveFunction fullObjectiveFunction = buildGlobalObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), initialGlobalFlowResults, raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction);

        // 4. Evaluate objective function after independent optimizations
        TECHNICAL_LOGS.info("[MARMOT] Evaluating global result after independent optimizations");
        /*TemporalData<PrePerimeterResult> postTopologicalActionsResults = topologicalOptimizationResults.map(
            raoResult -> ((FastRaoResultImpl) raoResult).getFinalResult()
        );
        TemporalData<RangeActionSetpointResult> initialSetpointResults = getInitialSetpointResults(topologicalOptimizationResults, interTemporalRaoInput.getRaoInputs());
        LinearOptimizationResult postTopologicalOptimizationResult = getPostTopologicalOptimizationResult(
            initialSetpointResults,
            postTopologicalActionsResults,
            fullObjectiveFunction,
            topologicalOptimizationResults,
            interTemporalRaoInput.getRaoInputs().map(individualRaoInput -> individualRaoInput.getCrac().getPreventiveState()));*/
        // TODO : reactivate topo optimization
        LinearOptimizationResult postTopologicalOptimizationResult = initialObjectiveFunctionResult;
        TemporalData<RangeActionSetpointResult> initialSetpointResults = getInitialSetpointResults(topologicalOptimizationResults, interTemporalRaoInput.getRaoInputs());

        // if no inter-temporal constraints are defined, the results can be returned
        if (noInterTemporalConstraint) {
            TECHNICAL_LOGS.info("[MARMOT] No inter-temporal constraint provided; no need to re-optimize range actions");
            return CompletableFuture.completedFuture(new InterTemporalRaoResultImpl(initialObjectiveFunctionResult, postTopologicalOptimizationResult, topologicalOptimizationResults));
        }

        // 5. Get and apply topological actions applied in independent optimizations
        TemporalData<NetworkActionsResult> preventiveTopologicalActions = getPreventiveTopologicalActions(interTemporalRaoInputWithNetworkPaths.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), topologicalOptimizationResults);
        applyPreventiveTopologicalActionsOnNetworks(interTemporalRaoInput.getRaoInputs(), preventiveTopologicalActions);

        // 6. Create and iteratively solve MIP to find optimal range actions' set-points
        // Get the curative ations applied in the individual results to be able to apply them during sensitivity computations
        TemporalData<AppliedRemedialActions> curativeRemedialActions = MarmotUtils.getAppliedRemedialActionsInCurative(interTemporalRaoInput.getRaoInputs(), topologicalOptimizationResults);

        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start]");
        // make fast rao result lighter by keeping only initial flow result and filtered rao result for actions
        replaceFastRaoResultsWithLightVersions(topologicalOptimizationResults);

        //TODO: loop
        Map<String, TemporalData<PrePerimeterResult>> loadFlowResults;
        GlobalLinearOptimizationResult linearOptimizationResults;
        GlobalLinearOptimizationResult fullResults;
        int counter = 1;
        do {
            // Clone the PostTopoScenario variant to make sure we work on a clean variant every time
            interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().values().forEach(raoInput -> {
                raoInput.getNetwork().getVariantManager().cloneVariant(POST_TOPO_SCENARIO, MIP_SCENARIO, true);
                raoInput.getNetwork().getVariantManager().setWorkingVariant(MIP_SCENARIO);
            });

            // Run post topo sensitivity analysis on all timestamps ON CONSIDERED CNECS ONLY (which is why we do it every loop)
            TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [start]");
            // TODO reactivate topo optimization
            for (String scenario : SCENARIO_REPO.getScenarios()) {

            }
            Map<String, TemporalData<PrePerimeterResult>> postTopoResults = runInitialLoadFlow(tmpInterTemporalRaoInput.getRaoInputs(), raoParameters, true);
            TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [end]");

            // Build objective function with ONLY THE CONSIDERED CNECS
            RobustObjectiveFunction filteredObjectiveFunction = buildFilteredObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), initialGlobalFlowResults, raoParameters, consideredCnecs);

            // Create and iteratively solve MIP to find optimal range actions' set-points FOR THE CONSIDERED CNECS
            TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start] for iteration {}", counter);
            linearOptimizationResults = optimizeLinearRemedialActions(interTemporalRaoInput, initialResults, initialSetpointResults, postTopoResults, raoParameters, preventiveTopologicalActions, curativeRemedialActions, consideredCnecs, filteredObjectiveFunction);
            TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end] for iteration {}", counter);

            // Compute the flows on ALL the cnecs to check if the worst cnecs have changed and were considered in the MIP or not
            loadFlowResults = applyActionsAndRunFullLoadflow(interTemporalRaoInput.getRaoInputs(), curativeRemedialActions, linearOptimizationResults, initialResults, raoParameters);

            // Create a global result with the flows on ALL cnecs and the actions applied during MIP
            TemporalData<RangeActionActivationResult> rangeActionActivationResultTemporalData = linearOptimizationResults.getRangeActionActivationResultTemporalData();
            Map<String, TemporalData<SensitivityResult>> sensitivityResults = new HashMap<>();
            for (String scenario : loadFlowResults.keySet()) {
                sensitivityResults.put(scenario, loadFlowResults.get(scenario).map(PrePerimeterResult::getSensitivityResult));
            }
            Map<String, TemporalData<FlowResult>> casted = new HashMap<>();
            for (String scenario : loadFlowResults.keySet()) {
                casted.put(scenario, loadFlowResults.get(scenario).map(FlowResult.class::cast));
            }
            fullResults = new GlobalLinearOptimizationResult(casted, sensitivityResults, rangeActionActivationResultTemporalData, preventiveTopologicalActions, fullObjectiveFunction, LinearProblemStatus.OPTIMAL);

            logCost("[MARMOT] next iteration of MIP: ", fullResults, raoParameters, 10);
            counter++;
        } while (shouldContinueAndAddCnecs(loadFlowResults, consideredCnecs) && counter < 10); // Stop if the worst element of each TS has been considered during MIP
        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end]");

        // 7. Merge topological and linear result
        TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");
        // Selecting reference and hoping it works
        // TODO make this cleaner
        InterTemporalRaoResultImpl interTemporalRaoResult = mergeTopologicalAndLinearOptimizationResults(interTemporalRaoInput.getRaoInputs(), initialResults.get("REFERENCE"), initialObjectiveFunctionResult, fullResults, topologicalOptimizationResults, raoParameters);

        // 8. Log initial and final results
        logCost("[MARMOT] Before topological optimizations: ", initialObjectiveFunctionResult, raoParameters, 10);
        logCost("[MARMOT] Before global linear optimization: ", postTopologicalOptimizationResult, raoParameters, 10);
        logCost("[MARMOT] After global linear optimization: ", fullResults, raoParameters, 10);

        return CompletableFuture.completedFuture(interTemporalRaoResult);
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

    private boolean shouldContinueAndAddCnecs(Map<String, TemporalData<PrePerimeterResult>> loadFlowResults, TemporalData<Set<FlowCnec>> consideredCnecs) {
        int cnecsToAddPerVirtualCostName = 20;
        double minRelativeImprovementOnMargin = 0.1;
        double marginWindowToConsider = 5.0;

        AtomicBoolean shouldContinue = new AtomicBoolean(false);
        updateShouldContinue(loadFlowResults, consideredCnecs, minRelativeImprovementOnMargin, shouldContinue);

        if (shouldContinue.get()) {
            updateConsideredCnecs(loadFlowResults, consideredCnecs, marginWindowToConsider, cnecsToAddPerVirtualCostName);
        }
        return shouldContinue.get();
    }

    private static void updateShouldContinue(Map<String, TemporalData<PrePerimeterResult>> loadFlowResults, TemporalData<Set<FlowCnec>> consideredCnecs, double minRelativeImprovementOnMargin, AtomicBoolean shouldContinue) {
        for (TemporalData<PrePerimeterResult> scenarioLoadFlowResult : loadFlowResults.values()) {
            scenarioLoadFlowResult.getTimestamps().forEach(timestamp -> {
                PrePerimeterResult loadFlowResult = scenarioLoadFlowResult.getData(timestamp).orElseThrow();
                Set<FlowCnec> previousCnecs = consideredCnecs.getData(timestamp).orElseThrow();

                // for margin violation - need to compare to min improvement on margin
                // ordered list of cnecs with an overload
                List<FlowCnec> worstCnecsForMarginViolation = loadFlowResult.getCostlyElements(MIN_MARGIN_VIOLATION_EVALUATOR, Integer.MAX_VALUE);
                double worstConsideredMargin = worstCnecsForMarginViolation.stream()
                    .filter(previousCnecs::contains)
                    .findFirst()
                    .map(cnec -> loadFlowResult.getMargin(cnec, Unit.MEGAWATT))
                    .orElse(0.);
                double worstMarginOfAll = worstCnecsForMarginViolation.stream()
                    .findFirst()
                    .map(cnec -> loadFlowResult.getMargin(cnec, Unit.MEGAWATT))
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
    }

    private static void updateConsideredCnecs(Map<String, TemporalData<PrePerimeterResult>> loadFlowResults, TemporalData<Set<FlowCnec>> consideredCnecs, double marginWindowToConsider, int cnecsToAddPerVirtualCostName) {
        List<LoggingAddedCnecs> addedCnecsForLogging = new ArrayList<>();
        for (TemporalData<PrePerimeterResult> scenarioLoadFlowResult : loadFlowResults.values()) {
            scenarioLoadFlowResult.getTimestamps().forEach(timestamp -> {
                PrePerimeterResult loadFlowResult = scenarioLoadFlowResult.getData(timestamp).orElseThrow();
                Set<FlowCnec> previousIterationCnecs = consideredCnecs.getData(timestamp).orElseThrow();
                Set<FlowCnec> nextIterationCnecs = new HashSet<>(previousIterationCnecs);

                double worstConsideredMargin = loadFlowResult.getCostlyElements(MIN_MARGIN_VIOLATION_EVALUATOR, Integer.MAX_VALUE)
                    .stream()
                    .filter(previousIterationCnecs::contains)
                    .findFirst()
                    .map(cnec -> loadFlowResult.getMargin(cnec, Unit.MEGAWATT))
                    .orElse(0.);

                loadFlowResult.getVirtualCostNames().forEach(vcName -> {
                    LoggingAddedCnecs currentLoggingAddedCnecs = new LoggingAddedCnecs(timestamp, vcName, new ArrayList<>(), new HashMap<>());
                    int addedCnecsForVcName = 0;

                    // for min margin violation take all cnecs
                    if (vcName.equals(MIN_MARGIN_VIOLATION_EVALUATOR)) {
                        for (FlowCnec cnec : loadFlowResult.getCostlyElements(vcName, Integer.MAX_VALUE)) {
                            if (loadFlowResult.getMargin(cnec, Unit.MEGAWATT) > worstConsideredMargin + marginWindowToConsider && addedCnecsForVcName > cnecsToAddPerVirtualCostName) {
                                // stop if out of window and already added enough
                                break;
                            } else if (!previousIterationCnecs.contains(cnec)) {
                                // if in window or not added enough yet, add
                                nextIterationCnecs.add(cnec);
                                addedCnecsForVcName++;
                                currentLoggingAddedCnecs.addCnec(cnec.getId(), loadFlowResult.getMargin(cnec, Unit.MEGAWATT));
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
        }
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

    record LoggingAddedCnecs(OffsetDateTime offsetDateTime, String vcName, List<String> addedCnecs,
                             Map<String, Double> margins) {
        private void addCnec(String cnec) {
            addedCnecs.add(cnec);
        }

        private void addCnec(String cnec, double margin) {
            addedCnecs.add(cnec);
            margins.put(cnec, margin);
        }
    }

    private static Map<String, TemporalData<PrePerimeterResult>> applyActionsAndRunFullLoadflow(TemporalData<RaoInput> raoInputs, TemporalData<AppliedRemedialActions> curativeRemedialActions, LinearOptimizationResult filteredResult, Map<String, TemporalData<PrePerimeterResult>> initialResults, RaoParameters raoParameters) {
        Map<String, TemporalData<PrePerimeterResult>> results = new HashMap<>();
        for (String scenario : SCENARIO_REPO.getScenarios()) {
            TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
            raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
                // duplicate the postTopoScenario variant and switch to the new clone
                raoInput.getNetwork().getVariantManager().cloneVariant(POST_TOPO_SCENARIO, "PostPreventiveScenario", true);
                raoInput.getNetwork().getVariantManager().setWorkingVariant("PostPreventiveScenario");
                SCENARIO_REPO.applyScenario(scenario, raoInput.getNetwork(), timestamp);
                State preventiveState = raoInput.getCrac().getPreventiveState();
                raoInput.getCrac().getRangeActions(preventiveState).forEach(rangeAction ->
                    rangeAction.apply(raoInput.getNetwork(), filteredResult.getOptimizedSetpoint(rangeAction, preventiveState))
                );
                prePerimeterResults.put(timestamp, runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(
                    raoInputs.getData(timestamp).orElseThrow(),
                    curativeRemedialActions.getData(timestamp).orElseThrow(),
                    initialResults.get(scenario).getData(timestamp).orElseThrow(),
                    raoParameters));
                // switch back to the postTopoScenario to avoid keeping applied range actions when entering the MIP
                raoInput.getNetwork().getVariantManager().setWorkingVariant(POST_TOPO_SCENARIO);
            });
            results.put(scenario, prePerimeterResults);
        }
        return results;
    }


    private static Map<String, TemporalData<PrePerimeterResult>> runInitialLoadFlow(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters, boolean withRangeActions) {
        Map<String, TemporalData<PrePerimeterResult>> results = new HashMap<>();
        for (String scenario : SCENARIO_REPO.getScenarios()) {
            TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
            raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
                // duplicate the postTopoScenario variant and switch to the new clone
                String initialVariant = raoInput.getNetwork().getVariantManager().getWorkingVariantId();
                String tmpVariant = "InitialLoadFlowVariant";
                raoInput.getNetwork().getVariantManager().cloneVariant(initialVariant, tmpVariant, true);
                raoInput.getNetwork().getVariantManager().setWorkingVariant(tmpVariant);
                SCENARIO_REPO.applyScenario(scenario, raoInput.getNetwork(), timestamp);
                // TODO apply topological results here ?
                Set<RangeAction<?>> rangeActions = Set.of();
                if (withRangeActions) {
                    Crac crac = raoInput.getCrac();
                    State preventiveState = crac.getPreventiveState();
                    rangeActions = crac.getRangeActions(preventiveState);
                }
                prePerimeterResults.put(timestamp, runInitialPrePerimeterSensitivityAnalysisWithRangeActions(
                    raoInputs.getData(timestamp).orElseThrow(),
                    raoParameters,
                    rangeActions));
                raoInput.getNetwork().getVariantManager().setWorkingVariant(initialVariant);
                raoInput.getNetwork().getVariantManager().removeVariant(tmpVariant);
            });
            results.put(scenario, prePerimeterResults);
        }
        return results;
    }

    private void replaceFastRaoResultsWithLightVersions(TemporalData<RaoResult> topologicalOptimizationResults) {
        topologicalOptimizationResults.getDataPerTimestamp().forEach((timestamp, raoResult) -> topologicalOptimizationResults.put(timestamp, new LightFastRaoResultImpl((FastRaoResultImpl) raoResult)));
    }

    private InterTemporalRaoInput importNetworksFromInterTemporalRaoInputWithNetworkPaths(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths) {
        return new InterTemporalRaoInput(
            interTemporalRaoInputWithNetworkPaths.getRaoInputs().map(raoInputWithNetworksPath -> {
                RaoInput raoInput = raoInputWithNetworksPath.toRaoInputWithPostIcsImportNetworkPath();
                raoInput.getNetwork().getVariantManager().cloneVariant(raoInput.getNetworkVariantId(), INITIAL_SCENARIO);
                return raoInput;
            }),
            interTemporalRaoInputWithNetworkPaths.getTimestampsToRun(),
            interTemporalRaoInputWithNetworkPaths.getGeneratorConstraints()
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

    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalysesBasedOnInitialResult(TemporalData<RaoInput> raoInputs, TemporalData<AppliedRemedialActions> curativeRemedialActions, TemporalData<? extends FlowResult> initialFlowResults, RaoParameters raoParameters, TemporalData<Set<FlowCnec>> consideredCnecs) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getTimestamps().forEach(timestamp -> {
            prePerimeterResults.put(timestamp, runSensitivityAnalysisBasedOnInitialResult(
                raoInputs.getData(timestamp).orElseThrow(),
                curativeRemedialActions.getData(timestamp).orElseThrow(),
                initialFlowResults.getData(timestamp).orElseThrow(),
                raoParameters,
                consideredCnecs.getData(timestamp).orElseThrow()
            ));
        });
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

    private static GlobalLinearOptimizationResult optimizeLinearRemedialActions(InterTemporalRaoInput raoInput, Map<String, TemporalData<PrePerimeterResult>> initialResults, TemporalData<RangeActionSetpointResult> initialSetpoints, Map<String, TemporalData<PrePerimeterResult>> postTopologicalActionsResults, RaoParameters parameters, TemporalData<NetworkActionsResult> preventiveTopologicalActions, TemporalData<AppliedRemedialActions> curativeRemedialActions, TemporalData<Set<FlowCnec>> consideredCnecs, RobustObjectiveFunction objectiveFunction) {

        // -- Build IteratingLinearOptimizerInterTemporalInput
        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInput::getCrac), consideredCnecs);
        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global
        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
        // Sending reference flow results, the iterating optimizer will shift them using sensi
        // TODO : make it cleaner by sending real loadflow results
        raoInput.getRaoInputs().getTimestamps().forEach(timestamp -> {
            Map<String, FlowResult> initialResultsPerScenario = new HashMap<>();
            Map<String, FlowResult> preoptimResultsPerScenario = new HashMap<>();
            Map<String, SensitivityResult> sensiResultsPerScenario = new HashMap<>();
            for (String scenario : initialResults.keySet()) {
                initialResultsPerScenario.put(scenario, initialResults.get(scenario).getData(timestamp).orElseThrow());
                preoptimResultsPerScenario.put(scenario, postTopologicalActionsResults.get(scenario).getData(timestamp).orElseThrow());
                sensiResultsPerScenario.put(scenario, postTopologicalActionsResults.get(scenario).getData(timestamp).orElseThrow());
            }
            linearOptimizerInputPerTimestamp.put(timestamp, IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getNetwork())
            .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow())
            .withInitialFlowResultPerScenario(initialResultsPerScenario)
            .withPrePerimeterFlowResultPerScenario(initialResultsPerScenario)
            .withPreOptimizationFlowResultPerScenario(preoptimResultsPerScenario)
            .withPrePerimeterSetpoints(initialSetpoints.getData(timestamp).orElseThrow())
            .withPreOptimizationSensitivityResultPerScenario(sensiResultsPerScenario)
            .withPreOptimizationAppliedRemedialActions(curativeRemedialActions.getData(timestamp).orElseThrow())
            .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
            .withOutageInstant(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getOutageInstant())
            .withAppliedNetworkActionsInPrimaryState(preventiveTopologicalActions.getData(timestamp).orElseThrow())
            .build());
        }
        );
        InterTemporalIteratingLinearOptimizerInput interTemporalLinearOptimizerInput = new InterTemporalIteratingLinearOptimizerInput(new TemporalDataImpl<>(linearOptimizerInputPerTimestamp), objectiveFunction, raoInput.getGeneratorConstraints());

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
            .withRaLimitationParameters(new RangeActionLimitationParameters())
            .withMinMarginParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().orElse(new SearchTreeRaoCostlyMinMarginParameters()));
        parameters.getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParametersExtension);
        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParametersExtension);
        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();

        //return InterTemporalIteratingLinearOptimizer.optimize(interTemporalLinearOptimizerInput, linearOptimizerParameters);
        return RobustInterTemporalIteratingLinearOptimizer.optimize(interTemporalLinearOptimizerInput, linearOptimizerParameters, SCENARIO_REPO);
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

    private static InterTemporalRaoResultImpl mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> initialResults, ObjectiveFunctionResult initialLinearOptimizationResult, GlobalLinearOptimizationResult globalLinearOptimizationResult, TemporalData<RaoResult> topologicalOptimizationResults, RaoParameters raoParameters) {
        return new InterTemporalRaoResultImpl(
            initialLinearOptimizationResult,
            globalLinearOptimizationResult,
            getPostOptimizationResults(
                raoInputs,
                initialResults,
                globalLinearOptimizationResult,
                topologicalOptimizationResults,
                raoParameters));
    }

    private static RobustObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, Map<String, FlowResult> globalInitialFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(MarmotUtils::getPreventivePerimeterCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return RobustObjectiveFunction.build(
            SCENARIO_REPO,
            allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private static RobustObjectiveFunction buildFilteredObjectiveFunction(TemporalData<Crac> cracs, Map<String, FlowResult> globalInitialFlowResult, RaoParameters raoParameters, TemporalData<Set<FlowCnec>> consideredCnecs) {
        Set<FlowCnec> flatConsideredCnecs = new HashSet<>();
        consideredCnecs.getDataPerTimestamp().values().forEach(flatConsideredCnecs::addAll);

        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return RobustObjectiveFunction.build(
            SCENARIO_REPO,
            flatConsideredCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private LinearOptimizationResult getInitialObjectiveFunctionResult(Map<String, TemporalData<PrePerimeterResult>> prePerimeterResults, RobustObjectiveFunction objectiveFunction) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = prePerimeterResults.values().stream().findAny().orElseThrow().map(RangeActionActivationResultImpl::new); // all RA activations should be the same
        TemporalData<NetworkActionsResult> networkActionsResults = new TemporalDataImpl<>();
        Map<String, TemporalData<SensitivityResult>> sensitivityResults = new HashMap<>();
        for (String scenario : prePerimeterResults.keySet()) {
            sensitivityResults.put(scenario, prePerimeterResults.get(scenario).map(PrePerimeterResult::getSensitivityResult));
        }
        Map<String, TemporalData<FlowResult>> casted = new HashMap<>();
        for (String scenario : prePerimeterResults.keySet()) {
            casted.put(scenario, prePerimeterResults.get(scenario).map(FlowResult.class::cast));
        }
        return new GlobalLinearOptimizationResult(casted, sensitivityResults, rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL);
    }

    private LinearOptimizationResult getPostTopologicalOptimizationResult(TemporalData<RangeActionSetpointResult> allInitialSetPoints, Map<String, TemporalData<PrePerimeterResult>> prePerimeterResults, RobustObjectiveFunction objectiveFunction, TemporalData<RaoResult> topologicalOptimizationResults, TemporalData<State> preventiveStates) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = getRangeActionActivationResults(allInitialSetPoints, topologicalOptimizationResults, preventiveStates);
        TemporalData<NetworkActionsResult> networkActionsResults = getNetworkActionActivationResults(topologicalOptimizationResults, preventiveStates);
        Map<String, TemporalData<SensitivityResult>> sensitivityResults = new HashMap<>();
        for (String scenario : prePerimeterResults.keySet()) {
            sensitivityResults.put(scenario, prePerimeterResults.get(scenario).map(PrePerimeterResult::getSensitivityResult));
        }
        Map<String, TemporalData<FlowResult>> casted = new HashMap<>();
        for (String scenario : prePerimeterResults.keySet()) {
            casted.put(scenario, prePerimeterResults.get(scenario).map(FlowResult.class::cast));
        }
        return new GlobalLinearOptimizationResult(casted, sensitivityResults, rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL);
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
        return INTER_TEMPORAL_RAO;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
