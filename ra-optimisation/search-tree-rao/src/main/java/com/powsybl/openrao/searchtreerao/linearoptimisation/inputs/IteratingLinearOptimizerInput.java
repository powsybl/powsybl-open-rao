/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.inputs;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public record IteratingLinearOptimizerInput(Network network, OptimizationPerimeter optimizationPerimeter,
                                            FlowResult initialFlowResult, FlowResult prePerimeterFlowResult,
                                            RangeActionSetpointResult prePerimeterSetpoints,
                                            FlowResult preOptimizationFlowResult,
                                            SensitivityResult preOptimizationSensitivityResult,
                                            AppliedRemedialActions preOptimizationAppliedRemedialActions,
                                            RangeActionActivationResult raActivationFromParentLeaf,
                                            NetworkActionsResult appliedNetworkActionsInPrimaryState,
                                            ObjectiveFunction objectiveFunction, ToolProvider toolProvider,
                                            Instant outageInstant) {

    public static IteratingLinearOptimizerInputBuilder create() {
        return new IteratingLinearOptimizerInputBuilder();
    }

    public static class IteratingLinearOptimizerInputBuilder {
        private Network network;
        private OptimizationPerimeter optimizationPerimeter;
        private FlowResult initialFlowResult;
        private FlowResult prePerimeterFlowResult;
        private RangeActionSetpointResult prePerimeterSetpoints;
        private FlowResult preOptimizationFlowResult;
        private SensitivityResult preOptimizationSensitivityResult;
        private AppliedRemedialActions preOptimizationAppliedRemedialActions;
        private RangeActionActivationResult raActivationFromParentLeaf;
        private NetworkActionsResult appliedNetworkActionsInPrimaryState;
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

        public IteratingLinearOptimizerInputBuilder withPrePerimeterFlowResult(FlowResult prePerimeterFlowResult) {
            this.prePerimeterFlowResult = prePerimeterFlowResult;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPrePerimeterSetpoints(RangeActionSetpointResult prePerimeterSetpoints) {
            this.prePerimeterSetpoints = prePerimeterSetpoints;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPreOptimizationFlowResult(FlowResult preOptimizationFlowResult) {
            this.preOptimizationFlowResult = preOptimizationFlowResult;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPreOptimizationSensitivityResult(SensitivityResult preOptimizationSensitivityResult) {
            this.preOptimizationSensitivityResult = preOptimizationSensitivityResult;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPreOptimizationAppliedRemedialActions(AppliedRemedialActions preOptimizationAppliedRemedialActions) {
            this.preOptimizationAppliedRemedialActions = preOptimizationAppliedRemedialActions;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withAppliedNetworkActionsInPrimaryState(NetworkActionsResult appliedNetworkActionsInPrimaryState) {
            this.appliedNetworkActionsInPrimaryState = appliedNetworkActionsInPrimaryState;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withRaActivationFromParentLeaf(RangeActionActivationResult raActivationFromParentLeaf) {
            this.raActivationFromParentLeaf = raActivationFromParentLeaf;
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
                prePerimeterFlowResult,
                prePerimeterSetpoints,
                preOptimizationFlowResult,
                preOptimizationSensitivityResult,
                preOptimizationAppliedRemedialActions,
                raActivationFromParentLeaf,
                appliedNetworkActionsInPrimaryState,
                objectiveFunction,
                toolProvider,
                outageInstant);
        }
    }
}

