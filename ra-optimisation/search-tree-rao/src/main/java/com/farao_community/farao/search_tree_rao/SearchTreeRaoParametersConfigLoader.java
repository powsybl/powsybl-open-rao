/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoComputationParameters.ConfigLoader.class)
public class SearchTreeRaoParametersConfigLoader implements RaoComputationParameters.ConfigLoader<SearchTreeRaoParameters> {
    @Override
    public SearchTreeRaoParameters load(PlatformConfig platformConfig) {
        return null;
    }

    @Override
    public String getExtensionName() {
        return null;
    }

    @Override
    public String getCategoryName() {
        return null;
    }

    @Override
    public Class<? super SearchTreeRaoParameters> getExtensionClass() {
        return null;
    }
}
