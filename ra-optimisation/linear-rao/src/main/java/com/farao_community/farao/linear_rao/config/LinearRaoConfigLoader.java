/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.sensitivity.SensitivityComputationParameters;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class LinearRaoConfigLoader implements RaoParameters.ConfigLoader<LinearRaoParameters>  {

    private static final String MODULE_NAME = "linear-rao-parameters";

    @Override
    public LinearRaoParameters load(PlatformConfig platformConfig) {
        LinearRaoParameters parameters = new LinearRaoParameters();
        parameters.setSensitivityComputationParameters(SensitivityComputationParameters.load(platformConfig));
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setMaxIterations(config.getIntProperty("max-number-of-iterations", LinearRaoParameters.DEFAULT_MAX_NUMBER_OF_ITERATIONS));
            parameters.setSkipLinearRao(config.getBooleanProperty("skip-linear-rao", LinearRaoParameters.DEFAULT_SKIP_LINEAR_RAO));
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "LinearRaoParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LinearRaoParameters> getExtensionClass() {
        return LinearRaoParameters.class;
    }
}
