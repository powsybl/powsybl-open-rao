/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
import static com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */

@AutoService(RaoParameters.ConfigLoader.class)
public class FastRaoConfigLoader implements RaoParameters.ConfigLoader<FastRaoParameters> {

    public FastRaoParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(FAST_RAO_PARAMETERS)
            .map(config -> {
                FastRaoParameters parameters = new FastRaoParameters();
                parameters.setNumberOfCnecsToAdd(config.getIntProperty(NUMBER_OF_CNECS_TO_ADD, DEFAULT_NUMBER_OF_CNECS_TO_ADD));
                parameters.setAddUnsecureCnecs(config.getBooleanProperty(ADD_UNSECURE_CNECS, DEFAULT_ADD_UNSECURE_CNECS));
                parameters.setMarginLimit(config.getDoubleProperty(MARGIN_LIMIT, DEFAULT_MARGIN_LIMIT));
                return parameters;
            })
            .orElse(null);
    }

    @Override
    public String getExtensionName() {
        return FAST_RAO_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super FastRaoParameters> getExtensionClass() {
        return FastRaoParameters.class;
    }

}
