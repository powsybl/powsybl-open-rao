/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.search_tree.inputs;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.OptimizationContext;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;

import java.util.Set;

public class SearchTreeInput {

    private final Network network;
    private final Set<FlowCnec> flowCnecs;
    private final Set<FlowCnec> loopFlowCnecs;
    private final OptimizationContext optimizationContext;
    private final Set<NetworkAction> availableNetworkActions;

    private final FlowResult initialFlowResult;
    private final PrePerimeterResult prePerimeterResult;
    private final AppliedRemedialActions preOptimizationAppliedNetworkActions;

    private final ObjectiveFunction objectiveFunction;
    private final ToolProvider toolProvider;

    private SearchTreeInput(Network network,
                            Set<FlowCnec> flowCnecs,
                            Set<FlowCnec> loopFlowCnecs,
                            OptimizationContext optimizationContext,
                            Set<NetworkAction> availableNetworkActions,
                            FlowResult initialFlowResult,
                            PrePerimeterResult prePerimeterResult,
                            AppliedRemedialActions preOptimizationAppliedNetworkActions,
                            ObjectiveFunction objectiveFunction,
                            ToolProvider toolProvider) {
        this.network = network;
        this.flowCnecs = flowCnecs;
        this.loopFlowCnecs = loopFlowCnecs;
        this.optimizationContext = optimizationContext;
        this.availableNetworkActions = availableNetworkActions;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterResult = prePerimeterResult;
        this.preOptimizationAppliedNetworkActions = preOptimizationAppliedNetworkActions;
        this.objectiveFunction = objectiveFunction;
        this.toolProvider = toolProvider;
    }

    public Network getNetwork() {
        return network;
    }

    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }

    public Set<FlowCnec> getLoopFlowCnecs() {
        return loopFlowCnecs;
    }

    public OptimizationContext getOptimizationContext() {
        return optimizationContext;
    }

    public Set<NetworkAction> getAvailableNetworkActions() {
        return availableNetworkActions;
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

    public SearchTreeInputBuilder create() {
        return new SearchTreeInputBuilder();
    }

    public static class SearchTreeInputBuilder {

        private Network network;
        private Set<FlowCnec> flowCnecs;
        private Set<FlowCnec> loopFlowCnecs;
        private OptimizationContext optimizationContext;
        private Set<NetworkAction> availableNetworkActions;
        private FlowResult initialFlowResult;
        private PrePerimeterResult prePerimeterResult;
        private AppliedRemedialActions preOptimizationAppliedNetworkActions;
        private ObjectiveFunction objectiveFunction;
        private ToolProvider toolProvider;

        SearchTreeInputBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        SearchTreeInputBuilder withFlowCnecs(Set<FlowCnec> flowCnecs) {
            this.flowCnecs = flowCnecs;
            return this;
        }

        SearchTreeInputBuilder withLoopFlowCnecs(Set<FlowCnec> loopFlowCnecs) {
            this.loopFlowCnecs = loopFlowCnecs;
            return this;
        }

        SearchTreeInputBuilder withOptimizationContext(OptimizationContext optimizationContext) {
            this.optimizationContext = optimizationContext;
            return this;
        }

        SearchTreeInputBuilder withAvailableNetworkActions(Set<NetworkAction> availableNetworkActions) {
            this.availableNetworkActions = availableNetworkActions;
            return this;
        }

        SearchTreeInputBuilder withInitialFlowResult(FlowResult initialFlowResult) {
            this.initialFlowResult = initialFlowResult;
            return this;
        }

        SearchTreeInputBuilder withPrePerimeterResult(PrePerimeterResult prePerimeterResult) {
            this.prePerimeterResult = prePerimeterResult;
            return this;
        }

        SearchTreeInputBuilder withPreOptimizationAppliedNetworkActions(AppliedRemedialActions preOptimizationAppliedNetworkActions) {
            this.preOptimizationAppliedNetworkActions = preOptimizationAppliedNetworkActions;
            return this;
        }

        SearchTreeInputBuilder withObjectiveFunction(ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        SearchTreeInputBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        SearchTreeInput build() {
            return new SearchTreeInput(network,
                flowCnecs,
                loopFlowCnecs,
                optimizationContext,
                availableNetworkActions,
                initialFlowResult,
                prePerimeterResult,
                preOptimizationAppliedNetworkActions,
                objectiveFunction,
                toolProvider);
        }
    }
}
