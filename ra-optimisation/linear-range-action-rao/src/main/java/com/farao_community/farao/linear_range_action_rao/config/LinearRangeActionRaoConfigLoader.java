/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_range_action_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.sensitivity.SensitivityComputationParameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class LinearRangeActionRaoConfigLoader implements RaoParameters.ConfigLoader<LinearRangeActionRaoParameters>  {

    @Override
    public LinearRangeActionRaoParameters load(PlatformConfig platformConfig) {
        LinearRangeActionRaoParameters parameters = new LinearRangeActionRaoParameters();
        return parameters.setSensitivityComputationParameters(SensitivityComputationParameters.load(platformConfig));
    }

    @Override
    public String getExtensionName() {
        return "LinearRangeActionRaoParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LinearRangeActionRaoParameters> getExtensionClass() {
        return LinearRangeActionRaoParameters.class;
    }
}
