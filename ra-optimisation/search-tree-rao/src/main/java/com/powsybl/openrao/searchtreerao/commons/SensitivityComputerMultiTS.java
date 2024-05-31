/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.searchtreerao.commons.adapter.BranchResultAdapter;
import com.powsybl.openrao.searchtreerao.commons.adapter.BranchResultAdapterImpl;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.MultipleSensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.SensitivityResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class SensitivityComputerMultiTS {
    private List<SystematicSensitivityInterface> systematicSensitivityInterfaces;
    private List<BranchResultAdapter> branchResultAdapters;
    private List<SystematicSensitivityResult> results;
    private List<Set<FlowCnec>> flowCnecsList;

    private SensitivityComputerMultiTS() {
        // Should not be used
    }

    public void compute(List<Network> networks) {
        results = new ArrayList<>(); //besoin d'initialiser?
        for (int i = 0; i < networks.size(); i++) {
            results.add(systematicSensitivityInterfaces.get(i).run(networks.get(i)));
        }
    }

    public FlowResult getBranchResult(Network network, int i) {
        return branchResultAdapters.get(i).getResult(results.get(i), network); //???
    }

    public MultipleSensitivityResult getSensitivityResults() {
        MultipleSensitivityResult multipleSensitivityResult = new MultipleSensitivityResult();
        for (int i = 0; i<results.size();i++) {
            multipleSensitivityResult.addResult(results.get(i), flowCnecsList.get(i));
        }
        return multipleSensitivityResult;
    }

    public static SensitivityComputerBuilder create() {
        return new SensitivityComputerBuilder();
    }

    public static final class SensitivityComputerBuilder {
        private ToolProvider toolProvider;
        private List<Set<FlowCnec>> flowCnecsList;
        private List<Set<RangeAction<?>>> rangeActionsList;
        private FlowResult fixedPtdfs;
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
        private FlowResult fixedCommercialFlows;
        private LoopFlowComputation loopFlowComputation;
        private List<Set<FlowCnec>> loopFlowCnecsList;
        private AppliedRemedialActions appliedRemedialActions;
        private Instant outageInstant; //list of outage instants?

        public SensitivityComputerBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public SensitivityComputerBuilder withCnecs(List<Set<FlowCnec>> flowCnecsList) {
            this.flowCnecsList = flowCnecsList;
            return this;
        }

        public SensitivityComputerBuilder withRangeActions(List<Set<RangeAction<?>>> rangeActionsList) {
            this.rangeActionsList = rangeActionsList;
            return this;
        }

        public SensitivityComputerBuilder withPtdfsResults(FlowResult fixedPtdfs) {
            this.fixedPtdfs = fixedPtdfs;
            return this;
        }

        public SensitivityComputerBuilder withPtdfsResults(AbsolutePtdfSumsComputation absolutePtdfSumsComputation, List<Set<FlowCnec>> cnecs) {
            this.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            this.flowCnecsList = cnecs;
            return this;
        }

        public SensitivityComputerBuilder withCommercialFlowsResults(FlowResult fixedCommercialFlows) {
            this.fixedCommercialFlows = fixedCommercialFlows;
            return this;
        }

        public SensitivityComputerBuilder withCommercialFlowsResults(LoopFlowComputation loopFlowComputation, List<Set<FlowCnec>> loopFlowCnecsList) {
            this.loopFlowComputation = loopFlowComputation;
            this.loopFlowCnecsList = loopFlowCnecsList;
            return this;
        }

        public SensitivityComputerBuilder withAppliedRemedialActions(AppliedRemedialActions appliedRemedialActions) {
            this.appliedRemedialActions = appliedRemedialActions;
            return this;
        }

        public SensitivityComputerBuilder withOutageInstant(Instant outageInstant) {
            if (!outageInstant.isOutage()) {
                throw new OpenRaoException("The provided instant must be an outage");
            }
            this.outageInstant = outageInstant;
            return this;
        }

        public SensitivityComputerMultiTS build() {
            Objects.requireNonNull(toolProvider);
            Objects.requireNonNull(flowCnecsList);
            Objects.requireNonNull(rangeActionsList);
            Objects.requireNonNull(outageInstant);
            SensitivityComputerMultiTS sensitivityComputer = new SensitivityComputerMultiTS();
            boolean computePtdfs = absolutePtdfSumsComputation != null;
            boolean computeLoopFlows = loopFlowComputation != null;

            sensitivityComputer.flowCnecsList = flowCnecsList;
            sensitivityComputer.systematicSensitivityInterfaces = new ArrayList<>(); //besoin d'initializer?
            List<BranchResultAdapter> branchResultAdapters = new ArrayList<>();

            for (int i = 0; i < flowCnecsList.size(); i++) {
                sensitivityComputer.systematicSensitivityInterfaces.add(toolProvider.getSystematicSensitivityInterface(
                    flowCnecsList.get(i),
                    rangeActionsList.get(i),
                    computePtdfs,
                    computeLoopFlows,
                    appliedRemedialActions,
                    outageInstant));
                BranchResultAdapterImpl.BranchResultAdpaterBuilder builder = BranchResultAdapterImpl.create();
                if (loopFlowComputation != null) {
                    builder.withCommercialFlowsResults(loopFlowComputation, loopFlowCnecsList.get(i));
                } else if (fixedCommercialFlows != null) {
                    builder.withCommercialFlowsResults(fixedCommercialFlows);
                }
                if (absolutePtdfSumsComputation != null) {
                    builder.withPtdfsResults(absolutePtdfSumsComputation, flowCnecsList.get(i));
                } else if (fixedPtdfs != null) {
                    builder.withPtdfsResults(fixedPtdfs);
                }
                branchResultAdapters.add(builder.build());
            }
            sensitivityComputer.branchResultAdapters = branchResultAdapters;


            return sensitivityComputer;
        }
    }

}
