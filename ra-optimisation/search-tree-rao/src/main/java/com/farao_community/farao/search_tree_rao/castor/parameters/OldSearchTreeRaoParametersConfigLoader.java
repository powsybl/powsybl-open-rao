/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.parameters;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import old.OldRaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(OldRaoParameters.ConfigLoader.class)
public class OldSearchTreeRaoParametersConfigLoader implements OldRaoParameters.ConfigLoader<OldSearchTreeRaoParameters> {
    private static final String MODULE_NAME = "search-tree-rao-parameters";

    @Override
    public OldSearchTreeRaoParameters load(PlatformConfig platformConfig) {
        OldSearchTreeRaoParameters parameters = new OldSearchTreeRaoParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setMaximumSearchDepth(config.getIntProperty("maximum-search-depth", OldSearchTreeRaoParameters.DEFAULT_MAXIMUM_SEARCH_DEPTH));
            parameters.setRelativeNetworkActionMinimumImpactThreshold(config.getDoubleProperty("relative-network-action-minimum-impact-threshold", OldSearchTreeRaoParameters.DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD));
            parameters.setAbsoluteNetworkActionMinimumImpactThreshold(config.getDoubleProperty("absolute-network-action-minimum-impact-threshold", OldSearchTreeRaoParameters.DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD));
            parameters.setPreventiveLeavesInParallel(config.getIntProperty("preventive-leaves-in-parallel", OldSearchTreeRaoParameters.DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL));
            parameters.setCurativeLeavesInParallel(config.getIntProperty("curative-leaves-in-parallel", OldSearchTreeRaoParameters.DEFAULT_CURATIVE_LEAVES_IN_PARALLEL));
            parameters.setSkipNetworkActionsFarFromMostLimitingElement(config.getBooleanProperty("skip-network-actions-far-from-most-limiting-element", OldSearchTreeRaoParameters.DEFAULT_SKIP_NETWORK_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT));
            parameters.setMaxNumberOfBoundariesForSkippingNetworkActions(config.getIntProperty("max-number-of-boundaries-for-skipping-network-actions", OldSearchTreeRaoParameters.DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_NETWORK_ACTIONS));
            parameters.setPreventiveRaoStopCriterion(config.getEnumProperty("preventive-rao-stop-criterion", OldSearchTreeRaoParameters.PreventiveRaoStopCriterion.class, OldSearchTreeRaoParameters.DEFAULT_PREVENTIVE_RAO_STOP_CRITERION));
            parameters.setCurativeRaoStopCriterion(config.getEnumProperty("curative-rao-stop-criterion", OldSearchTreeRaoParameters.CurativeRaoStopCriterion.class, OldSearchTreeRaoParameters.DEFAULT_CURATIVE_RAO_STOP_CRITERION));
            parameters.setCurativeRaoMinObjImprovement(config.getDoubleProperty("curative-rao-min-obj-improvement", OldSearchTreeRaoParameters.DEFAULT_CURATIVE_RAO_MIN_OBJ_IMPROVEMENT));
            parameters.setMaxCurativeRa(config.getIntProperty("max-curative-ra", OldSearchTreeRaoParameters.DEFAULT_MAX_CURATIVE_RA));
            parameters.setMaxCurativeTso(config.getIntProperty("max-curative-tso", OldSearchTreeRaoParameters.DEFAULT_MAX_CURATIVE_TSO));
            // TODO : read the following four parameters when it's possible in ModuleConfig
            logMapReadError(config, "max-curative-topo-per-tso");
            logMapReadError(config, "max-curative-pst-per-tso");
            logMapReadError(config, "max-curative-ra-per-tso");
            logMapReadError(config, "unoptimized-cnecs-in-series-with-psts");
            parameters.setCurativeRaoOptimizeOperatorsNotSharingCras(config.getBooleanProperty("curative-rao-optimize-operators-not-sharing-cras", OldSearchTreeRaoParameters.DEFAULT_CURATIVE_RAO_OPTIMIZE_OPERATORS_NOT_SHARING_CRAS));
            parameters.setSecondPreventiveOptimizationCondition(config.getEnumProperty("second-preventive-optimization-condition", OldSearchTreeRaoParameters.SecondPreventiveRaoCondition.class, OldSearchTreeRaoParameters.DEFAULT_WITH_SECOND_PREVENTIVE_OPTIMIZATION));
            parameters.setGlobalOptimizationInSecondPreventive(config.getBooleanProperty("global-opt-in-second-preventive", OldSearchTreeRaoParameters.DEFAULT_GLOBAL_OPT_IN_SECOND_PREVENTIVE));
            parameters.setSecondPreventiveHintFromFirstPreventive(config.getBooleanProperty("second-preventive-hint-from-first-preventive", OldSearchTreeRaoParameters.DEFAULT_GLOBAL_OPT_IN_SECOND_PREVENTIVE));
        }
        return parameters;
    }

    private void logMapReadError(ModuleConfig config, String property) {
        if (config.hasProperty(property)) {
            FaraoLoggerProvider.BUSINESS_WARNS.warn("ModuleConfig cannot read maps. The parameter {} you set will not be read. Set it in a json file instead.", property);
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
    public Class<? super OldSearchTreeRaoParameters> getExtensionClass() {
        return OldSearchTreeRaoParameters.class;
    }
}
