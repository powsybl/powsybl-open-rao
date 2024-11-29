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

import java.util.ArrayList;
import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class RelativeMarginsParametersConfigLoader implements RaoParameters.ConfigLoader<RelativeMarginsParametersExtension> {

    @Override
    public RelativeMarginsParametersExtension load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(RELATIVE_MARGINS_SECTION)
                .map(config -> {
                    RelativeMarginsParametersExtension parameters = new RelativeMarginsParametersExtension();
                    parameters.setPtdfBoundariesFromString(config.getStringListProperty(PTDF_BOUNDARIES, new ArrayList<>()));
                    parameters.setPtdfApproximation(config.getEnumProperty(PTDF_APPROXIMATION, PtdfApproximation.class, RelativeMarginsParametersExtension.DEFAULT_PTDF_APPROXIMATION));
                    parameters.setPtdfSumLowerBound(config.getDoubleProperty(PTDF_SUM_LOWER_BOUND, RelativeMarginsParametersExtension.DEFAULT_PTDF_SUM_LOWER_BOUND));
                    return parameters;
                })
                .orElse(null);
    }

    @Override
    public String getExtensionName() {
        return RELATIVE_MARGINS_SECTION;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super RelativeMarginsParametersExtension> getExtensionClass() {
        return RelativeMarginsParametersExtension.class;
    }
}
