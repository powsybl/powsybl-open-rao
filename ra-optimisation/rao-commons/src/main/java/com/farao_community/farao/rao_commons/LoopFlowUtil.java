/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LoopFlowUtil {

    private LoopFlowUtil() {
    }

    public static void buildLoopFlowsWithLatestSensi(RaoData raoData, boolean isLoopFlowApproximation) {
        if (isLoopFlowApproximation) {
            raoData.getCracResultManager().fillCnecResultsWithApproximatedLoopFlows();
        } else {
            LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getGlskProvider(), raoData.getReferenceProgram());
            LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getNetwork(), raoData.getSystematicSensitivityResult(), raoData.getLoopflowCnecs());
            raoData.getCracResultManager().fillCnecResultsWithLoopFlows(lfResults);
        }
    }

    public static Map<BranchCnec, Double> computeCommercialFlows(Network network,
                                                                 Set<BranchCnec> cnecs,
                                                                 ZonalData<LinearGlsk> glskProvider,
                                                                 ReferenceProgram referenceProgram,
                                                                 SystematicSensitivityResult sensitivityAndFlowResult) {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(glskProvider, referenceProgram);
        LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(network, sensitivityAndFlowResult, cnecs);
        Map<BranchCnec, Double> commercialFlows = new HashMap<>();
        for (BranchCnec cnec : cnecs) {
            commercialFlows.put(cnec, lfResults.getCommercialFlow(cnec));
        }
        return  commercialFlows;
    }
}
