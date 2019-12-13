/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(RaoComputationParameters.ConfigLoader.class)
public class ClosedOptimisationRaoParametersConfigLoader implements RaoComputationParameters.ConfigLoader<ClosedOptimisationRaoParameters> {

    private static final String MODULE_NAME = "closed-optimisation-rao-parameters";

    @Override
    public ClosedOptimisationRaoParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        ClosedOptimisationRaoParameters parameters = new ClosedOptimisationRaoParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setSolverType(config.getStringProperty("solver-type", ClosedOptimisationRaoParameters.DEFAULT_SOLVER_TYPE));
            parameters.setRelativeMipGap(config.getDoubleProperty("relative-mip-gap", ClosedOptimisationRaoParameters.DEFAULT_RELATIVE_MIP_GAP));
            parameters.setMaxTimeInSeconds(config.getDoubleProperty("max-time-in-seconds", ClosedOptimisationRaoParameters.DEFAULT_MAX_TIME));
            parameters.setOverloadPenaltyCost(config.getDoubleProperty("overload-penalty-cost", ClosedOptimisationRaoParameters.DEFAULT_OVERLOAD_PENALTY_COST));
            parameters.setRdSensitivityThreshold(config.getDoubleProperty("redispatching-sensitivity-threshold", ClosedOptimisationRaoParameters.DEFAULT_RD_SENSITIVITY_SIGNIFICANCE_THRESHOLD));
            parameters.setPstSensitivityThreshold(config.getDoubleProperty("pst-sensitivity-threshold", ClosedOptimisationRaoParameters.DEFAULT_PST_SENSITIVITY_SIGNIFICANCE_THRESHOLD));
            parameters.setNumberOfParallelThreads(config.getIntProperty("number-of-parallel-threads", ClosedOptimisationRaoParameters.DEFAULT_NUMBER_OF_PARALLEL_THREADS));
            parameters.addAllFillers(config.getStringListProperty("problem-fillers"));
            parameters.addAllPreProcessors(config.getStringListProperty("pre-processors", Collections.emptyList()));
            parameters.addAllPostProcessors(config.getStringListProperty("post-processors", Collections.emptyList()));
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "ClosedOptimisationRaoParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-computation-parameters";
    }

    @Override
    public Class<? super ClosedOptimisationRaoParameters> getExtensionClass() {
        return ClosedOptimisationRaoParameters.class;
    }
}
