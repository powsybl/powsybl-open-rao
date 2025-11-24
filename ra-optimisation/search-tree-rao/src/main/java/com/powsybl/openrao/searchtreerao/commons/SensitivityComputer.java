/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.searchtreerao.commons.adapter.BranchResultAdapter;
import com.powsybl.openrao.searchtreerao.commons.adapter.BranchResultAdapterImpl;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.SensitivityResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class SensitivityComputer {
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private BranchResultAdapter branchResultAdapter;
    private SystematicSensitivityResult result;

    private SensitivityComputer() {
        // Should not be used
    }

    public void compute(Network network) {
        result = systematicSensitivityInterface.run(network);
    }

    public FlowResult getBranchResult(Network network) {
        return branchResultAdapter.getResult(result, network);
    }

    public SensitivityResult getSensitivityResult() {
        return new SensitivityResultImpl(result);
    }

    public static SensitivityComputerBuilder create(final ReportNode reportNode) {
        return new SensitivityComputerBuilder(reportNode);
    }

    public static final class SensitivityComputerBuilder {
        private final ReportNode reportNode;
        private ToolProvider toolProvider;
        private Set<FlowCnec> flowCnecs;
        private Set<RangeAction<?>> rangeActions;
        private FlowResult fixedPtdfs;
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
        private FlowResult fixedCommercialFlows;
        private LoopFlowComputation loopFlowComputation;
        private Set<FlowCnec> loopFlowCnecs;
        private AppliedRemedialActions appliedRemedialActions;
        private Instant outageInstant;

        public SensitivityComputerBuilder(final ReportNode reportNode) {
            this.reportNode = reportNode;
        }

        public SensitivityComputerBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public SensitivityComputerBuilder withCnecs(Set<FlowCnec> flowCnecs) {
            this.flowCnecs = flowCnecs;
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

        public SensitivityComputerBuilder withPtdfsResults(AbsolutePtdfSumsComputation absolutePtdfSumsComputation, Set<FlowCnec> cnecs) {
            this.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            this.flowCnecs = cnecs;
            return this;
        }

        public SensitivityComputerBuilder withCommercialFlowsResults(FlowResult fixedCommercialFlows) {
            this.fixedCommercialFlows = fixedCommercialFlows;
            return this;
        }

        public SensitivityComputerBuilder withCommercialFlowsResults(LoopFlowComputation loopFlowComputation, Set<FlowCnec> loopFlowCnecs) {
            this.loopFlowComputation = loopFlowComputation;
            this.loopFlowCnecs = loopFlowCnecs;
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

        public SensitivityComputer build() {
            Objects.requireNonNull(toolProvider);
            Objects.requireNonNull(flowCnecs);
            Objects.requireNonNull(rangeActions);
            Objects.requireNonNull(outageInstant);
            SensitivityComputer sensitivityComputer = new SensitivityComputer();
            boolean computePtdfs = absolutePtdfSumsComputation != null;
            boolean computeLoopFlows = loopFlowComputation != null;
            sensitivityComputer.systematicSensitivityInterface = toolProvider.getSystematicSensitivityInterface(
                    flowCnecs,
                    rangeActions,
                    computePtdfs,
                    computeLoopFlows,
                    appliedRemedialActions,
                    outageInstant,
                    reportNode);
            BranchResultAdapterImpl.BranchResultAdpaterBuilder builder = BranchResultAdapterImpl.create();
            if (loopFlowComputation != null) {
                builder.withCommercialFlowsResults(loopFlowComputation, loopFlowCnecs);
            } else if (fixedCommercialFlows != null) {
                builder.withCommercialFlowsResults(fixedCommercialFlows);
            }
            if (absolutePtdfSumsComputation != null) {
                builder.withPtdfsResults(absolutePtdfSumsComputation, flowCnecs);
            } else if (fixedPtdfs != null) {
                builder.withPtdfsResults(fixedPtdfs);
            }
            sensitivityComputer.branchResultAdapter = builder.build();
            return sensitivityComputer;
        }
    }

}
