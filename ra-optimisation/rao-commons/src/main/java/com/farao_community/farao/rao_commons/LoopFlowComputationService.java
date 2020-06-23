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
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
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

    public static void computeInitialLoopFlowsAndUpdateCnecLoopFlowConstraint(RaoData raoData, double violationCost) {
        LoopFlowComputation initialLoopFlowComputation = new LoopFlowComputation(raoData.getCrac());
        Map<Cnec, Double> frefResults = initialLoopFlowComputation.computeRefFlowOnCurrentNetwork(raoData.getNetwork()); // Get reference flow
        Map<Cnec, Double> loopFlowShifts = initialLoopFlowComputation.buildZeroBalanceFlowShift(raoData.getNetwork()); // Compute PTDF * NetPosition
        Map<String, Double> loopFlows = initialLoopFlowComputation.buildLoopFlowsFromReferenceFlowAndLoopflowShifts(frefResults, loopFlowShifts);
        raoData.getRaoDataManager().fillCracResultsWithLoopFlowConstraints(loopFlows, loopFlowShifts);
        raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopFlows, violationCost);
    }

    public static Map<String, Double> calculateLoopFlows(RaoData raoData, boolean isLoopFlowApproximation) {
        Map<String, Double> loopFlows;
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getCrac());
        if (isLoopFlowApproximation) { // No re-compute ptdf
            loopFlows = loopFlowComputation.calculateLoopFlowsApproximation(raoData.getNetwork());
        } else {
            loopFlows = loopFlowComputation.calculateLoopFlows(raoData.getNetwork()); // Re-compute ptdf
        }
        return loopFlows;
    }

    public static boolean isLoopFlowsViolated(RaoData raoData, Map<String, Double> loopFlows) {
        boolean violated = false;
        for (Cnec cnec : raoData.getCrac().getCnecs(raoData.getCrac().getPreventiveState())) {
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))
                && Math.abs(loopFlows.get(cnec.getId())) > Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint())) {
                violated = true;
                LOGGER.info("Some loopflow constraints are not respected.");
                break;
            }
        }
        return violated;
    }

    public static void checkDataConsistency(RaoData raoData) {
        if (Objects.isNull(raoData.getCrac().getExtension(CracLoopFlowExtension.class))) {
            String msg = format(
                "Loopflow computation cannot be performed CRAC %s because it does not have loop flow extension",
                raoData.getCrac().getId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }
}
