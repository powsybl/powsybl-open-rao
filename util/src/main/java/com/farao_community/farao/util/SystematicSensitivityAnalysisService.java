/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import com.rte_france.powsybl.iidm.export.adn.ADNLoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class SystematicSensitivityAnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityAnalysisService.class);

    private SystematicSensitivityAnalysisService() {
    }

    public static SystematicSensitivityAnalysisResult runAnalysis(Network network,
                                                                  Crac crac,
                                                                  SensitivityComputationParameters sensitivityComputationParameters) {
        LOGGER.debug("Sensi run method [start]"); // temp ==> DELETE THIS LOG
        SensitivityComputationResults allStatesSensi = runSensitivityComputation(network, crac, sensitivityComputationParameters);
        LOGGER.debug("Sensi run method [end]"); // temp ==> DELETE THIS LOG

        LOGGER.debug("Filling systematic analysis results [start]"); // DELETE THIS LOG
        SystematicSensitivityAnalysisResult results = new SystematicSensitivityAnalysisResult(allStatesSensi);
        LOGGER.debug("Filling systematic analysis results [end]"); // DELETE THIS LOG
        return results;
    }

    private static SensitivityComputationResults runSensitivityComputation(
            Network network,
            Crac crac,
            SensitivityComputationParameters sensitivityComputationParameters) {

        SensitivityFactorsProvider factorsProvider = new CracFactorsProvider(crac, !isDc(sensitivityComputationParameters));
        ContingenciesProvider contingenciesProvider = new CracContingenciesProvider(crac);
        try {
            return SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider, contingenciesProvider, sensitivityComputationParameters);
        } catch (FaraoException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    private static boolean isDc(SensitivityComputationParameters sensitivityComputationParameters) {

        boolean isDc = false;

         /*
        todo : do something more generic, less specific to Hades sensitivity implementation
         (it is not possible to check this for now as the PowSyBl API does not allow yet to retrieve
         the AC/DC information of the sensi).
         */
        if (sensitivityComputationParameters.getLoadFlowParameters() != null &&
            sensitivityComputationParameters.getLoadFlowParameters().getExtension(ADNLoadFlowParameters.class) != null) {

            isDc = sensitivityComputationParameters.getLoadFlowParameters().getExtension(ADNLoadFlowParameters.class).isDcMode();
        } else {
            LOGGER.info("AC/DC mode of the sensitivity engine is unknown, BranchIntensity sensitivities will be by default transmitted to the engine.");
        }

        return isDc;
    }
}
