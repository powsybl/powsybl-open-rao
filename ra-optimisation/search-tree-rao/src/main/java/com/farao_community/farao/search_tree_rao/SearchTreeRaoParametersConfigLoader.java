/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.rao_api.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.sensitivity.SensitivityComputationParameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class SearchTreeRaoParametersConfigLoader implements RaoParameters.ConfigLoader<SearchTreeRaoParameters> {

    @Override
    public SearchTreeRaoParameters load(PlatformConfig platformConfig) {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setSensitivityComputationParameters(SensitivityComputationParameters.load(platformConfig));
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "SearchTreeRaoParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super SearchTreeRaoParameters> getExtensionClass() {
        return SearchTreeRaoParameters.class;
    }
}
