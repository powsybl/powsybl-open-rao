/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_api.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Optional;

import static com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters.DEFAULT_LOOPFLOW_APPROXIMATION;
import static com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters.DEFAULT_MAX_NUMBER_OF_ITERATIONS;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class IteratingLinearOptimizerConfigLoader implements RaoParameters.ConfigLoader<IteratingLinearOptimizerParameters> {
    private static final String MODULE_NAME = "iterating-linear-optimizer-parameters";

    @Override
    public IteratingLinearOptimizerParameters load(PlatformConfig platformConfig) {
        IteratingLinearOptimizerParameters parameters = new IteratingLinearOptimizerParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setMaxIterations(config.getIntProperty("max-number-of-iterations", DEFAULT_MAX_NUMBER_OF_ITERATIONS));
            parameters.setLoopflowApproximation(config.getBooleanProperty("loopflow-approximation", DEFAULT_LOOPFLOW_APPROXIMATION));
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "IteratingLinearOptimizerParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super IteratingLinearOptimizerParameters> getExtensionClass() {
        return IteratingLinearOptimizerParameters.class;
    }
}
