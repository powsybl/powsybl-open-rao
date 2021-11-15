/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class SearchTreeRaoParametersConfigLoader implements RaoParameters.ConfigLoader<SearchTreeRaoParameters> {
    static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoParametersConfigLoader.class);

    private static final String MODULE_NAME = "search-tree-rao-parameters";

    @Override
    public SearchTreeRaoParameters load(PlatformConfig platformConfig) {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setMaximumSearchDepth(config.getIntProperty("maximum-search-depth", SearchTreeRaoParameters.DEFAULT_MAXIMUM_SEARCH_DEPTH));
            parameters.setRelativeNetworkActionMinimumImpactThreshold(config.getDoubleProperty("relative-network-action-minimum-impact-threshold", SearchTreeRaoParameters.DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD));
            parameters.setAbsoluteNetworkActionMinimumImpactThreshold(config.getDoubleProperty("absolute-network-action-minimum-impact-threshold", SearchTreeRaoParameters.DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD));
            parameters.setPreventiveLeavesInParallel(config.getIntProperty("preventive-leaves-in-parallel", SearchTreeRaoParameters.DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL));
            parameters.setCurativeLeavesInParallel(config.getIntProperty("curative-leaves-in-parallel", SearchTreeRaoParameters.DEFAULT_CURATIVE_LEAVES_IN_PARALLEL));
            parameters.setSkipNetworkActionsFarFromMostLimitingElement(config.getBooleanProperty("skip-network-actions-far-from-most-limiting-element", SearchTreeRaoParameters.DEFAULT_SKIP_NETWORK_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT));
            parameters.setMaxNumberOfBoundariesForSkippingNetworkActions(config.getIntProperty("max-number-of-boundaries-for-skipping-network-actions", SearchTreeRaoParameters.DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_NETWORK_ACTIONS));
            parameters.setPreventiveRaoStopCriterion(config.getEnumProperty("preventive-rao-stop-criterion", SearchTreeRaoParameters.PreventiveRaoStopCriterion.class, SearchTreeRaoParameters.DEFAULT_PREVENTIVE_RAO_STOP_CRITERION));
            parameters.setCurativeRaoStopCriterion(config.getEnumProperty("curative-rao-stop-criterion", SearchTreeRaoParameters.CurativeRaoStopCriterion.class, SearchTreeRaoParameters.DEFAULT_CURATIVE_RAO_STOP_CRITERION));
            parameters.setCurativeRaoMinObjImprovement(config.getDoubleProperty("curative-rao-min-obj-improvement", SearchTreeRaoParameters.DEFAULT_CURATIVE_RAO_MIN_OBJ_IMPROVEMENT));
            parameters.setMaxCurativeRa(config.getIntProperty("max-curative-ra", SearchTreeRaoParameters.DEFAULT_MAX_CURATIVE_RA));
            parameters.setMaxCurativeTso(config.getIntProperty("max-curative-tso", SearchTreeRaoParameters.DEFAULT_MAX_CURATIVE_TSO));
            // TODO : read the following three parameters when it's possible in ModuleConfig
            logMapReadError(config, "max-curative-topo-per-tso");
            logMapReadError(config, "max-curative-pst-per-tso");
            logMapReadError(config, "max-curative-ra-per-tso");
            parameters.setCurativeRaoOptimizeOperatorsNotSharingCras(config.getBooleanProperty("curative-rao-optimize-operators-not-sharing-cras", SearchTreeRaoParameters.DEFAULT_CURATIVE_RAO_OPTIMIZE_OPERATORS_NOT_SHARING_CRAS));
            parameters.setSecondPreventiveOptimizationCondition(config.getEnumProperty("second-preventive-optimization-condition", SearchTreeRaoParameters.SecondPreventiveRaoCondition.class, SearchTreeRaoParameters.DEFAULT_WITH_SECOND_PREVENTIVE_OPTIMIZATION));

        }
        return parameters;
    }

    private void logMapReadError(ModuleConfig config, String property) {
        if (config.hasProperty(property)) {
            LOGGER.error("ModuleConfig cannot read maps. The parameter {} you set will not be read. Set it in a json file instead.", property);
        }
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
