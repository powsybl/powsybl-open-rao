/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class MnecParametersConfigLoader implements RaoParameters.ConfigLoader<MnecParametersExtension> {

    @Override
    public MnecParametersExtension load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(MNEC_PARAMETERS_SECTION)
                .map(config -> {
                    MnecParametersExtension parameters = new MnecParametersExtension();
                    parameters.setAcceptableMarginDecrease(config.getDoubleProperty(ACCEPTABLE_MARGIN_DECREASE, MnecParametersExtension.DEFAULT_ACCEPTABLE_MARGIN_DECREASE));
                    parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, MnecParametersExtension.DEFAULT_VIOLATION_COST));
                    parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, MnecParametersExtension.DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                    return parameters;
                })
                .orElse(null);
    }

    @Override
    public String getExtensionName() {
        return MNEC_PARAMETERS_SECTION;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super MnecParametersExtension> getExtensionClass() {
        return MnecParametersExtension.class;
    }
}
