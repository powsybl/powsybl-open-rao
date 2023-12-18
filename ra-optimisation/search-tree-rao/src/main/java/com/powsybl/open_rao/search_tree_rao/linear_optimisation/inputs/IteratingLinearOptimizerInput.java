/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.linear_optimisation.inputs;

import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.search_tree_rao.commons.ToolProvider;
import com.powsybl.open_rao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.powsybl.open_rao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.powsybl.open_rao.search_tree_rao.result.api.FlowResult;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionActivationResult;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.open_rao.search_tree_rao.result.api.SensitivityResult;
import com.powsybl.open_rao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class IteratingLinearOptimizerInput {

    private final Network network;

    private final OptimizationPerimeter optimizationPerimeter;

    private final FlowResult initialFlowResult;

    private final FlowResult prePerimeterFlowResult;
    private final RangeActionSetpointResult prePerimeterSetpoints;

    private final FlowResult preOptimizationFlowResult;
    private final SensitivityResult preOptimizationSensitivityResult;

    private final AppliedRemedialActions preOptimizationAppliedRemedialActions;
    private final RangeActionActivationResult raActivationFromParentLeaf;

    private final ObjectiveFunction objectiveFunction;

    private final ToolProvider toolProvider;
    private final Crac crac;

    public IteratingLinearOptimizerInput(Network network,
                                         OptimizationPerimeter optimizationPerimeter,
                                         FlowResult initialFlowResult,
                                         FlowResult prePerimeterFlowResult,
                                         RangeActionSetpointResult prePerimeterSetpoints,
                                         FlowResult preOptimizationFlowResult,
                                         SensitivityResult preOptimizationSensitivityResult,
                                         AppliedRemedialActions preOptimizationAppliedRemedialActions,
                                         RangeActionActivationResult raActivationFromParentLeaf,
                                         ObjectiveFunction objectiveFunction,
                                         ToolProvider toolProvider,
                                         Crac crac) {
        this.network = network;
        this.optimizationPerimeter = optimizationPerimeter;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.prePerimeterSetpoints = prePerimeterSetpoints;
        this.preOptimizationFlowResult = preOptimizationFlowResult;
        this.preOptimizationSensitivityResult = preOptimizationSensitivityResult;
        this.preOptimizationAppliedRemedialActions = preOptimizationAppliedRemedialActions;
        this.raActivationFromParentLeaf = raActivationFromParentLeaf;
        this.objectiveFunction = objectiveFunction;
        this.toolProvider = toolProvider;
        this.crac = crac;
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

    public FlowResult getPrePerimeterFlowResult() {
        return prePerimeterFlowResult;
    }

    public RangeActionSetpointResult getPrePerimeterSetpoints() {
        return prePerimeterSetpoints;
    }

    public FlowResult getPreOptimizationFlowResult() {
        return preOptimizationFlowResult;
    }

    public SensitivityResult getPreOptimizationSensitivityResult() {
        return preOptimizationSensitivityResult;
    }

    public AppliedRemedialActions getPreOptimizationAppliedRemedialActions() {
        return preOptimizationAppliedRemedialActions;
    }

    public RangeActionActivationResult getRaActivationFromParentLeaf() {
        return raActivationFromParentLeaf;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public ToolProvider getToolProvider() {
        return toolProvider;
    }

    public Crac getCrac() {
        return crac;
    }

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
        private ObjectiveFunction objectiveFunction;
        private ToolProvider toolProvider;
        private Crac crac;

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

        public IteratingLinearOptimizerInputBuilder withCrac(Crac crac) {
            this.crac = crac;
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
                objectiveFunction,
                toolProvider,
                crac);
        }
    }
}

