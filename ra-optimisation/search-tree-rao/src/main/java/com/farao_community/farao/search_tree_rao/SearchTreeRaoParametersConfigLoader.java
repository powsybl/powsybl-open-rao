/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.rao_api.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class SearchTreeRaoParametersConfigLoader implements RaoParameters.ConfigLoader<SearchTreeRaoParameters> {

    private static final String MODULE_NAME = "search-tree-rao-parameters";

    @Override
    public SearchTreeRaoParameters load(PlatformConfig platformConfig) {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setStopCriterion(config.getEnumProperty("stop-criterion", SearchTreeRaoParameters.StopCriterion.class, SearchTreeRaoParameters.DEFAULT_STOP_CRITERION));
            parameters.setMaximumSearchDepth(config.getIntProperty("maximum-search-depth", SearchTreeRaoParameters.DEFAULT_MAXIMUM_SEARCH_DEPTH));
            parameters.setRelativeNetworkActionMinimumImpactThreshold(config.getDoubleProperty("relative-network-action-minimum-impact-threshold", SearchTreeRaoParameters.DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD));
            parameters.setAbsoluteNetworkActionMinimumImpactThreshold(config.getDoubleProperty("absolute-network-action-minimum-impact-threshold", SearchTreeRaoParameters.DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD));
            parameters.setLeavesInParallel(config.getIntProperty("leaves-in-parallel", SearchTreeRaoParameters.DEFAULT_LEAVES_IN_PARALLEL));
            parameters.setTargetObjectiveValue(config.getDoubleProperty("target-objective-value", SearchTreeRaoParameters.DEFAULT_TARGET_OBJECTIVE_VALUE));
        }
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
