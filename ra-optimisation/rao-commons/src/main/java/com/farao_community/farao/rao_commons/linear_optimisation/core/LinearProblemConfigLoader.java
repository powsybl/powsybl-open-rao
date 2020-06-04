/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.core;

import com.farao_community.farao.rao_api.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class LinearProblemConfigLoader implements RaoParameters.ConfigLoader<LinearProblemParameters> {

    private static final String MODULE_NAME = "linear-problem-parameters";

    @Override
    public LinearProblemParameters load(PlatformConfig platformConfig) {
        LinearProblemParameters parameters = new LinearProblemParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setPstPenaltyCost(config.getDoubleProperty("pst-penalty-cost", LinearProblemParameters.DEFAULT_PST_PENALTY_COST));
            parameters.setPstSensitivityThreshold(config.getDoubleProperty("pst-sensitivity-threshold", LinearProblemParameters.DEFAULT_PST_SENSITIVITY_THRESHOLD));
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "LinearProblemParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LinearProblemParameters> getExtensionClass() {
        return LinearProblemParameters.class;
    }
}
