/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.InterTemporalRaoProvider;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPostOptimizationResults;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getTopologicalOptimizationResult;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runInitialPrePerimeterSensitivityAnalysis;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Marmot implements InterTemporalRaoProvider {

    private static final String INTER_TEMPORAL_RAO = "InterTemporalRao";
    private static final String VERSION = "1.0.0";

    @Override
    public CompletableFuture<TemporalData<RaoResult>> run(InterTemporalRaoInput raoInput, RaoParameters raoParameters) {
        // 1. Run independent RAOs to compute optimal preventive topological remedial actions
        TemporalData<RaoResult> topologicalOptimizationResults = runTopologicalOptimization(raoInput.getRaoInputs(), raoParameters);

        // if no inter-temporal constraints are defined, the results can be returned
        if (raoInput.getPowerGradientConstraints().isEmpty()) {
            return CompletableFuture.completedFuture(topologicalOptimizationResults);
        }

        // 2. Apply preventive topological remedial actions
        applyPreventiveTopologicalActionsOnNetwork(raoInput.getRaoInputs(), topologicalOptimizationResults);

        // 3. Run initial sensitivity analysis on all timestamps
        TemporalData<PrePerimeterResult> prePerimeterResults = runAllInitialPrePerimeterSensitivityAnalysis(raoInput.getRaoInputs(), raoParameters);

        // 4. Create and iteratively solve MIP to find optimal range actions' set-points
        TemporalData<LinearOptimizationResult> linearOptimizationResults = optimizeLinearRemedialActions(raoInput, prePerimeterResults, raoParameters);

        // 5. Merge topological and linear result
        TemporalData<RaoResult> mergedRaoResults = mergeTopologicalAndLinearOptimizationResults(raoInput.getRaoInputs(), prePerimeterResults, linearOptimizationResults, topologicalOptimizationResults);

        return CompletableFuture.completedFuture(mergedRaoResults);
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters) {
        return raoInputs.map(individualRaoInput -> Rao.run(individualRaoInput, raoParameters));
    }

    private static void applyPreventiveTopologicalActionsOnNetwork(TemporalData<RaoInput> raoInputs, TemporalData<RaoResult> topologicalOptimizationResults) {
        getTopologicalOptimizationResult(raoInputs, topologicalOptimizationResults)
            .getDataPerTimestamp()
            .values()
            .forEach(TopologicalOptimizationResult::applyTopologicalActions);
        // TODO: also handle curative remedial actions
    }

    private static TemporalData<PrePerimeterResult> runAllInitialPrePerimeterSensitivityAnalysis(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters) {
        return raoInputs.map(individualRaoInput -> runInitialPrePerimeterSensitivityAnalysis(individualRaoInput, raoParameters));
    }

    private static TemporalData<LinearOptimizationResult> optimizeLinearRemedialActions(InterTemporalRaoInput raoInput, TemporalData<PrePerimeterResult> prePerimeterResults, RaoParameters parameters) {
        // TODO: create MIP with all timestamps and power gradient constraints
        return new TemporalDataImpl<>();
    }

    private static TemporalData<RaoResult> mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> prePerimeterResults, TemporalData<LinearOptimizationResult> linearOptimizationResults, TemporalData<RaoResult> topologicalOptimizationResults) {
        // TODO: add curative RAs (range action and topological)
        return getPostOptimizationResults(raoInputs, prePerimeterResults, linearOptimizationResults, topologicalOptimizationResults).map(PostOptimizationResult::merge);
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
