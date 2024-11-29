/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.openrao.raoapi.parameters.ParametersUtil;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;

import java.util.ArrayList;
import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class LoopFlowParametersConfigLoader implements RaoParameters.ConfigLoader<LoopFlowParametersExtension> {

    @Override
    public LoopFlowParametersExtension load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(LOOP_FLOW_PARAMETERS_SECTION)
                .map(config -> {
                    LoopFlowParametersExtension parameters = new LoopFlowParametersExtension();
                    parameters.setAcceptableIncrease(config.getDoubleProperty(ACCEPTABLE_INCREASE, LoopFlowParametersExtension.DEFAULT_ACCEPTABLE_INCREASE));
                    parameters.setPtdfApproximation(config.getEnumProperty(PTDF_APPROXIMATION, PtdfApproximation.class, LoopFlowParametersExtension.DEFAULT_PTDF_APPROXIMATION));
                    parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, LoopFlowParametersExtension.DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                    parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, LoopFlowParametersExtension.DEFAULT_VIOLATION_COST));
                    parameters.setCountries(ParametersUtil.convertToCountrySet(config.getStringListProperty(COUNTRIES, new ArrayList<>())));
                    return parameters;
                })
                .orElse(null);
    }

    @Override
    public String getExtensionName() {
        return LOOP_FLOW_PARAMETERS_SECTION;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LoopFlowParametersExtension> getExtensionClass() {
        return LoopFlowParametersExtension.class;
    }
}
