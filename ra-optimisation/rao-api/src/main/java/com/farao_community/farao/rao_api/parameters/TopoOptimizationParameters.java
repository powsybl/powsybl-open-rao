/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.rao_api.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.*;

import static com.powsybl.open_rao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.open_rao.rao_api.RaoParametersCommons.*;

/**
 * Topological actions optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class TopoOptimizationParameters {
    // Default values
    private static final int DEFAULT_MAX_SEARCH_TREE_DEPTH = Integer.MAX_VALUE;
    private static final List<List<String>> DEFAULT_PREDEFINED_COMBINATIONS = new ArrayList<>();
    private static final double DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD = 0;
    private static final double DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD = 0;
    private static final boolean DEFAULT_SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT = false;
    private static final int DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS = 2;
    // Attributes
    private int maxSearchTreeDepth = DEFAULT_MAX_SEARCH_TREE_DEPTH;
    private List<List<String>> predefinedCombinations = DEFAULT_PREDEFINED_COMBINATIONS;
    private double relativeMinImpactThreshold = DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD;
    private double absoluteMinImpactThreshold = DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD;
    private boolean skipActionsFarFromMostLimitingElement = DEFAULT_SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT;
    private int maxNumberOfBoundariesForSkippingActions = DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS;

    public void setMaxSearchTreeDepth(int maxSearchTreeDepth) {
        this.maxSearchTreeDepth = maxSearchTreeDepth;
    }

    public void setPredefinedCombinations(List<List<String>> predefinedCombinations) {
        this.predefinedCombinations = predefinedCombinations;
    }

    public void setRelativeMinImpactThreshold(double relativeMinImpactThreshold) {
        if (relativeMinImpactThreshold < 0) {
            BUSINESS_WARNS.warn("The value {} provided for relative minimum impact threshold is smaller than 0. It will be set to 0.", relativeMinImpactThreshold);
            this.relativeMinImpactThreshold = 0;
        } else if (relativeMinImpactThreshold > 1) {
            BUSINESS_WARNS.warn("The value {} provided for relativeminimum impact threshold is greater than 1. It will be set to 1.", relativeMinImpactThreshold);
            this.relativeMinImpactThreshold = 1;
        } else {
            this.relativeMinImpactThreshold = relativeMinImpactThreshold;
        }
    }

    public void setAbsoluteMinImpactThreshold(double absoluteMinImpactThreshold) {
        this.absoluteMinImpactThreshold = absoluteMinImpactThreshold;
    }

    public void setSkipActionsFarFromMostLimitingElement(boolean skipActionsFarFromMostLimitingElement) {
        this.skipActionsFarFromMostLimitingElement = skipActionsFarFromMostLimitingElement;
    }

    public void setMaxNumberOfBoundariesForSkippingActions(int maxNumberOfBoundariesForSkippingActions) {
        if (maxNumberOfBoundariesForSkippingActions < 0) {
            BUSINESS_WARNS.warn("The value {} provided for max number of boundaries for skipping actions is smaller than 0. It will be set to 0.", maxNumberOfBoundariesForSkippingActions);
            this.maxNumberOfBoundariesForSkippingActions = 0;
        } else {
            this.maxNumberOfBoundariesForSkippingActions = maxNumberOfBoundariesForSkippingActions;
        }
    }

    public int getMaxSearchTreeDepth() {
        return maxSearchTreeDepth;
    }

    public double getRelativeMinImpactThreshold() {
        return relativeMinImpactThreshold;
    }

    public double getAbsoluteMinImpactThreshold() {
        return absoluteMinImpactThreshold;
    }

    public boolean getSkipActionsFarFromMostLimitingElement() {
        return skipActionsFarFromMostLimitingElement;
    }

    public int getMaxNumberOfBoundariesForSkippingActions() {
        return maxNumberOfBoundariesForSkippingActions;
    }

    public List<List<String>> getPredefinedCombinations() {
        return predefinedCombinations;
    }

    public static TopoOptimizationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        TopoOptimizationParameters parameters = new TopoOptimizationParameters();
        platformConfig.getOptionalModuleConfig(TOPOLOGICAL_ACTIONS_OPTIMIZATION_SECTION)
                .ifPresent(config -> {
                    parameters.setMaxSearchTreeDepth(config.getIntProperty(MAX_SEARCH_TREE_DEPTH, DEFAULT_MAX_SEARCH_TREE_DEPTH));
                    parameters.setPredefinedCombinations(ParametersUtil.convertListToListOfList(config.getStringListProperty(PREDEFINED_COMBINATIONS, ParametersUtil.convertListOfListToList(DEFAULT_PREDEFINED_COMBINATIONS))));
                    parameters.setRelativeMinImpactThreshold(config.getDoubleProperty(RELATIVE_MINIMUM_IMPACT_THRESHOLD, DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD));
                    parameters.setAbsoluteMinImpactThreshold(config.getDoubleProperty(ABSOLUTE_MINIMUM_IMPACT_THRESHOLD, DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD));
                    parameters.setSkipActionsFarFromMostLimitingElement(config.getBooleanProperty(SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT, DEFAULT_SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT));
                    parameters.setMaxNumberOfBoundariesForSkippingActions(config.getIntProperty(MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS, DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS));
                });
        return parameters;
    }
}
