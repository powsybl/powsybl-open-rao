/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * Topological actions optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class TopoOptimizationParameters {
    // Default values
    private static final double DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD = 0;
    private static final double DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD = 0;
    // Attributes
    private double relativeMinImpactThreshold = DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD;
    private double absoluteMinImpactThreshold = DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD;

    public void setRelativeMinImpactThreshold(double relativeMinImpactThreshold) {
        if (relativeMinImpactThreshold < 0) {
            BUSINESS_WARNS.warn("The value {} provided for relative minimum impact threshold is smaller than 0. It will be set to 0.", relativeMinImpactThreshold);
            this.relativeMinImpactThreshold = 0;
        } else if (relativeMinImpactThreshold > 1) {
            BUSINESS_WARNS.warn("The value {} provided for relativeminimum impact threshold is greater than 1. It will be set to 1.", relativeMinImpactThreshold);
            this.relativeMinImpactThreshold = 1;
        } else {
            this.relativeMinImpactThreshold = relativeMinImpactThreshold;
        }
    }

    public void setAbsoluteMinImpactThreshold(double absoluteMinImpactThreshold) {
        this.absoluteMinImpactThreshold = absoluteMinImpactThreshold;
    }

    public double getRelativeMinImpactThreshold() {
        return relativeMinImpactThreshold;
    }

    public double getAbsoluteMinImpactThreshold() {
        return absoluteMinImpactThreshold;
    }

    public static TopoOptimizationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        TopoOptimizationParameters parameters = new TopoOptimizationParameters();
        platformConfig.getOptionalModuleConfig(TOPOLOGICAL_ACTIONS_OPTIMIZATION_SECTION)
                .ifPresent(config -> {
                    parameters.setRelativeMinImpactThreshold(config.getDoubleProperty(RELATIVE_MINIMUM_IMPACT_THRESHOLD, DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD));
                    parameters.setAbsoluteMinImpactThreshold(config.getDoubleProperty(ABSOLUTE_MINIMUM_IMPACT_THRESHOLD, DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD));
                });
        return parameters;
    }
}
