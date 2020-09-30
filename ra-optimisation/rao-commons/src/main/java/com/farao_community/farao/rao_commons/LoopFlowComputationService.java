/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LoopFlowComputationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowComputationService.class);

    private LoopFlowComputationService() { }

    public static void computeInitialLoopFlowsAndUpdateCnecLoopFlowConstraint(RaoData raoData, RaoParameters raoParameters) {

        LoopFlowComputation initialLoopFlowComputation = new LoopFlowComputation(raoData.getCrac(), raoData.getGlskProvider(), raoData.getReferenceProgram());
        LoopFlowResult loopFlowResult = initialLoopFlowComputation.calculateLoopFlows(raoData.getNetwork(), raoParameters.getDefaultSensitivityComputationParameters());

        raoData.getRaoDataManager().fillCracResultsWithLoopFlowConstraints(loopFlowResult, raoData.getNetwork());
        raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopFlowResult);
    }

    public static void buildLoopFlowsWithLatestSensi(RaoData raoData, boolean isLoopFlowApproximation) {
        if (isLoopFlowApproximation) {
            raoData.getRaoDataManager().fillCracResultsWithLoopFlowApproximation();
        } else {
            LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getCrac(), raoData.getGlskProvider(), raoData.getReferenceProgram());
            LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getSystematicSensitivityResult(), raoData.getNetwork());
            raoData.getRaoDataManager().fillCracResultsWithLoopFlows(lfResults);
        }
    }

    public static void checkDataConsistency(RaoData raoData) {
        if (Objects.isNull(raoData.getReferenceProgram()) || Objects.isNull(raoData.getGlskProvider())) {
            String msg = format(
                "Loopflow computation cannot be performed CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                raoData.getCrac().getId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }
}
