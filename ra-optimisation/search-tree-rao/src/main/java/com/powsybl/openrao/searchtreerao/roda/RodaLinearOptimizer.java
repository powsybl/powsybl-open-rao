/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.MultiScenarioTemporalData;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.ProblemFillerHelper;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemIdGenerator;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.LinearProblemResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class RodaLinearOptimizer {

    private RodaLinearOptimizer() {
    }

    public static TemporalData<RangeActionActivationResult> optimize(
        TemporalData<Crac> cracs,
        TemporalData<Network> networks,
        MultiScenarioTemporalData<FlowAndSensitivityResult> initialFlowsAndSensis,
        RaoParameters raoParameters) {
        IteratingLinearOptimizerParameters parameters = buildParameters(raoParameters);

        LinearProblem linearProblem = LinearProblem.create()
            .withSolver(parameters.getSolverParameters().getSolver())
            .withRelativeMipGap(parameters.getSolverParameters().getRelativeMipGap())
            .withSolverSpecificParameters(parameters.getSolverParameters().getSolverSpecificParameters())
            .build();

        TemporalData<RangeActionSetpointResult> prePerimeterSetpoints = new TemporalDataImpl<>();
        TemporalData<OptimizationPerimeter> optimizationPerimeters = new TemporalDataImpl<>();
        for (OffsetDateTime timestamp : initialFlowsAndSensis.getTimestamps()) {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            Network network = networks.getData(timestamp).orElseThrow();
            optimizationPerimeters.put(timestamp, getPreventiveOptimizationPerimeter(crac));
            prePerimeterSetpoints.put(timestamp, RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, crac.getRangeActions()));
        }

        for (String scenario : initialFlowsAndSensis.getScenarios()) {
            // TODO set prefix in LinearProblem
            LinearProblemIdGenerator.setPrefix(String.format("scenario(%s)", scenario));
            for (OffsetDateTime timestamp : initialFlowsAndSensis.getTimestamps()) {
                Network network = networks.getData(timestamp).orElseThrow();
                FlowAndSensitivityResult flowAndSensi = initialFlowsAndSensis.get(scenario, timestamp).orElseThrow();
                OptimizationPerimeter optimizationPerimeter = optimizationPerimeters.getData(timestamp).orElseThrow();
                List<ProblemFiller> fillers = ProblemFillerHelper.getProblemFillers(
                    optimizationPerimeter,
                    network,
                    RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, optimizationPerimeter.getRangeActions()),
                    flowAndSensi,
                    flowAndSensi,
                    flowAndSensi,
                    parameters,
                    timestamp
                );
                fillers.forEach(filler -> filler.fill(linearProblem, flowAndSensi, flowAndSensi, new EmptyRemedialActionActivationResult(network)));
            }
        }

        LinearProblemStatus status = linearProblem.solve();
        TemporalData<RangeActionActivationResult> results = retrieveRangeActionActivationResults(linearProblem, prePerimeterSetpoints, optimizationPerimeters);
        return results;
    }

    private static IteratingLinearOptimizerParameters buildParameters(RaoParameters parameters) {
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
        return linearOptimizerParametersBuilder.build();
    }

    private static OptimizationPerimeter getPreventiveOptimizationPerimeter(Crac crac) {
        return new PreventiveOptimizationPerimeter(
            crac.getPreventiveState(),
            MarmotUtils.getPreventivePerimeterCnecs(crac),
            new HashSet<>(), // no loopflows for now
            new HashSet<>(), // don't re-optimize topological actions in Marmot
            crac.getRangeActions(crac.getPreventiveState())
        );
    }

    private static TemporalData<RangeActionActivationResult> retrieveRangeActionActivationResults(LinearProblem linearProblem, TemporalData<? extends RangeActionSetpointResult> prePerimeterSetPoints, TemporalData<OptimizationPerimeter> optimizationPerimeters) {
        Map<OffsetDateTime, RangeActionActivationResult> linearOptimizationResults = new HashMap<>();
        List<OffsetDateTime> timestamps = optimizationPerimeters.getTimestamps();
        timestamps.forEach(timestamp -> linearOptimizationResults.put(timestamp, new LinearProblemResult(linearProblem, prePerimeterSetPoints.getData(timestamp).orElseThrow(), optimizationPerimeters.getData(timestamp).orElseThrow())));
        return new TemporalDataImpl<>(linearOptimizationResults);
    }


}
