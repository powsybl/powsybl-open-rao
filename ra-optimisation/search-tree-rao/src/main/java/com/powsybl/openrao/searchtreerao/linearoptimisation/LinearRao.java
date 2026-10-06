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
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
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
import com.powsybl.openrao.searchtreerao.marmot.TimeCoupledIteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.marmot.TimeCoupledIteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        List<OffsetDateTime> timestamps = raoInput.getRaoInputs().getTimestamps();
        TemporalData<Network> networks = raoInput.getRaoInputs().map(RaoInput::getNetwork);
        TemporalData<Crac> cracs = raoInput.getRaoInputs().map(RaoInput::getCrac);
        TemporalData<ToolProvider> toolProviders = raoInput.getRaoInputs().map(r -> ToolProvider.buildFromRaoInputAndParameters(r, raoParameters));
        TemporalData<PrePerimeterResult> initialFlowResults = new TemporalDataImpl<>();
        timestamps.forEach(timestamp -> {
            PrePerimeterSensitivityAnalysis initialSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                cracs.getData(timestamp).orElseThrow(),
                cracs.getData(timestamp).orElseThrow().getFlowCnecs(),
                cracs.getData(timestamp).orElseThrow().getRangeActions(),
                raoParameters,
                toolProviders.getData(timestamp).orElseThrow(),
                true
            );
            initialFlowResults.put(timestamp, initialSensitivityAnalysis.runInitialSensitivityAnalysis(networks.getData(timestamp).orElseThrow(), reportNode));
        });

        // create optimization perimeters
        TemporalData<OptimizationPerimeter> optimizationPerimeters = new TemporalDataImpl<>();
        timestamps.forEach(timestamp -> optimizationPerimeters.put(
            timestamp,
            GlobalOptimizationPerimeter.build(
                cracs.getData(timestamp).orElseThrow(),
                networks.getData(timestamp).orElseThrow(),
                raoParameters,
                initialFlowResults.getData(timestamp).orElseThrow(),
                reportNode
            )
        ));

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
        TemporalData<IteratingLinearOptimizerInput> optimizerInputsPerTimestamp = new TemporalDataImpl<>();
        timestamps.forEach(timestamp -> {
            PrePerimeterResult initialFlowResult = initialFlowResults.getData(timestamp).orElseThrow();
            PrePerimeterResult prePerimeterResult = initialFlowResult; // FIXME: use remedial actions from previous state
            PrePerimeterResult preOptimizationResult = initialFlowResult; // FIXME: use remedial actions from previous state and current state
            optimizerInputsPerTimestamp.put(timestamp, IteratingLinearOptimizerInput.create()
                .withNetwork(networks.getData(timestamp).orElseThrow())
                .withOptimizationPerimeter(optimizationPerimeters.getData(timestamp).orElseThrow())
                .withInitialFlowResult(initialFlowResult)
                .withPrePerimeterFlowResult(prePerimeterResult)
                .withPrePerimeterSetpoints(prePerimeterResult)
                .withPreOptimizationFlowResult(preOptimizationResult)
                .withPreOptimizationSensitivityResult(preOptimizationResult)
                .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
                .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(preOptimizationResult))
                .withAppliedNetworkActionsInPrimaryState(new NetworkActionsResultImpl(Map.of()))
                .withObjectiveFunction(globalObjectiveFunction)
                .withToolProvider(toolProviders.getData(timestamp).orElseThrow())
                .withOutageInstant(cracs.getData(timestamp).orElseThrow().getOutageInstant())
                .build());
        });
        TimeCoupledIteratingLinearOptimizerInput optimizerInputs = new TimeCoupledIteratingLinearOptimizerInput(
            optimizerInputsPerTimestamp, globalObjectiveFunction, raoInput.getTimeCoupledConstraints()
        );

        // create linear optimize parameters
        // TODO: define static method to define Ra Limitation Parameters from crac and topos (mutualize with search tree) : SearchTreeParameters::decreaseRemedialActionsUsageLimits
        // TODO: handle case when RAO Parameters do not have the OpenRaoSearchTreeParameters extension
        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder parameters = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(raoParameters.getObjectiveFunctionParameters().getType())
            .withFlowUnit(getFlowUnit(raoParameters))
            .withRangeActionParameters(raoParameters.getRangeActionsOptimizationParameters())
            .withRangeActionParametersExtension(raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters())
            .withMaxNumberOfIterations(raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(ENABLED.equals(raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking())
                || ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()))
            .withSolverParameters(raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxMinRelativeMarginParameters(raoParameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class))
            .withRaLimitationParameters(new RangeActionLimitationParameters())
            .withMinMarginParameters(raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().orElse(new SearchTreeRaoCostlyMinMarginParameters()));
        raoParameters.getMnecParameters().ifPresent(parameters::withMnecParameters);
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getMnecParameters().ifPresent(parameters::withMnecParametersExtension);
        raoParameters.getLoopFlowParameters().ifPresent(parameters::withLoopFlowParameters);
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters().ifPresent(parameters::withLoopFlowParametersExtension);
        IteratingLinearOptimizerParameters linearOptimizerParameters = parameters.build();

        // run optimizer
        // TODO: parametrize parallelism
        GlobalLinearOptimizationResult optimizationResult = TimeCoupledIteratingLinearOptimizer.optimize(optimizerInputs, linearOptimizerParameters, 1, reportNode);

        // convert optimization result to RAO Result
        // TODO: @Claude convert optimizationResult to a TimeCoupledRaoResult -> take inspiration in Marmot
        return null;
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode) {
        // FIXME: this will most likely need fixing because of the timestamp management
        // TODO: 1. [ ] Extract the linear code from MARMOT
        // TODO: 2. [ ] Allow refine inputs like in the search-tree
        // TODO: 3. [ ] Pass the different pre-perimeter results in an extension of the RaoInput
        // TODO: 4. [ ] Run on preventive and outage only
        // TODO: 5. [ ] Run on preventive only
        // TODO: 6. [ ] Run on curative only
        // TODO: 7. [ ] Add loopflows
        // TODO: 8. [ ] Add RA usage limits and decrement them from previous results
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

    private static IteratingLinearOptimizerInput getInput(RaoInput raoInput, RaoParameters raoParameters, ReportNode reportNode) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();

        // run initial sensitivity analysis
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
        PrePerimeterSensitivityAnalysis initialSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true
        );
        PrePerimeterResult initialResult = initialSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode);

        // TODO: distinguish from initialResult when applied topological actions are taken into account
        PrePerimeterResult prePerimeterResult = initialResult;
        PrePerimeterResult preOptimizationResult = initialResult;

        // build optimization perimeter and objective function
        OptimizationPerimeter optimizationPerimeter = GlobalOptimizationPerimeter.build(crac, network, raoParameters, initialResult, reportNode);
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
            optimizationPerimeter.getFlowCnecs(),
            optimizationPerimeter.getLoopFlowCnecs(),
            initialResult,
            prePerimeterResult,
            crac.findOperatorsNotSharingCras(),
            raoParameters,
            optimizationPerimeter.getRangeActionOptimizationStates()
        );

        return IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optimizationPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterFlowResult(prePerimeterResult)
            .withPrePerimeterSetpoints(prePerimeterResult)
            .withPreOptimizationFlowResult(preOptimizationResult)
            .withPreOptimizationSensitivityResult(preOptimizationResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(preOptimizationResult))
            .withAppliedNetworkActionsInPrimaryState(new NetworkActionsResultImpl(Map.of()))
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(raoInput.getCrac().getOutageInstant())
            .build();
    }

    private static ObjectiveFunction buildObjectiveFunction(TemporalData<Crac> cracs,
                                                            TemporalData<PrePerimeterResult> initialFlowResults,
                                                            RaoParameters raoParameters) {
        GlobalFlowResult globalInitialFlowResult = new GlobalFlowResult(initialFlowResults);
        return ObjectiveFunction.build(
            cracs.flatMap(Crac::getFlowCnecs),
            new HashSet<>(), // FIXME: no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial -> FIXME
            cracs.flatMap(Crac::findOperatorsNotSharingCras),
            raoParameters,
            cracs.flatMap(Crac::getStates)
        );
    }
}
