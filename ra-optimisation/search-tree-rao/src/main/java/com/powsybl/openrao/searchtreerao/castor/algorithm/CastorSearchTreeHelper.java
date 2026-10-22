/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.Set;

import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;

/**
 * Shared search-tree assembly used by {@link CastorContingencyScenarios}, {@link CastorSecondPreventive} and
 * {@link CastorFullOptimization}: {@link #searchNetworkActionCombination} explores the tree of network-action
 * combinations (optimizing range actions at each leaf) and returns the best one found, while
 * {@link #reoptimizeNetworkActionCombination} takes an already-decided combination and re-optimizes only its
 * range actions against different inputs, without exploring further combinations. When optimize-in-dc is
 * active, callers search on DC sensitivities for performance, then re-optimize the decided combination against
 * AC-flavored {@link OptimizationPerimeter} and {@link ObjectiveFunction} before reporting it — but whether a
 * given call runs in DC or AC depends entirely on the inputs the caller passes in, not on the method itself.
 */
final class CastorSearchTreeHelper {
    private static final double COST_EPSILON = 1e-6;

    private CastorSearchTreeHelper() {
    }

    /**
     * Shared by {@link CastorContingencyScenarios} and {@link CastorFullOptimization}: whether a result already
     * satisfies the search's stop criterion (e.g. no need to look for a better network-action combination).
     */
    static boolean isStopCriterionChecked(ObjectiveFunctionResult result, TreeParameters treeParameters) {
        if (result.getVirtualCost() > COST_EPSILON) {
            return false;
        }
        if (result.getFunctionalCost() < -Double.MAX_VALUE / 2 && result.getVirtualCost() < COST_EPSILON) {
            return true;
        }

        if (treeParameters.stopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
            return false;
        } else if (treeParameters.stopCriterion().equals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE)) {
            return result.getCost() < treeParameters.targetObjectiveValue() + COST_EPSILON;
        } else {
            throw new OpenRaoException("Unexpected stop criterion: " + treeParameters.stopCriterion());
        }
    }

    static SearchTreeParameters buildSearchTreeParameters(RaoParameters raoParameters,
                                                           TreeParameters treeParameters,
                                                           Crac crac,
                                                           Network network,
                                                           Set<String> operatorsNotSharingCras,
                                                           ReportNode reportNode) {
        SearchTreeParameters.SearchTreeParametersBuilder builder = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(treeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), operatorsNotSharingCras));

        if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                : new LoadFlowAndSensitivityParameters(reportNode);
            builder.withLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);
        }

        return builder.build();
    }

    static OptimizationResult searchNetworkActionCombination(Network network,
                                                              OptimizationPerimeter optPerimeter,
                                                              ObjectiveFunction objectiveFunction,
                                                              PrePerimeterResult initialResult,
                                                              PrePerimeterResult prePerimeterResult,
                                                              AppliedRemedialActions appliedRemedialActions,
                                                              ToolProvider toolProvider,
                                                              Instant outageInstant,
                                                              SearchTreeParameters searchTreeParameters,
                                                              boolean verbose,
                                                              ReportNode reportNode) {
        SearchTreeInput searchTreeInput = buildSearchTreeInput(network, optPerimeter, objectiveFunction, initialResult,
            prePerimeterResult, appliedRemedialActions, toolProvider, outageInstant);
        return new SearchTree(searchTreeInput, searchTreeParameters, verbose, reportNode).run().join();
    }

    static OptimizationResult reoptimizeNetworkActionCombination(Network network,
                                                                  OptimizationPerimeter optPerimeter,
                                                                  ObjectiveFunction objectiveFunction,
                                                                  PrePerimeterResult initialResult,
                                                                  PrePerimeterResult prePerimeterResult,
                                                                  AppliedRemedialActions appliedRemedialActions,
                                                                  ToolProvider toolProvider,
                                                                  Instant outageInstant,
                                                                  SearchTreeParameters searchTreeParameters,
                                                                  OptimizationResult decidedResult,
                                                                  boolean verbose,
                                                                  ReportNode reportNode) {
        SearchTreeInput searchTreeInput = buildSearchTreeInput(network, optPerimeter, objectiveFunction, initialResult,
            prePerimeterResult, appliedRemedialActions, toolProvider, outageInstant);
        return new SearchTree(searchTreeInput, searchTreeParameters, verbose, reportNode)
            .optimizeDecidedNetworkActionCombination(decidedResult, reportNode);
    }

    private static SearchTreeInput buildSearchTreeInput(Network network,
                                                         OptimizationPerimeter optPerimeter,
                                                         ObjectiveFunction objectiveFunction,
                                                         PrePerimeterResult initialResult,
                                                         PrePerimeterResult prePerimeterResult,
                                                         AppliedRemedialActions appliedRemedialActions,
                                                         ToolProvider toolProvider,
                                                         Instant outageInstant) {
        return SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(prePerimeterResult)
            .withPreOptimizationAppliedNetworkActions(appliedRemedialActions)
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(outageInstant)
            .build();
    }
}
