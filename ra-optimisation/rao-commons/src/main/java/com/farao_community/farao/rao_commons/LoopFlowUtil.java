/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LoopFlowUtil {

    private LoopFlowUtil() { }

    public static void buildLoopFlowsWithLatestSensi(RaoData raoData, boolean isLoopFlowApproximation) {
        if (isLoopFlowApproximation) {
            raoData.getRaoDataManager().fillCnecResultsWithApproximatedLoopFlows();
        } else {
            LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getCrac(), raoData.getGlskProvider(), raoData.getReferenceProgram());
            LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getSystematicSensitivityResult(), raoData.getNetwork());
            raoData.getRaoDataManager().fillCnecResultsWithLoopFlows(lfResults);
        }
    }
}
