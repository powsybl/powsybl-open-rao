/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * Extension : MNEC parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MnecParameters {
    static final double DEFAULT_ACCEPTABLE_MARGIN_DECREASE = 50.0;
    private double acceptableMarginDecrease = DEFAULT_ACCEPTABLE_MARGIN_DECREASE;
    // "A equivalent cost per A violation" or "MW per MW", depending on the objective function

    public double getAcceptableMarginDecrease() {
        return acceptableMarginDecrease;
    }

    public void setAcceptableMarginDecrease(double acceptableMarginDecrease) {
        this.acceptableMarginDecrease = acceptableMarginDecrease;
    }

    public static Optional<MnecParameters> load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(MNEC_PARAMETERS_SECTION)
            .map(config -> {
                MnecParameters parameters = new MnecParameters();
                parameters.setAcceptableMarginDecrease(config.getDoubleProperty(ACCEPTABLE_MARGIN_DECREASE, MnecParameters.DEFAULT_ACCEPTABLE_MARGIN_DECREASE));
                return parameters;
            });
    }

    //TODO to remove
    public static double getAcceptableMarginDecrease(RaoParameters parameters) {
        return parameters.getMnecParameters().map(MnecParameters::getAcceptableMarginDecrease).orElse(DEFAULT_ACCEPTABLE_MARGIN_DECREASE);
    }
}
