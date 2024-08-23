/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.inputs;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.List;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class IteratingLinearOptimizerMultiTSInput {

    private final List<Network> networks;

    private final List<OptimizationPerimeter> optimizationPerimeters;

    private final FlowResult initialFlowResult;

    private final FlowResult prePerimeterFlowResult;
    private final RangeActionSetpointResult prePerimeterSetpoints;

    private final FlowResult preOptimizationFlowResult;
    private final SensitivityResult preOptimizationSensitivityResult;

    private final AppliedRemedialActions preOptimizationAppliedRemedialActions;
    private final RangeActionActivationResult raActivationFromParentLeaf;

    private final ObjectiveFunction objectiveFunction;

    private final ToolProvider toolProvider;
    private final Instant outageInstant;

    public IteratingLinearOptimizerMultiTSInput(List<Network> networks,
                                                List<OptimizationPerimeter> optimizationPerimeters,
                                                FlowResult initialFlowResult,
                                                FlowResult prePerimeterFlowResult,
                                                RangeActionSetpointResult prePerimeterSetpoints,
                                                FlowResult preOptimizationFlowResult,
                                                SensitivityResult preOptimizationSensitivityResult,
                                                AppliedRemedialActions preOptimizationAppliedRemedialActions,
                                                RangeActionActivationResult raActivationFromParentLeaf,
                                                ObjectiveFunction objectiveFunction,
                                                ToolProvider toolProvider,
                                                Instant outageInstant) {
        this.networks = networks;
        this.optimizationPerimeters = optimizationPerimeters;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.prePerimeterSetpoints = prePerimeterSetpoints;
        this.preOptimizationFlowResult = preOptimizationFlowResult;
        this.preOptimizationSensitivityResult = preOptimizationSensitivityResult;
        this.preOptimizationAppliedRemedialActions = preOptimizationAppliedRemedialActions;
        this.raActivationFromParentLeaf = raActivationFromParentLeaf;
        this.objectiveFunction = objectiveFunction;
        this.toolProvider = toolProvider;
        this.outageInstant = outageInstant;
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public Network getNetwork(int i) {
        return networks.get(i);
    }

    public List<OptimizationPerimeter> getOptimizationPerimeters() {
        return optimizationPerimeters;
    }

    public OptimizationPerimeter getOptimizationPerimeter(int i) {
        return optimizationPerimeters.get(i);
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

    public Instant getOutageInstant() {
        return outageInstant;
    }

    public static IteratingLinearOptimizerMultiTSInputBuilder create() {
        return new IteratingLinearOptimizerMultiTSInputBuilder();
    }

    public static class IteratingLinearOptimizerMultiTSInputBuilder {
        private List<Network> networks;
        private List<OptimizationPerimeter> optimizationPerimeters;
        private FlowResult initialFlowResult;
        private FlowResult prePerimeterFlowResult;
        private RangeActionSetpointResult prePerimeterSetpoints;
        private FlowResult preOptimizationFlowResult;
        private SensitivityResult preOptimizationSensitivityResult;
        private AppliedRemedialActions preOptimizationAppliedRemedialActions;
        private RangeActionActivationResult raActivationFromParentLeaf;
        private ObjectiveFunction objectiveFunction;
        private ToolProvider toolProvider;
        private Instant outageInstant;

        public IteratingLinearOptimizerMultiTSInputBuilder withNetworks(List<Network> networks) {
            this.networks = networks;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withOptimizationPerimeters(List<OptimizationPerimeter> optimizationPerimeters) {
            this.optimizationPerimeters = optimizationPerimeters;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withInitialFlowResult(FlowResult initialFlowResult) {
            this.initialFlowResult = initialFlowResult;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withPrePerimeterFlowResult(FlowResult prePerimeterFlowResult) {
            this.prePerimeterFlowResult = prePerimeterFlowResult;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withPrePerimeterSetpoints(RangeActionSetpointResult prePerimeterSetpoints) {
            this.prePerimeterSetpoints = prePerimeterSetpoints;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withPreOptimizationFlowResult(FlowResult preOptimizationFlowResult) {
            this.preOptimizationFlowResult = preOptimizationFlowResult;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withPreOptimizationSensitivityResult(SensitivityResult preOptimizationSensitivityResult) {
            this.preOptimizationSensitivityResult = preOptimizationSensitivityResult;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withPreOptimizationAppliedRemedialActions(AppliedRemedialActions preOptimizationAppliedRemedialActions) {
            this.preOptimizationAppliedRemedialActions = preOptimizationAppliedRemedialActions;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withRaActivationFromParentLeaf(RangeActionActivationResult raActivationFromParentLeaf) {
            this.raActivationFromParentLeaf = raActivationFromParentLeaf;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withObjectiveFunction(ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInputBuilder withOutageInstant(Instant outageInstant) {
            this.outageInstant = outageInstant;
            return this;
        }

        public IteratingLinearOptimizerMultiTSInput build() {
            return new IteratingLinearOptimizerMultiTSInput(networks,
                optimizationPerimeters,
                initialFlowResult,
                prePerimeterFlowResult,
                prePerimeterSetpoints,
                preOptimizationFlowResult,
                preOptimizationSensitivityResult,
                preOptimizationAppliedRemedialActions,
                raActivationFromParentLeaf,
                objectiveFunction,
                toolProvider,
                outageInstant);
        }
    }
}

