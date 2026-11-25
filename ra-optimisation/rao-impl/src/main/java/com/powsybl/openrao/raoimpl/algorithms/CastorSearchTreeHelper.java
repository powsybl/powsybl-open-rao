/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPreSolveParameters;
import com.powsybl.openrao.raoimpl.algorithms.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.raoimpl.algorithms.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.raoimpl.algorithms.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.raoimpl.commons.NetworkActionCombination;
import com.powsybl.openrao.raoimpl.commons.ToolProvider;
import com.powsybl.openrao.raoimpl.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.parameters.TreeParameters;
import com.powsybl.openrao.raoimpl.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.OptimizationResult;
import com.powsybl.openrao.raoimpl.result.api.PrePerimeterResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.Set;

import static com.powsybl.openrao.raoimpl.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;

/**
 * @author Maxence Fleury {@literal <maxence.fleury at artelys.com>}
 */
final class CastorSearchTreeHelper {

    private CastorSearchTreeHelper() {
        // Utility class should not be instantiated
    }

    static SearchTreeParameters buildSearchTreeParameters(final RaoParameters raoParameters,
                                                           final TreeParameters treeParameters,
                                                           final Crac crac,
                                                           final Network network,
                                                           final Set<String> operatorsNotSharingCras,
                                                           final ReportNode reportNode) {
        SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(treeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), operatorsNotSharingCras));

        if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters =
                raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                    ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                    : new LoadFlowAndSensitivityParameters(reportNode);
            searchTreeParametersBuilder.withLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);
        }

        return searchTreeParametersBuilder.build();
    }

    static OptimizationResult runSearchTree(final Network network,
                                            final OptimizationPerimeter optPerimeter,
                                            final ObjectiveFunction objectiveFunction,
                                            final FlowResult initialFlowResult,
                                            final PrePerimeterResult prePerimeterResult,
                                            final AppliedRemedialActions appliedRemedialActions,
                                            final ToolProvider toolProvider,
                                            final Instant outageInstant,
                                            final SearchTreeParameters searchTreeParameters,
                                            final boolean verbose,
                                            final ReportNode reportNode) {
        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialFlowResult)
            .withPrePerimeterResult(prePerimeterResult)
            .withPreOptimizationAppliedNetworkActions(appliedRemedialActions)
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(outageInstant)
            .build();

        return new SearchTree(searchTreeInput, searchTreeParameters, verbose, reportNode).run().join();
    }

    /**
     * HINT: the pre-solve combination competes at depth 1 alongside all single network actions.
     * FORCED: depth 1 of the main search is replaced by this combination alone.
     */
    static void injectPreSolveCombination(final SearchTreeParameters mainSearchTreeParameters,
                                          final Set<NetworkAction> preSolveCombination,
                                          final SearchTreeRaoPreSolveParameters.SolutionUsage preSolveUsage) {
        if (preSolveCombination.isEmpty()) {
            return;
        }
        NetworkActionCombination combination = new NetworkActionCombination(preSolveCombination, true);
        if (preSolveUsage == SearchTreeRaoPreSolveParameters.SolutionUsage.FORCED) {
            mainSearchTreeParameters.getNetworkActionParameters().addForcedNetworkActionCombination(combination);
        } else {
            mainSearchTreeParameters.getNetworkActionParameters().addHintedNetworkActionCombination(combination);
        }
    }
}
