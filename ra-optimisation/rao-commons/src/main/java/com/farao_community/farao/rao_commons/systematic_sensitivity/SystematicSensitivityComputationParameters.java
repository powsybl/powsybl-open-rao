/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.systematic_sensitivity;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.sensitivity.SensitivityComputationParameters;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SystematicSensitivityComputationParameters extends AbstractExtension<RaoParameters> {

    private SensitivityComputationParameters defaultParameters = new SensitivityComputationParameters();
    private SensitivityComputationParameters fallbackParameters = null;

    @Override
    public String getName() {
        return "SystematicSensitivityComputationParameters";
    }

    public SensitivityComputationParameters getDefaultParameters() {
        return defaultParameters;
    }

    public SystematicSensitivityComputationParameters setDefaultParameters(SensitivityComputationParameters sensiParameters) {
        this.defaultParameters = Objects.requireNonNull(sensiParameters);
        return this;
    }

    public SensitivityComputationParameters getFallbackParameters() {
        return fallbackParameters;
    }

    public SystematicSensitivityComputationParameters setFallbackParameters(SensitivityComputationParameters sensiParameters) {
        this.fallbackParameters = Objects.requireNonNull(sensiParameters);
        return this;
    }
}
