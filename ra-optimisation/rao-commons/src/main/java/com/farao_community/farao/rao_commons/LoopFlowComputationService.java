/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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
        raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopFlowResult, raoParameters.getLoopFlowViolationCost());
    }

    public static Map<String, Double> calculateLoopFlows(RaoData raoData, boolean isLoopFlowApproximation) {
        Map<String, Double> loopFlows;
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getCrac(), raoData.getGlskProvider(), raoData.getReferenceProgram());

        if (isLoopFlowApproximation) { // do not recompute PTDFs
            //
            return loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(p)
        } else {
            loopFlows = loopFlowComputation.calculateLoopFlows(raoData.getNetwork()); // Re-compute ptdf
        }

        return loopFlows;
    }

    public static boolean isLoopFlowsViolated(RaoData raoData, Map<String, Double> loopFlows) {
        boolean violated = false;
        for (Cnec cnec : raoData.getCrac().getCnecs(raoData.getCrac().getPreventiveState())) {
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))
                && Math.abs(loopFlows.get(cnec.getId())) > Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraintInMW())) {
                violated = true;
                LOGGER.info("Some loopflow constraints are not respected.");
                break;
            }
        }
        return violated;
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
