/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
final class SystematicSensitivityAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityAdapter.class);

    static SystematicSensitivityResult runSensitivity(Network network,
                                                               CnecSensitivityProvider cnecSensitivityProvider,
                                                               SensitivityAnalysisParameters sensitivityComputationParameters) {
        LOGGER.debug("Sensitivity analysis [start]");
        SensitivityAnalysisResult result = SensitivityAnalysis.run(network, cnecSensitivityProvider, cnecSensitivityProvider.getContingencies(network), sensitivityComputationParameters);
        LOGGER.debug("Sensitivity analysis [end]");
        return new SystematicSensitivityResult(result);
    }

    private SystematicSensitivityAdapter() {
    }
}
