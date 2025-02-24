/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.adapter;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.searchtreerao.commons.AbsolutePtdfSumsComputation;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.impl.EmptyFlowResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.FlowResultImpl;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class BranchResultAdapterImpl implements BranchResultAdapter {
    private FlowResult fixedPtdfs = new EmptyFlowResultImpl();
    private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
    private Set<FlowCnec> flowCnecs;
    private FlowResult fixedCommercialFlows = new EmptyFlowResultImpl();
    private LoopFlowComputation loopFlowComputation;
    private Set<FlowCnec> loopFlowCnecs;

    private BranchResultAdapterImpl() {
        // Should not be used
    }

    public static BranchResultAdpaterBuilder create() {
        return new BranchResultAdpaterBuilder();
    }

    @Override
    public FlowResult getResult(SystematicSensitivityResult systematicSensitivityResult, Network network) {
        FlowResult ptdfs = null;
        Map<FlowCnec, Map<TwoSides, Double>> ptdfsMap = null;
        double ptdfSumLowerBound;
        if (absolutePtdfSumsComputation != null) {
            ptdfsMap = absolutePtdfSumsComputation.computeAbsolutePtdfSums(flowCnecs, systematicSensitivityResult);
            ptdfSumLowerBound = absolutePtdfSumsComputation.getPtdfSumLowerBound();
        } else {
            ptdfs = fixedPtdfs;
            ptdfSumLowerBound = 0;
        }

        FlowResult commercialFlows = null;
        Map<FlowCnec, Map<TwoSides, Double>> commercialFlowsMap = null;
        if (loopFlowComputation != null) {
            commercialFlowsMap = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(
                    systematicSensitivityResult,
                    loopFlowCnecs,
                    network
            ).getCommercialFlowsMap();
        } else {
            commercialFlows = fixedCommercialFlows;
        }
        return new FlowResultImpl(systematicSensitivityResult, commercialFlowsMap, commercialFlows, ptdfsMap, ptdfs, ptdfSumLowerBound);
    }

    public static final class BranchResultAdpaterBuilder {
        private FlowResult fixedPtdfs = new EmptyFlowResultImpl();
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
        private Set<FlowCnec> cnecs;
        private FlowResult fixedCommercialFlows = new EmptyFlowResultImpl();
        private LoopFlowComputation loopFlowComputation;
        private Set<FlowCnec> loopFlowCnecs;

        public BranchResultAdpaterBuilder withPtdfsResults(FlowResult fixedPtdfs) {
            this.fixedPtdfs = fixedPtdfs;
            return this;
        }

        public BranchResultAdpaterBuilder withPtdfsResults(AbsolutePtdfSumsComputation absolutePtdfSumsComputation, Set<FlowCnec> cnecs) {
            this.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            this.cnecs = cnecs;
            return this;
        }

        public BranchResultAdpaterBuilder withCommercialFlowsResults(FlowResult fixedCommercialFlows) {
            this.fixedCommercialFlows = fixedCommercialFlows;
            return this;
        }

        public BranchResultAdpaterBuilder withCommercialFlowsResults(LoopFlowComputation loopFlowComputation, Set<FlowCnec> loopFlowCnecs) {
            this.loopFlowComputation = loopFlowComputation;
            this.loopFlowCnecs = loopFlowCnecs;
            return this;
        }

        public BranchResultAdapterImpl build() {
            BranchResultAdapterImpl adapter = new BranchResultAdapterImpl();
            adapter.fixedPtdfs = fixedPtdfs;
            adapter.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            adapter.flowCnecs = cnecs;
            adapter.fixedCommercialFlows = fixedCommercialFlows;
            adapter.loopFlowComputation = loopFlowComputation;
            adapter.loopFlowCnecs = loopFlowCnecs;
            return adapter;
        }
    }
}
