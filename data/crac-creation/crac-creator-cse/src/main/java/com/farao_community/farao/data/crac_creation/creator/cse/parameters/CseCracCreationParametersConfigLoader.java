/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.parameters;

import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Optional;

import static com.farao_community.farao.data.crac_creation.creator.cse.parameters.CseCracCreationParameters.DEFAULT_RA_GROUPS_AS_STRING;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(CracCreationParameters.ConfigLoader.class)
public class CseCracCreationParametersConfigLoader implements CracCreationParameters.ConfigLoader<CseCracCreationParameters> {

    private static final String MODULE_NAME = "cse-crac-creation-parameters";

    @Override
    public CseCracCreationParameters load(PlatformConfig platformConfig) {
        CseCracCreationParameters parameters = new CseCracCreationParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setRangeActionGroupsAsString(config.getStringListProperty("range-action-groups", DEFAULT_RA_GROUPS_AS_STRING));
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "CseCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super CseCracCreationParameters> getExtensionClass() {
        return CseCracCreationParameters.class;
    }

}
