/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.rao_api.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

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
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setObjectiveFunction(config.getEnumProperty("objective-function", LinearRaoParameters.ObjectiveFunction.class, LinearRaoParameters.DEFAULT_OBJECTIVE_FUNCTION));
            parameters.setSecurityAnalysisWithoutRao(config.getBooleanProperty("security-analysis-without-rao", LinearRaoParameters.DEFAULT_SECURITY_ANALYSIS_WITHOUT_RAO));
            parameters.setFallbackOvercost(config.getDoubleProperty("sensitivity-fallback-overcost", LinearRaoParameters.DEFAULT_FALLBACK_OVERCOST));
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
