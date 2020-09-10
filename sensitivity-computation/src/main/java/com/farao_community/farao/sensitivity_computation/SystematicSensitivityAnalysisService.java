/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.SensitivityComputationResults;
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
        SensitivityComputationResults allStatesSensi = runSensitivityComputation(network, crac, sensitivityComputationParameters);

        LOGGER.debug("Filling systematic analysis results [start]");
        SystematicSensitivityAnalysisResult results = new SystematicSensitivityAnalysisResult(allStatesSensi);
        LOGGER.debug("Filling systematic analysis results [end]");
        return results;
    }

    private static SensitivityComputationResults runSensitivityComputation(
            Network network,
            Crac crac,
            SensitivityComputationParameters sensitivityComputationParameters) {
        SensitivityProvider sensitivityProvider = new RangeActionSensitivityProvider(crac);
        try {
            return SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), sensitivityProvider, sensitivityProvider, sensitivityComputationParameters);
        } catch (FaraoException e) {
            LOGGER.error(e.getMessage());
            return null;
        }

    }
}
