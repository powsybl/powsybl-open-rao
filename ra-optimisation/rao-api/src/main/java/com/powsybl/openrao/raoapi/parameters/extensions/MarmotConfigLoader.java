/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.MARGIN_WINDOW_TO_CONSIDER;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MARMOT_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MAX_MIP_ITERATIONS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MIN_RELATIVE_IMPROVEMENT_ON_MARGIN;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.PARALLELISM;
import static com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters.DEFAULT_MARGIN_WINDOW_TO_CONSIDER;
import static com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters.DEFAULT_MAX_MIP_ITERATIONS;
import static com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters.DEFAULT_MIN_RELATIVE_IMPROVEMENT_ON_MARGIN;
import static com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters.DEFAULT_NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME;
import static com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters.DEFAULT_PARALLELISM;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class MarmotConfigLoader implements RaoParameters.ConfigLoader<MarmotParameters> {
    public MarmotParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(MARMOT_PARAMETERS)
            .map(config -> {
                MarmotParameters parameters = new MarmotParameters();
                parameters.setNumberOfCnecsToAddPerVirtualCostName(config.getIntProperty(NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME, DEFAULT_NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME));
                parameters.setMinRelativeImprovementOnMargin(config.getDoubleProperty(MIN_RELATIVE_IMPROVEMENT_ON_MARGIN, DEFAULT_MIN_RELATIVE_IMPROVEMENT_ON_MARGIN));
                parameters.setMarginWindowToConsider(config.getDoubleProperty(MARGIN_WINDOW_TO_CONSIDER, DEFAULT_MARGIN_WINDOW_TO_CONSIDER));
                parameters.setMaxMipIterations(config.getIntProperty(MAX_MIP_ITERATIONS, DEFAULT_MAX_MIP_ITERATIONS));
                parameters.setParallelism(config.getIntProperty(PARALLELISM, DEFAULT_PARALLELISM));
                return parameters;
            })
            .orElse(null);
    }

    @Override
    public String getExtensionName() {
        return MARMOT_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super MarmotParameters> getExtensionClass() {
        return MarmotParameters.class;
    }

}
