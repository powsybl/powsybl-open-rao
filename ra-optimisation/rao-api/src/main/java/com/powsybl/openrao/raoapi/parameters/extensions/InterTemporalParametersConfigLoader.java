/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class InterTemporalParametersConfigLoader implements RaoParameters.ConfigLoader<InterTemporalParametersExtension> {

    @Override
    public InterTemporalParametersExtension load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        InterTemporalParametersExtension parameters = new InterTemporalParametersExtension();
        platformConfig.getOptionalModuleConfig(INTER_TEMPORAL_PARAMETERS_SECTION)
                .ifPresent(config -> parameters.setSensitivityComputationsInParallel(config.getIntProperty(SENSITIVITY_COMPUTATIONS_IN_PARALLEL, InterTemporalParametersExtension.DEFAULT_SENSITIVITY_COMPUTATIONS_IN_PARALLEL)));
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return INTER_TEMPORAL_PARAMETERS_SECTION;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super InterTemporalParametersExtension> getExtensionClass() {
        return InterTemporalParametersExtension.class;
    }
}
