/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LoopFlowComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowComputation.class);

    private LoopFlowComputation() { }

    public static void run(RaoData raoData) {
        checkDataConsistency(raoData);
        Map<String, Double> loopFlows = calculateLoopFlows(raoData); // For the initial Network, compute the F_(0,all)_init
        updateCnecsLoopFlowConstraint(raoData.getCrac(), loopFlows); //todo: cnec loop flow extension need to be based on ResultVariantManger
    }

    static void updateCnecsLoopFlowConstraint(Crac crac, Map<String, Double> fZeroAll) {
        // For each Cnec, get the maximum F_(0,all)_MAX = Math.max(F_(0,all)_init, loop flow threshold
        crac.getCnecs(crac.getPreventiveState()).forEach(cnec -> {
            CnecLoopFlowExtension cnecLoopFlowExtension = cnec.getExtension(CnecLoopFlowExtension.class);
            if (!Objects.isNull(cnecLoopFlowExtension)) {
                //!!! note here we use the result of branch flow of preventive state for all cnec of all states
                //this could be ameliorated by re-calculating loopflow for each cnec in curative state: [network + cnec's contingencies + current applied remedial actions]
                double initialLoopFlow = Math.abs(fZeroAll.get(cnec.getId()));
                double loopFlowThreshold = Math.abs(cnecLoopFlowExtension.getInputLoopFlow());
                cnecLoopFlowExtension.setLoopFlowConstraint(Math.max(initialLoopFlow, loopFlowThreshold)); //todo: cnec loop flow extension need to be based on ResultVariantManger
            }
        });
    }

    public static Map<String, Double> calculateLoopFlows(RaoData raoData) {
        //todo: optim: if CnecResult contains already ptdf or loopflows, then do not recompute the whole loopflows. if (this.runLoopflow && !linearRaoData.getCracResult().hasPtdfResults())
        com.farao_community.farao.loopflow_computation.LoopFlowComputation initialLoopFlowComputation =
            new com.farao_community.farao.loopflow_computation.LoopFlowComputation(raoData.getCrac());
        return initialLoopFlowComputation.calculateLoopFlows(raoData.getNetwork()); //todo save loopflowShift and Ptdf value in CracReult, to be reused
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

    private static void checkDataConsistency(RaoData raoData) {
        if (Objects.isNull(raoData.getCrac().getExtension(CracLoopFlowExtension.class))) {
            String msg = format(
                "Loopflow computation cannot be performed CRAC %s because it does not have loop flow extension",
                raoData.getCrac().getId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }
}
