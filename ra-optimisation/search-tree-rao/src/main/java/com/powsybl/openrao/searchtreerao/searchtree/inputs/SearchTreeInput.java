/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.inputs;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public final class SearchTreeInput {

    private final TemporalData<Network> networks;

    private final TemporalData<OptimizationPerimeter> optimizationPerimeters;

    private final TemporalData<FlowResult> initialFlowResults;
    private final TemporalData<PrePerimeterResult> prePerimeterResults;
    private final TemporalData<AppliedRemedialActions> preOptimizationAppliedRemedialActions;

    private final ObjectiveFunction objectiveFunction;
    private final TemporalData<ToolProvider> toolProviders;
    private final TemporalData<Instant> outageInstants;

    private final boolean isTimeCoupled;

    private SearchTreeInput(TemporalData<Network> networks,
                            TemporalData<OptimizationPerimeter> optimizationPerimeters,
                            TemporalData<FlowResult> initialFlowResults,
                            TemporalData<PrePerimeterResult> prePerimeterResults,
                            TemporalData<AppliedRemedialActions> preOptimizationAppliedRemedialActions,
                            ObjectiveFunction objectiveFunction,
                            TemporalData<ToolProvider> toolProviders,
                            TemporalData<Instant> outageInstants) {
        this.networks = networks;
        this.optimizationPerimeters = optimizationPerimeters;
        this.initialFlowResults = initialFlowResults;
        this.prePerimeterResults = prePerimeterResults;
        this.preOptimizationAppliedRemedialActions = preOptimizationAppliedRemedialActions;
        this.objectiveFunction = objectiveFunction;
        this.toolProviders = toolProviders;
        this.outageInstants = outageInstants;
        this.isTimeCoupled = networks.getTimestamps().size() > 1;
    }

    // single timestamp
    public Network getNetwork() {
        if (!isTimeCoupled) {
            return networks.getData(networks.getTimestamps().getFirst()).orElseThrow();
        }
        throw new OpenRaoException("getNetwork cannot be used on a time-coupled input, use getAllNetworks instead.");
    }

    public OptimizationPerimeter getOptimizationPerimeter() {
        if (!isTimeCoupled) {
            return optimizationPerimeters.getData(optimizationPerimeters.getTimestamps().getFirst()).orElseThrow();
        }
        throw new OpenRaoException("getOptimizationPerimeter cannot be used on a time-coupled input, use getAllOptimizationPerimeters instead.");
    }

    public PrePerimeterResult getPrePerimeterResult() {
        if (!isTimeCoupled) {
            return prePerimeterResults.getData(prePerimeterResults.getTimestamps().getFirst()).orElseThrow();
        }
        throw new OpenRaoException("getPrePerimeterResult cannot be used on a time-coupled input, use getAllPrePerimeterResults instead.");
    }

    public AppliedRemedialActions getPreOptimizationAppliedRemedialActions() {
        if (!isTimeCoupled) {
            return preOptimizationAppliedRemedialActions.getData(preOptimizationAppliedRemedialActions.getTimestamps().getFirst()).orElseThrow();
        }
        throw new OpenRaoException("getPreOptimizationAppliedRemedialActions cannot be used on a time-coupled input, use getAllPreOptimizationAppliedRemedialActions instead.");
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public ToolProvider getToolProvider() {
        if (!isTimeCoupled) {
            return toolProviders.getData(toolProviders.getTimestamps().getFirst()).orElseThrow();
        }
        throw new OpenRaoException("getToolProvider cannot be used on a time-coupled input, use getAllToolProviders instead.");
    }

    public FlowResult getInitialFlowResult() {
        if (!isTimeCoupled) {
            return initialFlowResults.getData(initialFlowResults.getTimestamps().getFirst()).orElseThrow();
        }
        throw new OpenRaoException("getInitialFlowResult cannot be used on a time-coupled input, use getAllInitialFlowResults instead.");
    }

    public Instant getOutageInstant() {
        if (!isTimeCoupled) {
            return outageInstants.getData(outageInstants.getTimestamps().getFirst()).orElseThrow();
        }
        throw new OpenRaoException("getOutageInstant cannot be used on a time-coupled input, use getAllOutageInstants instead.");
    }

    // time-coupled
    public TemporalData<Network> getAllNetworks() {
        return networks;
    }

    public TemporalData<OptimizationPerimeter> getAllOptimizationPerimeters() {
        return optimizationPerimeters;
    }

    public TemporalData<FlowResult> getAllInitialFlowResults() {
        return initialFlowResults;
    }

    public TemporalData<PrePerimeterResult> getAllPrePerimeterResults() {
        return prePerimeterResults;
    }

    public TemporalData<AppliedRemedialActions> getAllPreOptimizationAppliedRemedialActions() {
        return preOptimizationAppliedRemedialActions;
    }

    public TemporalData<ToolProvider> getAllToolProviders() {
        return toolProviders;
    }

    public TemporalData<Instant> getAllOutageInstants() {
        return outageInstants;
    }

    public static SearchTreeInputBuilder create() {
        return new SearchTreeInputBuilder();
    }

    public static class SearchTreeInputBuilder {

        private TemporalData<Network> networks;
        private TemporalData<OptimizationPerimeter> optimizationPerimeters;
        private TemporalData<FlowResult> initialFlowResults;
        private TemporalData<PrePerimeterResult> prePerimeterResults;
        private TemporalData<AppliedRemedialActions> preOptimizationAppliedNetworkActions;
        private ObjectiveFunction objectiveFunction;
        private TemporalData<ToolProvider> toolProviders;
        private TemporalData<Instant> outageInstants;

        public SearchTreeInputBuilder withNetwork(Network network) {
            this.networks = new TemporalDataImpl<>(Map.of(OffsetDateTime.MIN, network));
            return this;
        }

        public SearchTreeInputBuilder withAllNetworks(TemporalData<Network> networks) {
            this.networks = networks;
            return this;
        }

        public SearchTreeInputBuilder withOptimizationPerimeter(OptimizationPerimeter optimizationPerimeter) {
            this.optimizationPerimeters = new TemporalDataImpl<>(Map.of(OffsetDateTime.MIN, optimizationPerimeter));
            return this;
        }

        public SearchTreeInputBuilder withAllOptimizationPerimeters(TemporalData<OptimizationPerimeter> optimizationPerimeters) {
            this.optimizationPerimeters = optimizationPerimeters;
            return this;
        }

        public SearchTreeInputBuilder withInitialFlowResult(FlowResult initialFlowResult) {
            this.initialFlowResults = new TemporalDataImpl<>(Map.of(OffsetDateTime.MIN, initialFlowResult));
            return this;
        }

        public SearchTreeInputBuilder withAllInitialFlowResults(TemporalData<FlowResult> initialFlowResults) {
            this.initialFlowResults = initialFlowResults;
            return this;
        }

        public SearchTreeInputBuilder withPrePerimeterResult(PrePerimeterResult prePerimeterResult) {
            this.prePerimeterResults = new TemporalDataImpl<>(Map.of(OffsetDateTime.MIN, prePerimeterResult));
            return this;
        }

        public SearchTreeInputBuilder withAllPrePerimeterResults(TemporalData<PrePerimeterResult> prePerimeterResults) {
            this.prePerimeterResults = prePerimeterResults;
            return this;
        }

        public SearchTreeInputBuilder withPreOptimizationAppliedNetworkActions(AppliedRemedialActions preOptimizationAppliedNetworkActions) {
            this.preOptimizationAppliedNetworkActions = new TemporalDataImpl<>(Map.of(OffsetDateTime.MIN, preOptimizationAppliedNetworkActions));
            return this;
        }

        public SearchTreeInputBuilder withAllPreOptimizationAppliedNetworkActions(TemporalData<AppliedRemedialActions> preOptimizationAppliedNetworkActions) {
            this.preOptimizationAppliedNetworkActions = preOptimizationAppliedNetworkActions;
            return this;
        }

        public SearchTreeInputBuilder withObjectiveFunction(ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public SearchTreeInputBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProviders = new TemporalDataImpl<>(Map.of(OffsetDateTime.MIN, toolProvider));
            return this;
        }

        public SearchTreeInputBuilder withAllToolProviders(TemporalData<ToolProvider> toolProviders) {
            this.toolProviders = toolProviders;
            return this;
        }

        public SearchTreeInputBuilder withOutageInstant(Instant outageInstant) {
            this.outageInstants = new TemporalDataImpl<>(Map.of(OffsetDateTime.MIN, outageInstant));
            return this;
        }

        public SearchTreeInputBuilder withAllOutageInstants(TemporalData<Instant> outageInstants) {
            this.outageInstants = outageInstants;
            return this;
        }

        public SearchTreeInput build() {
            return new SearchTreeInput(networks,
                optimizationPerimeters,
                initialFlowResults,
                prePerimeterResults,
                preOptimizationAppliedNetworkActions,
                objectiveFunction,
                toolProviders,
                outageInstants);
        }
    }
}
