/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.adapter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.results.FlowResult;
import com.farao_community.farao.rao_commons.AbsolutePtdfSumsComputation;
import com.farao_community.farao.rao_commons.result.FlowResultImpl;
import com.farao_community.farao.rao_commons.result.EmptyFlowResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class BranchResultAdapterImpl implements BranchResultAdapter {
    private FlowResult fixedPtdfs = new EmptyFlowResult();
    private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
    private Set<FlowCnec> flowCnecs;
    private FlowResult fixedCommercialFlows = new EmptyFlowResult();
    private LoopFlowComputation loopFlowComputation;
    private Set<FlowCnec> loopFlowCnecs;

    private BranchResultAdapterImpl() {
        // Should not be used
    }

    public static BranchResultAdpaterBuilder create() {
        return new BranchResultAdpaterBuilder();
    }

    @Override
    public FlowResult getResult(SystematicSensitivityResult systematicSensitivityResult) {
        FlowResult ptdfs;
        if (absolutePtdfSumsComputation != null) {
            Map<FlowCnec, Double> ptdfsMap = absolutePtdfSumsComputation.computeAbsolutePtdfSums(flowCnecs, systematicSensitivityResult);
            ptdfs = new FlowResult() {
                @Override
                public double getFlow(FlowCnec flowCnec, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getCommercialFlow(FlowCnec flowCnec, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getPtdfZonalSum(FlowCnec flowCnec) {
                    if (ptdfsMap.containsKey(flowCnec)) {
                        return ptdfsMap.get(flowCnec);
                    } else {
                        throw new FaraoException(String.format("No PTDF zonal sum for cnec %s", flowCnec.getId()));
                    }
                }

                @Override
                public Map<FlowCnec, Double> getPtdfZonalSums() {
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
                    loopFlowCnecs
            );
            commercialFlows = new FlowResult() {
                @Override
                public double getFlow(FlowCnec flowCnec, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getCommercialFlow(FlowCnec flowCnec, Unit unit) {
                    if (unit == Unit.MEGAWATT) {
                        return loopFlowResult.getCommercialFlow(flowCnec);
                    } else {
                        throw new NotImplementedException();
                    }

                }

                @Override
                public double getPtdfZonalSum(FlowCnec flowCnec) {
                    throw new NotImplementedException();
                }

                @Override
                public Map<FlowCnec, Double> getPtdfZonalSums() {
                    throw new NotImplementedException();
                }
            };
        } else {
            commercialFlows = fixedCommercialFlows;
        }
        return new FlowResultImpl(systematicSensitivityResult, commercialFlows, ptdfs);
    }

    public static final class BranchResultAdpaterBuilder {
        private FlowResult fixedPtdfs = new EmptyFlowResult();
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
        private Set<FlowCnec> cnecs;
        private FlowResult fixedCommercialFlows = new EmptyFlowResult();
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
