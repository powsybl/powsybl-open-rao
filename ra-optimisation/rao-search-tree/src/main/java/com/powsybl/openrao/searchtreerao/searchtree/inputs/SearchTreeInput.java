/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.inputs;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public final class SearchTreeInput {

    private final Network network;

    private final OptimizationPerimeter optimizationPerimeter;

    private final FlowResult initialFlowResult;
    private final PrePerimeterResult prePerimeterResult;
    private final AppliedRemedialActions preOptimizationAppliedRemedialActions;

    private final ObjectiveFunction objectiveFunction;
    private final ToolProvider toolProvider;
    private final Instant outageInstant;

    private SearchTreeInput(Network network,
                            OptimizationPerimeter optimizationPerimeter,
                            FlowResult initialFlowResult,
                            PrePerimeterResult prePerimeterResult,
                            AppliedRemedialActions preOptimizationAppliedRemedialActions,
                            ObjectiveFunction objectiveFunction,
                            ToolProvider toolProvider,
                            Instant outageInstant) {
        this.network = network;
        this.optimizationPerimeter = optimizationPerimeter;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterResult = prePerimeterResult;
        this.preOptimizationAppliedRemedialActions = preOptimizationAppliedRemedialActions;
        this.objectiveFunction = objectiveFunction;
        this.toolProvider = toolProvider;
        this.outageInstant = outageInstant;
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

    public AppliedRemedialActions getPreOptimizationAppliedRemedialActions() {
        return preOptimizationAppliedRemedialActions;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public ToolProvider getToolProvider() {
        return toolProvider;
    }

    public Instant getOutageInstant() {
        return outageInstant;
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
        private Instant outageInstant;

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

        public SearchTreeInputBuilder withOutageInstant(Instant outageInstant) {
            this.outageInstant = outageInstant;
            return this;
        }

        public SearchTreeInput build() {
            return new SearchTreeInput(network,
                optimizationPerimeter,
                initialFlowResult,
                prePerimeterResult,
                preOptimizationAppliedNetworkActions,
                objectiveFunction,
                toolProvider,
                outageInstant);
        }
    }
}
