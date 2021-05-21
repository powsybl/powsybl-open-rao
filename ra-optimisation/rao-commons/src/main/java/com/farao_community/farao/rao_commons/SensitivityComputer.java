/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.farao_community.farao.rao_commons.adapter.*;
import com.farao_community.farao.rao_commons.result.SensitivityResultImpl;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class SensitivityComputer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityComputer.class);

    private SystematicSensitivityInterface systematicSensitivityInterface;
    private BranchResultAdapter branchResultAdapter;
    private SystematicSensitivityResult result;

    private SensitivityComputer() {
        // Should not be used
    }

    public void compute(Network network) {
        try {
            LOGGER.debug("Systematic sensitivity analysis [start]");
            result = systematicSensitivityInterface.run(network);
            LOGGER.debug("Systematic sensitivity analysis [end]");
        } catch (SensitivityAnalysisException e) {
            LOGGER.error("Systematic sensitivity computation failed on {} mode: {}", systematicSensitivityInterface.isFallback() ? "Fallback" : "Default", e.getMessage());
            throw e;
        }
    }

    public BranchResult getBranchResult() {
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
        private Set<BranchCnec> cnecs;
        private Set<RangeAction> rangeActions;
        private BranchResult fixedPtdfs;
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
        private BranchResult fixedCommercialFlows;
        private LoopFlowComputation loopFlowComputation;
        private Set<BranchCnec> loopFlowCnecs;

        public SensitivityComputerBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public SensitivityComputerBuilder withCnecs(Set<BranchCnec> cnecs) {
            this.cnecs = cnecs;
            return this;
        }

        public SensitivityComputerBuilder withRangeActions(Set<RangeAction> rangeActions) {
            this.rangeActions = rangeActions;
            return this;
        }

        public SensitivityComputerBuilder withPtdfsResults(BranchResult fixedPtdfs) {
            this.fixedPtdfs = fixedPtdfs;
            return this;
        }

        public SensitivityComputerBuilder withPtdfsResults(AbsolutePtdfSumsComputation absolutePtdfSumsComputation, Set<BranchCnec> cnecs) {
            this.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            this.cnecs = cnecs;
            return this;
        }

        public SensitivityComputerBuilder withCommercialFlowsResults(BranchResult fixedCommercialFlows) {
            this.fixedCommercialFlows = fixedCommercialFlows;
            return this;
        }

        public SensitivityComputerBuilder withCommercialFlowsResults(LoopFlowComputation loopFlowComputation, Set<BranchCnec> loopFlowCnecs) {
            this.loopFlowComputation = loopFlowComputation;
            this.loopFlowCnecs = loopFlowCnecs;
            return this;
        }

        public SensitivityComputer build() {
            Objects.requireNonNull(toolProvider);
            Objects.requireNonNull(cnecs);
            Objects.requireNonNull(rangeActions);
            SensitivityComputer sensitivityComputer = new SensitivityComputer();
            boolean computePtdfs = absolutePtdfSumsComputation != null;
            boolean computeLoopFlows = loopFlowComputation != null;
            sensitivityComputer.systematicSensitivityInterface = toolProvider.getSystematicSensitivityInterface(
                    cnecs,
                    rangeActions,
                    computePtdfs,
                    computeLoopFlows
            );
            BranchResultAdapterImpl.BranchResultAdpaterBuilder builder = BranchResultAdapterImpl.create();
            if (loopFlowComputation != null) {
                builder.withCommercialFlowsResults(loopFlowComputation, loopFlowCnecs);
            } else if (fixedCommercialFlows != null) {
                builder.withCommercialFlowsResults(fixedCommercialFlows);
            }
            if (absolutePtdfSumsComputation != null) {
                builder.withPtdfsResults(absolutePtdfSumsComputation, cnecs);
            } else if (fixedPtdfs != null) {
                builder.withPtdfsResults(fixedPtdfs);
            }
            sensitivityComputer.branchResultAdapter = builder.build();
            return sensitivityComputer;
        }
    }

}
