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

import static com.powsybl.openrao.raoapi.RaoParametersCommons.FORCED_NETWORK_ACTIONS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.LOUTRE_PARAMETERS;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoutreParameters.DEFAULT_FORCED_NETWORK_ACTIONS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class LoutreConfigLoader implements RaoParameters.ConfigLoader<LoutreParameters> {
    @Override
    public LoutreParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(LOUTRE_PARAMETERS)
            .map(config -> {
                LoutreParameters parameters = new LoutreParameters();
                parameters.setForcedNetworkActions(config.getStringListProperty(FORCED_NETWORK_ACTIONS, DEFAULT_FORCED_NETWORK_ACTIONS));
                return parameters;
            })
            .orElse(null);
    }

    @Override
    public String getExtensionName() {
        return LOUTRE_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LoutreParameters> getExtensionClass() {
        return LoutreParameters.class;
    }
}
