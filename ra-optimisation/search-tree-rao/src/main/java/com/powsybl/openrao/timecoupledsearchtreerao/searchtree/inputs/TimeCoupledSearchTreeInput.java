/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.timecoupledsearchtreerao.searchtree.inputs;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.ToolProvider;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.FlowResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.PrePerimeterResult;

/**
 * All the inputs are TemporalData and the objective function is global.
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public final class TimeCoupledSearchTreeInput {
    private final TemporalData<Network> networks;
    private final TemporalData<OptimizationPerimeter> optimizationPerimeters;
    private final TemporalData<FlowResult> initialFlowResults;
    private final TemporalData<PrePerimeterResult> prePerimeterResults;
    private final TemporalData<AppliedRemedialActions> preOptimizationAppliedRemedialActions;
    private final TemporalData<ToolProvider> toolProviders;
    private final TemporalData<Instant> outageInstants;
    private final ObjectiveFunction globalObjectiveFunction;

    private TimeCoupledSearchTreeInput(TemporalData<Network> networks,
                                       TemporalData<OptimizationPerimeter> optimizationPerimeters,
                                       TemporalData<FlowResult> initialFlowResults,
                                       TemporalData<PrePerimeterResult> prePerimeterResults,
                                       TemporalData<AppliedRemedialActions> preOptimizationAppliedRemedialActions,
                                       ObjectiveFunction globalObjectiveFunction,
                                       TemporalData<ToolProvider> toolProviders,
                                       TemporalData<Instant> outageInstants) {
        this.networks = networks;
        this.optimizationPerimeters = optimizationPerimeters;
        this.initialFlowResults = initialFlowResults;
        this.prePerimeterResults = prePerimeterResults;
        this.preOptimizationAppliedRemedialActions = preOptimizationAppliedRemedialActions;
        this.globalObjectiveFunction = globalObjectiveFunction;
        this.toolProviders = toolProviders;
        this.outageInstants = outageInstants;
    }

    public TemporalData<Network> getNetworks() {
        return networks;
    }

    public TemporalData<OptimizationPerimeter> getOptimizationPerimeters() {
        return optimizationPerimeters;
    }

    public TemporalData<FlowResult> getInitialFlowResults() {
        return initialFlowResults;
    }

    public TemporalData<PrePerimeterResult> getPrePerimeterResults() {
        return prePerimeterResults;
    }

    public TemporalData<AppliedRemedialActions> getPreOptimizationAppliedRemedialActions() {
        return preOptimizationAppliedRemedialActions;
    }

    public ObjectiveFunction getGlobalObjectiveFunction() {
        return globalObjectiveFunction;
    }

    public TemporalData<ToolProvider> getToolProviders() {
        return toolProviders;
    }

    public TemporalData<Instant> getOutageInstants() {
        return outageInstants;
    }

    public static TimeCoupledSearchTreeInputBuilder create() {
        return new TimeCoupledSearchTreeInputBuilder();
    }

    public static class TimeCoupledSearchTreeInputBuilder {

        private TemporalData<Network> networks;
        private TemporalData<OptimizationPerimeter> optimizationPerimeters;
        private TemporalData<FlowResult> initialFlowResults;
        private TemporalData<PrePerimeterResult> prePerimeterResults;
        private TemporalData<AppliedRemedialActions> preOptimizationAppliedNetworkActions;
        private ObjectiveFunction globalObjectiveFunction;
        private TemporalData<ToolProvider> toolProviders;
        private TemporalData<Instant> outageInstants;

        public TimeCoupledSearchTreeInputBuilder withNetworks(TemporalData<Network> networks) {
            this.networks = networks;
            return this;
        }

        public TimeCoupledSearchTreeInputBuilder withOptimizationPerimeters(TemporalData<OptimizationPerimeter> optimizationPerimeters) {
            this.optimizationPerimeters = optimizationPerimeters;
            return this;
        }

        public TimeCoupledSearchTreeInputBuilder withInitialFlowResults(TemporalData<FlowResult> initialFlowResults) {
            this.initialFlowResults = initialFlowResults;
            return this;
        }

        public TimeCoupledSearchTreeInputBuilder withPrePerimeterResults(TemporalData<PrePerimeterResult> prePerimeterResults) {
            this.prePerimeterResults = prePerimeterResults;
            return this;
        }

        public TimeCoupledSearchTreeInputBuilder withPreOptimizationAppliedNetworkActions(TemporalData<AppliedRemedialActions> preOptimizationAppliedNetworkActions) {
            this.preOptimizationAppliedNetworkActions = preOptimizationAppliedNetworkActions;
            return this;
        }

        public TimeCoupledSearchTreeInputBuilder withObjectiveFunction(ObjectiveFunction objectiveFunction) {
            this.globalObjectiveFunction = objectiveFunction;
            return this;
        }

        public TimeCoupledSearchTreeInputBuilder withToolProviders(TemporalData<ToolProvider> toolProviders) {
            this.toolProviders = toolProviders;
            return this;
        }

        public TimeCoupledSearchTreeInputBuilder withOutageInstants(TemporalData<Instant> outageInstants) {
            this.outageInstants = outageInstants;
            return this;
        }

        public TimeCoupledSearchTreeInput build() {
            return new TimeCoupledSearchTreeInput(
                networks,
                optimizationPerimeters,
                initialFlowResults,
                prePerimeterResults,
                preOptimizationAppliedNetworkActions,
                globalObjectiveFunction,
                toolProviders,
                outageInstants);
        }
    }
}
