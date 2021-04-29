/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.adapter;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.result.BranchResultImpl;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BranchResultAdapterWithFixedPtdfsAndUpdatedCommercialFlows extends BranchResultAdapterWithFixedPtdfs implements BranchResultAdapter {
    private final LoopFlowComputation loopFlowComputation;
    private final Set<BranchCnec> loopFlowCnecs;

    public BranchResultAdapterWithFixedPtdfsAndUpdatedCommercialFlows(Map<BranchCnec, Double> ptdfZonalSums,
                                                                      LoopFlowComputation loopFlowComputation,
                                                                      Set<BranchCnec> loopFlowCnecs) {
        super(ptdfZonalSums);
        this.loopFlowComputation = loopFlowComputation;
        this.loopFlowCnecs = loopFlowCnecs;
    }

    @Override
    public BranchResult getResult(SystematicSensitivityResult systematicSensitivityResult) {
        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(
                systematicSensitivityResult,
                loopFlowCnecs
        );
        Map<BranchCnec, Double> commercialFlows = loopFlowCnecs.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        loopFlowResult::getCommercialFlow
                ));
        return new BranchResultImpl(systematicSensitivityResult, commercialFlows, ptdfZonalSums);
    }
}
