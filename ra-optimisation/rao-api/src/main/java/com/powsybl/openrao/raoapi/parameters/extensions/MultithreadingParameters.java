/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Objects;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
/**
 * Multi-threading optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class MultithreadingParameters {
    private static final int DEFAULT_AVAILABLE_CPUS = 1;
    private int availableCPUs = DEFAULT_AVAILABLE_CPUS;

    public int getAvailableCPUs() {
        return availableCPUs;
    }

    public void setAvailableCPUs(int availableCPUs) {
        this.availableCPUs = availableCPUs;
    }

    public static MultithreadingParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        MultithreadingParameters parameters = new MultithreadingParameters();
        platformConfig.getOptionalModuleConfig(MULTI_THREADING_SECTION)
                .ifPresent(config -> {
                    int availableCpus = config.getIntProperty(AVAILABLE_CPUS, 1);
                    parameters.setAvailableCPUs(availableCpus);
                });
        return parameters;
    }

    public static int getAvailableCPUs(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getMultithreadingParameters().getAvailableCPUs();
        }
        return DEFAULT_AVAILABLE_CPUS;
    }
}
