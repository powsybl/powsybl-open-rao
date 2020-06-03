/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.systematic_sensitivity;

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
public class SystematicSensitivityComputationConfigLoader implements RaoParameters.ConfigLoader<SystematicSensitivityComputationParameters> {
    private static final String MODULE_NAME = "systematic-sensitivity-computation-parameters";
    @Override
    public SystematicSensitivityComputationParameters load(PlatformConfig platformConfig) {
        SystematicSensitivityComputationParameters parameters = new SystematicSensitivityComputationParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setFallbackOvercost(config.getDoubleProperty("sensitivity-fallback-overcost",
                SystematicSensitivityComputationParameters.DEFAULT_FALLBACK_OVERCOST));
        }
        // NB: Only the default sensitivity parameters are loaded, not the fallback ones...
        parameters.setDefaultParameters(SensitivityComputationParameters.load(platformConfig));
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "SystematicSensitivityComputationParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super SystematicSensitivityComputationParameters> getExtensionClass() {
        return SystematicSensitivityComputationParameters.class;
    }
}
