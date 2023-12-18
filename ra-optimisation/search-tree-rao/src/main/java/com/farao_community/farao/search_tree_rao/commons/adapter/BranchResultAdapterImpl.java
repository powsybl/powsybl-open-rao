/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.commons.adapter;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.loopflow_computation.LoopFlowComputation;
import com.powsybl.open_rao.loopflow_computation.LoopFlowResult;
import com.powsybl.open_rao.search_tree_rao.commons.AbsolutePtdfSumsComputation;
import com.powsybl.open_rao.search_tree_rao.result.impl.FlowResultImpl;
import com.powsybl.open_rao.search_tree_rao.result.api.FlowResult;
import com.powsybl.open_rao.search_tree_rao.result.impl.EmptyFlowResultImpl;
import com.powsybl.open_rao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

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
        FlowResult ptdfs;
        if (absolutePtdfSumsComputation != null) {
            Map<FlowCnec, Map<Side, Double>> ptdfsMap = absolutePtdfSumsComputation.computeAbsolutePtdfSums(flowCnecs, systematicSensitivityResult);
            ptdfs = new FlowResult() {

                @Override
                public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
                    if (ptdfsMap.containsKey(flowCnec) && ptdfsMap.get(flowCnec).containsKey(side)) {
                        return ptdfsMap.get(flowCnec).get(side);
                    } else {
                        throw new FaraoException(String.format("No PTDF zonal sum for cnec %s (side %s)", flowCnec.getId(), side));
                    }
                }

                @Override
                public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
                    return ptdfsMap;
                }
            };
        } else {
            ptdfs = fixedPtdfs;
        }

        FlowResult commercialFlows;
        if (loopFlowComputation != null) {
            LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(
                    systematicSensitivityResult,
                    loopFlowCnecs,
                    network
            );
            commercialFlows = new FlowResult() {
                @Override
                public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
                    if (unit == Unit.MEGAWATT) {
                        return loopFlowResult.getCommercialFlow(flowCnec, side);
                    } else {
                        throw new NotImplementedException();
                    }

                }

                @Override
                public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
                    throw new NotImplementedException();
                }

                @Override
                public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
                    throw new NotImplementedException();
                }
            };
        } else {
            commercialFlows = fixedCommercialFlows;
        }
        return new FlowResultImpl(systematicSensitivityResult, commercialFlows, ptdfs);
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
