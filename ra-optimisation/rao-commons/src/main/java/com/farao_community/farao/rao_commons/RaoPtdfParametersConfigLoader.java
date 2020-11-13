/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.rao_api.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Optional;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class RaoPtdfParametersConfigLoader implements RaoParameters.ConfigLoader<RaoPtdfParameters> {
    private static final String MODULE_NAME = "rao-ptdf-parameters";

    @Override
    public RaoPtdfParameters load(PlatformConfig platformConfig) {
        RaoPtdfParameters parameters = new RaoPtdfParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setBoundariesFromCountryCodes(config.getStringListProperty("boundaries"));
            parameters.setPtdfSumLowerBound(config.getDoubleProperty("ptdf-sum-lower-bound", RaoPtdfParameters.DEFAULT_PTDF_SUM_LOWER_BOUND));
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "RaoPtdfParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super RaoPtdfParameters> getExtensionClass() {
        return RaoPtdfParameters.class;
    }
}
