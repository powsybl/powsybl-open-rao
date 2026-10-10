/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.searchtreerao.marmot.TimeCoupledIteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.marmot.TimeCoupledIteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.marmot.results.TimeCoupledRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.UnoptimizedRaoResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;

/**
 * RAO Provider that optimized only linear remedial actions (PST, Redispatching).
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService({RaoProvider.class, TimeCoupledRaoProvider.class})
public class LinearRao implements RaoProvider, TimeCoupledRaoProvider {
    private static final String PROVIDER_NAME = "LinearRAO"; // TODO: Use LOUTRE name? (Linear Optimizer Using Transformers and REdispatching)

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public CompletableFuture<TimeCoupledRaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters raoParameters, ReportNode reportNode) {
        // TODO: 1. [X] Extract the linear code from MARMOT
        // TODO: 2. [ ] Allow refine inputs like in the search-tree
        // TODO: 3. [ ] Pass the different pre-perimeter results in an extension of the RaoInput
        // TODO: 4. [ ] Run on preventive and outage only
        // TODO: 5. [ ] Run on preventive only
        // TODO: 6. [ ] Run on curative only
        // TODO: 7. [X] Add loopflows
        // TODO: 8. [ ] Add RA usage limits and decrement them from previous results
        List<OffsetDateTime> timestamps = raoInput.getRaoInputs().getTimestamps();
        TemporalData<Network> networks = raoInput.getRaoInputs().map(RaoInput::getNetwork);
        TemporalData<Crac> cracs = raoInput.getRaoInputs().map(RaoInput::getCrac);
        int parallelism = Math.min(MultithreadingParameters.getAvailableCPUs(raoParameters), timestamps.size());

        // run initial sensitivity analysis
        TemporalData<ToolProvider> toolProviders = raoInput.getRaoInputs().map(r -> ToolProvider.buildFromRaoInputAndParameters(r, raoParameters));
        TemporalData<PrePerimeterResult> initialFlowResults = TemporalData.map(
            networks, cracs, toolProviders,
            (network, crac, toolProvider) -> LinearRaoUtils.runInitialSensitivityAnalysis(network, crac, toolProvider, raoParameters, reportNode)
        );

        // create optimization perimeters
        TemporalData<OptimizationPerimeter> optimizationPerimeters = TemporalData.map(
            networks, cracs, initialFlowResults,
            (network, crac, initialFlowResult) -> GlobalOptimizationPerimeter.build(
                crac,
                network,
                raoParameters,
                initialFlowResult,
                reportNode
            )
        );

        // create global objective function
        GlobalFlowResult initialGlobalFlowResult = new GlobalFlowResult(initialFlowResults);
        ObjectiveFunction globalObjectiveFunction = ObjectiveFunction.build(
            optimizationPerimeters.flatMap(OptimizationPerimeter::getFlowCnecs),
            optimizationPerimeters.flatMap(OptimizationPerimeter::getLoopFlowCnecs),
            initialGlobalFlowResult,
            initialGlobalFlowResult, // always building from preventive so prePerimeter = initial -> FIXME
            cracs.flatMap(Crac::findOperatorsNotSharingCras),
            raoParameters,
            optimizationPerimeters.flatMap(OptimizationPerimeter::getRangeActionOptimizationStates)
        );

        // create linear optimizer inputs
        TemporalData<IteratingLinearOptimizerInput> optimizerInputsPerTimestamp = TemporalData.map(
            raoInput.getRaoInputs(), initialFlowResults, optimizationPerimeters, toolProviders,
            (input, initialFlowResult, optimizationPerimeter, toolProvider) ->
                LinearRaoUtils.buildOptimizerInput(input, optimizationPerimeter, initialFlowResult, toolProvider, globalObjectiveFunction)
        );
        TimeCoupledIteratingLinearOptimizerInput optimizerInputs = new TimeCoupledIteratingLinearOptimizerInput(
            optimizerInputsPerTimestamp, globalObjectiveFunction, raoInput.getTimeCoupledConstraints()
        );
        IteratingLinearOptimizerParameters linearOptimizerParameters = buildOptimizerParameters(raoParameters, optimizationPerimeters, cracs, reportNode);

        // run optimizer
        GlobalLinearOptimizationResult optimizationResult = TimeCoupledIteratingLinearOptimizer.optimize(optimizerInputs, linearOptimizerParameters, parallelism, reportNode);

        // convert optimization result to RAO Result
        // TODO: remove dependency to MARMOT code
        TemporalData<Set<NetworkAction>> preventiveNetworkActions = new TemporalDataImpl<>();
        TemporalData<AppliedRemedialActions> curativeNetworkActions = new TemporalDataImpl<>();
        TemporalData<Set<FlowCnec>> consideredCnecs = new TemporalDataImpl<>();
        timestamps.forEach(timestamp -> {
            preventiveNetworkActions.put(timestamp, new HashSet<>()); // network actions are not optimized
            curativeNetworkActions.put(timestamp, new AppliedRemedialActions());
            consideredCnecs.put(timestamp, cracs.getData(timestamp).orElseThrow().getFlowCnecs());
        });
        GlobalLinearOptimizationResult initialOptimizationResult = new GlobalLinearOptimizationResult(
            initialFlowResults.map(PrePerimeterResult::getFlowResult),
            initialFlowResults.map(PrePerimeterResult::getSensitivityResult),
            initialFlowResults.map(RangeActionActivationResultImpl::new),
            new TemporalDataImpl<>(),
            globalObjectiveFunction,
            LinearProblemStatus.OPTIMAL,
            reportNode
        );
        TimeCoupledRaoResultImpl result = new TimeCoupledRaoResultImpl(
            initialOptimizationResult,
            optimizationResult,
            MarmotUtils.getPostOptimizationResults(
                raoInput.getRaoInputs(),
                initialFlowResults,
                optimizationResult,
                preventiveNetworkActions,
                curativeNetworkActions,
                consideredCnecs,
                raoParameters,
                reportNode
            )
        );
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, java.time.Instant targetEndInstant, ReportNode reportNode) {
        // FIXME: this will most likely need fixing because of the timestamp management
        if (raoInput.getCrac().getFlowCnecs().isEmpty()) {
            Crac crac = raoInput.getCrac();
            PrePerimeterResult initialResult = new PrePerimeterSensitivityAnalysis(
                crac, crac.getFlowCnecs(), crac.getRangeActions(), parameters, ToolProvider.buildFromRaoInputAndParameters(raoInput, parameters), true
            ).runInitialSensitivityAnalysis(raoInput.getNetwork(), reportNode);
            return CompletableFuture.completedFuture(new UnoptimizedRaoResultImpl(initialResult));
        }
        OffsetDateTime timestamp = raoInput.getCrac().getTimestamp().orElse(OffsetDateTime.now());
        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>();
        raoInputs.put(timestamp, raoInput);
        TimeCoupledRaoInput timeCoupledRaoInput = new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints());
        try {
            return CompletableFuture.completedFuture(
                run(timeCoupledRaoInput, parameters, reportNode).get().getIndividualRaoResult(timestamp)
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new OpenRaoException("LinearRAO failed: " + e);
        }
    }

    private static IteratingLinearOptimizerParameters buildOptimizerParameters(RaoParameters raoParameters,
                                                                               TemporalData<OptimizationPerimeter> optimizationPerimeters,
                                                                               TemporalData<Crac> cracs,
                                                                               ReportNode reportNode) {
        // TODO: define static method to define Ra Limitation Parameters from crac and topos (mutualize with search tree) : SearchTreeParameters::decreaseRemedialActionsUsageLimits
        OpenRaoSearchTreeParameters openRaoSearchTreeParameters = raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
            ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class)
            : new OpenRaoSearchTreeParameters(reportNode);
        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder parameters = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(raoParameters.getObjectiveFunctionParameters().getType())
            .withFlowUnit(getFlowUnit(raoParameters))
            .withRangeActionParameters(raoParameters.getRangeActionsOptimizationParameters())
            .withRangeActionParametersExtension(openRaoSearchTreeParameters.getRangeActionsOptimizationParameters())
            .withMaxNumberOfIterations(openRaoSearchTreeParameters.getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(ENABLED.equals(openRaoSearchTreeParameters.getRangeActionsOptimizationParameters().getRaRangeShrinking())
                || ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(openRaoSearchTreeParameters.getRangeActionsOptimizationParameters().getRaRangeShrinking()))
            .withSolverParameters(openRaoSearchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxMinRelativeMarginParameters(raoParameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class))
            .withRaLimitationParameters(getRaLimitationParameters(optimizationPerimeters, cracs))
            .withMinMarginParameters(openRaoSearchTreeParameters.getMinMarginsParameters().orElse(new SearchTreeRaoCostlyMinMarginParameters()));
        raoParameters.getMnecParameters().ifPresent(parameters::withMnecParameters);
        openRaoSearchTreeParameters.getMnecParameters().ifPresent(parameters::withMnecParametersExtension);
        raoParameters.getLoopFlowParameters().ifPresent(parameters::withLoopFlowParameters);
        openRaoSearchTreeParameters.getLoopFlowParameters().ifPresent(parameters::withLoopFlowParametersExtension);
        return parameters.build();
    }

    private static RangeActionLimitationParameters getRaLimitationParameters(TemporalData<OptimizationPerimeter> optimizationPerimeters, TemporalData<Crac> cracs) {
        RangeActionLimitationParameters limitationParameters = new RangeActionLimitationParameters();
        List<OffsetDateTime> timestamps = cracs.getTimestamps();

        for (OffsetDateTime timestamp : timestamps) {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            Map<Instant, RaUsageLimits> usageLimits = crac.getRaUsageLimitsPerInstant();
            OptimizationPerimeter optimizationPerimeter = optimizationPerimeters.getData(timestamp).orElseThrow();
            for (State state : optimizationPerimeter.getRangeActionOptimizationStates()) {
                if (usageLimits.containsKey(state.getInstant())) {
                    RaUsageLimits raUsageLimits = usageLimits.get(state.getInstant());
                    Set<NetworkAction> appliedNetworkActions = Set.of();
                    Integer maxRa = raUsageLimits.getMaxRa();
                    if (maxRa != null) {
                        maxRa = raUsageLimits.getMaxRa() - appliedNetworkActions.size();
                    }

                    Map<String, Integer> maxPstPerTso = raUsageLimits.getMaxPstPerTso();
                    Map<String, Integer> maxRaPerTso = new HashMap<>(raUsageLimits.getMaxRaPerTso());
                    maxRaPerTso.entrySet().forEach(entry -> {
                        int alreadyActivatedNetworkActionsForTso = appliedNetworkActions.stream().filter(na -> entry.getKey().equals(na.getOperator())).collect(
                            Collectors.toSet()).size();
                        entry.setValue(entry.getValue() - alreadyActivatedNetworkActionsForTso);
                    });
                    Map<String, Integer> maxElementaryActionsPerTso = new HashMap<>(raUsageLimits.getMaxElementaryActionsPerTso());
                    maxElementaryActionsPerTso.entrySet().forEach(entry -> {
                        int alreadyActivatedNetworkActionsForTso = appliedNetworkActions.stream()
                            .filter(na -> entry.getKey().equals(na.getOperator()))
                            .mapToInt(na -> na.getElementaryActions().size())
                            .sum();
                        entry.setValue(Math.max(0, entry.getValue() - alreadyActivatedNetworkActionsForTso));
                    });

                    limitationParameters.setMaxRangeAction(state, maxRa);
                    limitationParameters.setMaxPstPerTso(state, maxPstPerTso);
                    limitationParameters.setMaxRangeActionPerTso(state, maxRaPerTso);
                    limitationParameters.setMaxElementaryActionsPerTso(state, maxElementaryActionsPerTso);
                }
            }
        }

        return limitationParameters;
    }
}
