/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.inputs;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;
import com.powsybl.openrao.searchtreerao.result.impl.SearchTreeResult;
import com.powsybl.iidm.network.Network;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class IteratingLinearOptimizerInput {

    private final Network network;

    private final OptimizationPerimeter optimizationPerimeter;

    private final FlowResult initialFlowResult;

    private final PerimeterResultWithCnecs prePerimeterResult;

    private final SearchTreeResult preOptimizationResult;

    private final ObjectiveFunction objectiveFunction;

    private final ToolProvider toolProvider;
    private final Instant outageInstant;

    public IteratingLinearOptimizerInput(Network network,
                                         OptimizationPerimeter optimizationPerimeter,
                                         FlowResult initialFlowResult,
                                         PerimeterResultWithCnecs prePerimeterResult,
                                         SearchTreeResult preOptimizationResult,
                                         ObjectiveFunction objectiveFunction,
                                         ToolProvider toolProvider,
                                         Instant outageInstant) {
        this.network = network;
        this.optimizationPerimeter = optimizationPerimeter;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterResult = prePerimeterResult;
        this.preOptimizationResult = preOptimizationResult;
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

    public PerimeterResultWithCnecs getPrePerimeterResult() {
        return prePerimeterResult;
    }

    public SearchTreeResult getPreOptimizationResult() {
        return preOptimizationResult;
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

    public static IteratingLinearOptimizerInputBuilder create() {
        return new IteratingLinearOptimizerInputBuilder();
    }

    public static class IteratingLinearOptimizerInputBuilder {
        private Network network;
        private OptimizationPerimeter optimizationPerimeter;
        private FlowResult initialFlowResult;
        private PerimeterResultWithCnecs prePerimeterResult;
        private SearchTreeResult preOptimizationResult;
        private ObjectiveFunction objectiveFunction;
        private ToolProvider toolProvider;
        private Instant outageInstant;

        public IteratingLinearOptimizerInputBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withOptimizationPerimeter(OptimizationPerimeter optimizationPerimeter) {
            this.optimizationPerimeter = optimizationPerimeter;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withInitialFlowResult(FlowResult initialFlowResult) {
            this.initialFlowResult = initialFlowResult;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPrePerimeterResult(PerimeterResultWithCnecs prePerimeterResult) {
            this.prePerimeterResult = prePerimeterResult;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPreOptimizationResult(SearchTreeResult preOptimizationResult) {
            this.preOptimizationResult = preOptimizationResult;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withObjectiveFunction(ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withOutageInstant(Instant outageInstant) {
            this.outageInstant = outageInstant;
            return this;
        }

        public IteratingLinearOptimizerInput build() {
            return new IteratingLinearOptimizerInput(network,
                optimizationPerimeter,
                initialFlowResult,
                prePerimeterResult,
                preOptimizationResult,
                objectiveFunction,
                toolProvider,
                outageInstant);
        }
    }
}

