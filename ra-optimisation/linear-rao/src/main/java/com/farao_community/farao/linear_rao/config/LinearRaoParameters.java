/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.sensitivity.SensitivityComputationParameters;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoParameters extends AbstractExtension<RaoParameters> {

    private SensitivityComputationParameters sensitivityComputationParameters = new SensitivityComputationParameters();

    @Override
    public String getName() {
        return "LinearRaoParameters";
    }

    public SensitivityComputationParameters getSensitivityComputationParameters() {
        return sensitivityComputationParameters;
    }

    public LinearRaoParameters setSensitivityComputationParameters(SensitivityComputationParameters sensiParameters) {
        this.sensitivityComputationParameters = Objects.requireNonNull(sensiParameters);
        return this;
    }
}
