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
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.impl.MultipleSensitivityResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

import java.util.*;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public final class SensitivityComputerMultiTS {
    private List<SystematicSensitivityInterface> systematicSensitivityInterfaces;
    private List<BranchResultAdapter> branchResultAdapters;
    private List<SystematicSensitivityResult> results;
    private List<Set<FlowCnec>> flowCnecsList;

    private SensitivityComputerMultiTS() {
        // Should not be used
    }

    public static SensitivityComputerBuilder create() {
        return new SensitivityComputerBuilder();
    }

    public void compute(List<Network> networks) {
        results = new ArrayList<>();
        for (int i = 0; i < networks.size(); i++) {
            results.add(systematicSensitivityInterfaces.get(i).run(networks.get(i)));
        }
    }

    public FlowResult getBranchResult(Network network, int i) {
        return branchResultAdapters.get(i).getResult(results.get(i), network);
    }

    public MultipleSensitivityResult getSensitivityResults() {
        MultipleSensitivityResult multipleSensitivityResult = new MultipleSensitivityResult();
        for (int i = 0; i < results.size(); i++) {
            multipleSensitivityResult.addResult(results.get(i), flowCnecsList.get(i));
        }
        return multipleSensitivityResult;
    }

    public static final class SensitivityComputerBuilder {
        private ToolProvider toolProvider;
        private List<Set<FlowCnec>> flowCnecsList;
        private Set<RangeAction<?>> rangeActions;
        private FlowResult fixedPtdfs;
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
        private FlowResult fixedCommercialFlows;
        private LoopFlowComputation loopFlowComputation;
        private List<Set<FlowCnec>> loopFlowCnecsList;
        private AppliedRemedialActions appliedRemedialActions;
        private Instant outageInstant;
        // curently using only one outageInstant, but we could use a List ot make it cleaner

        public SensitivityComputerBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public SensitivityComputerBuilder withCnecs(List<Set<FlowCnec>> flowCnecsList) {
            this.flowCnecsList = flowCnecsList;
            return this;
        }

        public SensitivityComputerBuilder withRangeActions(Set<RangeAction<?>> rangeActions) {
            this.rangeActions = rangeActions;
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
            Objects.requireNonNull(rangeActions);
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
                    rangeActions,
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
