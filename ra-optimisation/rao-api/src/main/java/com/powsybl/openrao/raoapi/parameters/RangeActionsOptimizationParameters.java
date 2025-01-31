/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * Range actions optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 *
 */
public class RangeActionsOptimizationParameters {

    // Default values
    private static final double DEFAULT_PST_RA_MIN_IMPACT_THRESHOLD = 0.01;
    private static final double DEFAULT_HVDC_RA_MIN_IMPACT_THRESHOLD = 0.001;
    private static final double DEFAULT_INJECTION_RA_MIN_IMPACT_THRESHOLD = 0.001;
    // Attributes
    private double pstRAMinImpactThreshold = DEFAULT_PST_RA_MIN_IMPACT_THRESHOLD;
    private double hvdcRAMinImpactThreshold = DEFAULT_HVDC_RA_MIN_IMPACT_THRESHOLD;
    private double injectionRAMinImpactThreshold = DEFAULT_INJECTION_RA_MIN_IMPACT_THRESHOLD;

    // Getters and setters
    public double getPstRAMinImpactThreshold() {
        return pstRAMinImpactThreshold;
    }

    public void setPstRAMinImpactThreshold(double pstRAMinImpactThreshold) {
        this.pstRAMinImpactThreshold = pstRAMinImpactThreshold;
    }

    public double getHvdcRAMinImpactThreshold() {
        return hvdcRAMinImpactThreshold;
    }

    public void setHvdcRAMinImpactThreshold(double hvdcRAMinImpactThreshold) {
        this.hvdcRAMinImpactThreshold = hvdcRAMinImpactThreshold;
    }

    public double getInjectionRAMinImpactThreshold() {
        return injectionRAMinImpactThreshold;
    }

    public void setInjectionRAMinImpactThreshold(double injectionRAMinImpactThreshold) {
        this.injectionRAMinImpactThreshold = injectionRAMinImpactThreshold;
    }

    public static RangeActionsOptimizationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        RangeActionsOptimizationParameters parameters = new RangeActionsOptimizationParameters();
        platformConfig.getOptionalModuleConfig(RANGE_ACTIONS_OPTIMIZATION_SECTION)
                .ifPresent(config -> {
                    parameters.setPstRAMinImpactThreshold(config.getDoubleProperty(PST_RA_MIN_IMPACT_THRESHOLD, DEFAULT_PST_RA_MIN_IMPACT_THRESHOLD));
                    parameters.setHvdcRAMinImpactThreshold(config.getDoubleProperty(HVDC_RA_MIN_IMPACT_THRESHOLD, DEFAULT_HVDC_RA_MIN_IMPACT_THRESHOLD));
                    parameters.setInjectionRAMinImpactThreshold(config.getDoubleProperty(INJECTION_RA_MIN_IMPACT_THRESHOLD, DEFAULT_INJECTION_RA_MIN_IMPACT_THRESHOLD));
                });
        return parameters;
    }
}
