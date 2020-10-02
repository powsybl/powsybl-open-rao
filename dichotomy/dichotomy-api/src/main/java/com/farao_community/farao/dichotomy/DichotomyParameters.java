/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class DichotomyParameters {

    private static final double DEFAULT_INITIAL_STEP = 500;
    private static final double DEFAULT_MINIMUM_STEP = 50;

    private double initialStep = DEFAULT_INITIAL_STEP;
    private double minimumStep = DEFAULT_MINIMUM_STEP;

    public double getInitialStep() {
        return initialStep;
    }

    public void setInitialStep(double initialStep) {
        this.initialStep = initialStep;
    }

    public double getMinimumStep() {
        return minimumStep;
    }

    public void setMinimumStep(double minimumStep) {
        this.minimumStep = minimumStep;
    }

    /**
     * @return RaoParameters from platform default config.
     */
    public static DichotomyParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    /**
     * @param platformConfig PlatformConfig where the RaoParameters should be read from
     * @return RaoParameters from the provided platform config
     */
    public static DichotomyParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        DichotomyParameters parameters = new DichotomyParameters();
        load(parameters, platformConfig);

        return parameters;
    }

    private static void load(DichotomyParameters parameters, PlatformConfig platformConfig) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(platformConfig);

        platformConfig.getOptionalModuleConfig("dichotomy-parameters")
            .ifPresent(config -> {
                parameters.setInitialStep(config.getDoubleProperty("initial-step", DEFAULT_INITIAL_STEP));
                parameters.setMinimumStep(config.getDoubleProperty("minimum-step", DEFAULT_MINIMUM_STEP));
            });
    }
}
