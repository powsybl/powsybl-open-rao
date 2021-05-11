/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.adapter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.AbsolutePtdfSumsComputation;
import com.farao_community.farao.rao_commons.result.BranchResultImpl;
import com.farao_community.farao.rao_commons.result.EmptyBranchResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class BranchResultAdapterImpl implements BranchResultAdapter {
    private BranchResult fixedPtdfs = new EmptyBranchResult();
    private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
    private Set<BranchCnec> cnecs;
    private BranchResult fixedCommercialFlows = new EmptyBranchResult();
    private LoopFlowComputation loopFlowComputation;
    private Set<BranchCnec> loopFlowCnecs;

    private BranchResultAdapterImpl() {
        // Should not be used
    }

    public static BranchResultAdpaterWithNoComputationBuilder create() {
        return new BranchResultAdpaterWithNoComputationBuilder();
    }

    @Override
    public BranchResult getResult(SystematicSensitivityResult systematicSensitivityResult) {
        BranchResult ptdfs;
        if (absolutePtdfSumsComputation != null) {
            Map<BranchCnec, Double> ptdfsMap = absolutePtdfSumsComputation.computeAbsolutePtdfSums(cnecs, systematicSensitivityResult);
            ptdfs = new BranchResult() {
                @Override
                public double getFlow(BranchCnec branchCnec, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getPtdfZonalSum(BranchCnec branchCnec) {
                    return ptdfsMap.get(branchCnec);
                }

                @Override
                public Map<BranchCnec, Double> getPtdfZonalSums() {
                    return ptdfsMap;
                }
            };
        } else if (fixedPtdfs != null) {
            ptdfs = fixedPtdfs;
        } else {
            ptdfs = new EmptyBranchResult();
        }

        BranchResult commercialFlows;
        if (loopFlowComputation != null) {
            LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(
                    systematicSensitivityResult,
                    loopFlowCnecs
            );
            commercialFlows = new BranchResult() {
                @Override
                public double getFlow(BranchCnec branchCnec, Unit unit) {
                    throw new NotImplementedException();
                }

                @Override
                public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
                    if (unit == Unit.MEGAWATT) {
                        return loopFlowResult.getCommercialFlow(branchCnec);
                    } else {
                        throw new NotImplementedException();
                    }

                }

                @Override
                public double getPtdfZonalSum(BranchCnec branchCnec) {
                    throw new NotImplementedException();
                }

                @Override
                public Map<BranchCnec, Double> getPtdfZonalSums() {
                    throw new NotImplementedException();
                }
            };
        } else if (fixedCommercialFlows != null) {
            commercialFlows = fixedCommercialFlows;
        } else {
            commercialFlows = new EmptyBranchResult();
        }
        return new BranchResultImpl(systematicSensitivityResult, commercialFlows, ptdfs);
    }

    public static final class BranchResultAdpaterWithNoComputationBuilder {
        private BranchResult fixedPtdfs = new EmptyBranchResult();
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
        private Set<BranchCnec> cnecs;
        private BranchResult fixedCommercialFlows = new EmptyBranchResult();
        private LoopFlowComputation loopFlowComputation;
        private Set<BranchCnec> loopFlowCnecs;

        public BranchResultAdpaterWithNoComputationBuilder withPtdfsResults(BranchResult fixedPtdfs) {
            this.fixedPtdfs = fixedPtdfs;
            return this;
        }

        public BranchResultAdpaterWithNoComputationBuilder withPtdfsResults(AbsolutePtdfSumsComputation absolutePtdfSumsComputation, Set<BranchCnec> cnecs) {
            this.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            this.cnecs = cnecs;
            return this;
        }

        public BranchResultAdpaterWithNoComputationBuilder withCommercialFlowsResults(BranchResult fixedCommercialFlows) {
            this.fixedCommercialFlows = fixedCommercialFlows;
            return this;
        }

        public BranchResultAdpaterWithNoComputationBuilder withCommercialFlowsResults(LoopFlowComputation loopFlowComputation, Set<BranchCnec> loopFlowCnecs) {
            this.loopFlowComputation = loopFlowComputation;
            this.loopFlowCnecs = loopFlowCnecs;
            return this;
        }

        public BranchResultAdapterImpl build() {
            BranchResultAdapterImpl adapter = new BranchResultAdapterImpl();
            adapter.fixedPtdfs = fixedPtdfs;
            adapter.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            adapter.cnecs = cnecs;
            adapter.fixedCommercialFlows = fixedCommercialFlows;
            adapter.loopFlowComputation = loopFlowComputation;
            adapter.loopFlowCnecs = loopFlowCnecs;
            return adapter;
        }
    }
}
