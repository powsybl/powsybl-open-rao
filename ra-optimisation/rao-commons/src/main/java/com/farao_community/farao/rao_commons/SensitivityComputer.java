/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_commons.adapter.*;
import com.farao_community.farao.rao_commons.result.SensitivityResultImpl;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;

import java.util.*;

import static com.farao_community.farao.commons.FaraoLogger.TECHNICAL_LOGS;

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
        try {
            TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
            result = systematicSensitivityInterface.run(network);
            TECHNICAL_LOGS.debug("Systematic sensitivity analysis [end]");
        } catch (SensitivityAnalysisException e) {
            TECHNICAL_LOGS.warn("Systematic sensitivity computation failed on {} mode: {}", systematicSensitivityInterface.isFallback() ? "Fallback" : "Default", e.getMessage());
            throw e;
        }
    }

    public FlowResult getBranchResult() {
        return branchResultAdapter.getResult(result);
    }

    public SensitivityResult getSensitivityResult() {
        return new SensitivityResultImpl(result);
    }

    public static SensitivityComputerBuilder create() {
        return new SensitivityComputerBuilder();
    }

    public static final class SensitivityComputerBuilder {
        private ToolProvider toolProvider;
        private Set<FlowCnec> flowCnecs;
        private Set<RangeAction> rangeActions;
        private FlowResult fixedPtdfs;
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
        private FlowResult fixedCommercialFlows;
        private LoopFlowComputation loopFlowComputation;
        private Set<FlowCnec> loopFlowCnecs;
        private AppliedRemedialActions appliedRemedialActions;

        public SensitivityComputerBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public SensitivityComputerBuilder withCnecs(Set<FlowCnec> flowCnecs) {
            this.flowCnecs = flowCnecs;
            return this;
        }

        public SensitivityComputerBuilder withRangeActions(Set<RangeAction> rangeActions) {
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

        public SensitivityComputer build() {
            Objects.requireNonNull(toolProvider);
            Objects.requireNonNull(flowCnecs);
            Objects.requireNonNull(rangeActions);
            SensitivityComputer sensitivityComputer = new SensitivityComputer();
            boolean computePtdfs = absolutePtdfSumsComputation != null;
            boolean computeLoopFlows = loopFlowComputation != null;
            sensitivityComputer.systematicSensitivityInterface = toolProvider.getSystematicSensitivityInterface(
                    flowCnecs,
                    rangeActions,
                    computePtdfs,
                    computeLoopFlows,
                    appliedRemedialActions
            );
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
