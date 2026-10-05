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
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    public CompletableFuture<TimeCoupledRaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return null;
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode) {
        // FIXME: this will most likely need fixing because of the timestamp management
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
}
