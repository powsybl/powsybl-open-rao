/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.search_tree.inputs;

import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;

public class SearchTreeInput {

    private final Network network;

    private final OptimizationPerimeter optimizationPerimeter;

    private final FlowResult initialFlowResult;
    private final PrePerimeterResult prePerimeterResult;
    private final AppliedRemedialActions preOptimizationAppliedNetworkActions;

    private final ObjectiveFunction objectiveFunction;
    private final ToolProvider toolProvider;

    private SearchTreeInput(Network network,
                            OptimizationPerimeter optimizationPerimeter,
                            FlowResult initialFlowResult,
                            PrePerimeterResult prePerimeterResult,
                            AppliedRemedialActions preOptimizationAppliedNetworkActions,
                            ObjectiveFunction objectiveFunction,
                            ToolProvider toolProvider) {
        this.network = network;
        this.optimizationPerimeter = optimizationPerimeter;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterResult = prePerimeterResult;
        this.preOptimizationAppliedNetworkActions = preOptimizationAppliedNetworkActions;
        this.objectiveFunction = objectiveFunction;
        this.toolProvider = toolProvider;
    }

    public Network getNetwork() {
        return network;
    }

    public OptimizationPerimeter getOptimizationPerimeter() {
        return optimizationPerimeter;
    }

    public FlowResult getInitialFlowResult() {
        return initialFlowResult;
    }

    public PrePerimeterResult getPrePerimeterResult() {
        return prePerimeterResult;
    }

    public AppliedRemedialActions getPreOptimizationAppliedNetworkActions() {
        return preOptimizationAppliedNetworkActions;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public ToolProvider getToolProvider() {
        return toolProvider;
    }

    public static SearchTreeInputBuilder create() {
        return new SearchTreeInputBuilder();
    }

    public static class SearchTreeInputBuilder {

        private Network network;
        private OptimizationPerimeter optimizationPerimeter;
        private FlowResult initialFlowResult;
        private PrePerimeterResult prePerimeterResult;
        private AppliedRemedialActions preOptimizationAppliedNetworkActions;
        private ObjectiveFunction objectiveFunction;
        private ToolProvider toolProvider;

        public SearchTreeInputBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public SearchTreeInputBuilder withOptimizationPerimeter(OptimizationPerimeter optimizationPerimeter) {
            this.optimizationPerimeter = optimizationPerimeter;
            return this;
        }

        public SearchTreeInputBuilder withInitialFlowResult(FlowResult initialFlowResult) {
            this.initialFlowResult = initialFlowResult;
            return this;
        }

        public SearchTreeInputBuilder withPrePerimeterResult(PrePerimeterResult prePerimeterResult) {
            this.prePerimeterResult = prePerimeterResult;
            return this;
        }

        public SearchTreeInputBuilder withPreOptimizationAppliedNetworkActions(AppliedRemedialActions preOptimizationAppliedNetworkActions) {
            this.preOptimizationAppliedNetworkActions = preOptimizationAppliedNetworkActions;
            return this;
        }

        public SearchTreeInputBuilder withObjectiveFunction(ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public SearchTreeInputBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public SearchTreeInput build() {
            return new SearchTreeInput(network,
                optimizationPerimeter,
                initialFlowResult,
                prePerimeterResult,
                preOptimizationAppliedNetworkActions,
                objectiveFunction,
                toolProvider);
        }
    }
}
