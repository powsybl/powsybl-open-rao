/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.ParametersUtil;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * Topological actions optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class TopoOptimizationParameters {
    // Default values
    private static final int DEFAULT_MAX_SEARCH_TREE_DEPTH = Integer.MAX_VALUE;
    private static final List<List<String>> DEFAULT_PREDEFINED_COMBINATIONS = new ArrayList<>();
    private static final boolean DEFAULT_SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT = false;
    private static final int DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS = 2;
    // Attributes
    private int maxPreventiveSearchTreeDepth = DEFAULT_MAX_SEARCH_TREE_DEPTH;
    private int maxAutoSearchTreeDepth = DEFAULT_MAX_SEARCH_TREE_DEPTH;
    private int maxCurativeSearchTreeDepth = DEFAULT_MAX_SEARCH_TREE_DEPTH;
    private List<List<String>> predefinedCombinations = DEFAULT_PREDEFINED_COMBINATIONS;
    private boolean skipActionsFarFromMostLimitingElement = DEFAULT_SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT;
    private int maxNumberOfBoundariesForSkippingActions = DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS;

    public void setMaxPreventiveSearchTreeDepth(int maxPreventiveSearchTreeDepth) {
        this.maxPreventiveSearchTreeDepth = maxPreventiveSearchTreeDepth;
    }

    public void setMaxAutoSearchTreeDepth(int maxAutoSearchTreeDepth) {
        this.maxAutoSearchTreeDepth = maxAutoSearchTreeDepth;
    }

    public void setMaxCurativeSearchTreeDepth(int maxCurativeSearchTreeDepth) {
        this.maxCurativeSearchTreeDepth = maxCurativeSearchTreeDepth;
    }

    public void setPredefinedCombinations(List<List<String>> predefinedCombinations) {
        this.predefinedCombinations = predefinedCombinations;
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

    public int getMaxPreventiveSearchTreeDepth() {
        return maxPreventiveSearchTreeDepth;
    }

    public int getMaxAutoSearchTreeDepth() {
        return maxAutoSearchTreeDepth;
    }

    public int getMaxCurativeSearchTreeDepth() {
        return maxCurativeSearchTreeDepth;
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
        platformConfig.getOptionalModuleConfig(ST_TOPOLOGICAL_ACTIONS_OPTIMIZATION_SECTION)
                .ifPresent(config -> {
                    parameters.setMaxPreventiveSearchTreeDepth(config.getIntProperty(MAX_PREVENTIVE_SEARCH_TREE_DEPTH, DEFAULT_MAX_SEARCH_TREE_DEPTH));
                    parameters.setMaxAutoSearchTreeDepth(config.getIntProperty(MAX_AUTO_SEARCH_TREE_DEPTH, DEFAULT_MAX_SEARCH_TREE_DEPTH));
                    parameters.setMaxCurativeSearchTreeDepth(config.getIntProperty(MAX_CURATIVE_SEARCH_TREE_DEPTH, DEFAULT_MAX_SEARCH_TREE_DEPTH));
                    parameters.setPredefinedCombinations(ParametersUtil.convertListToListOfList(config.getStringListProperty(PREDEFINED_COMBINATIONS, ParametersUtil.convertListOfListToList(DEFAULT_PREDEFINED_COMBINATIONS))));
                    parameters.setSkipActionsFarFromMostLimitingElement(config.getBooleanProperty(SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT, DEFAULT_SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT));
                    parameters.setMaxNumberOfBoundariesForSkippingActions(config.getIntProperty(MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS, DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS));
                });
        return parameters;
    }

    public static int getMaxPreventiveSearchTreeDepth(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().getMaxPreventiveSearchTreeDepth();
        }
        return DEFAULT_MAX_SEARCH_TREE_DEPTH;
    }

    public static int getMaxAutoSearchTreeDepth(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().getMaxAutoSearchTreeDepth();
        }
        return DEFAULT_MAX_SEARCH_TREE_DEPTH;
    }

    public static boolean isSkipActionsFarFromMostLimitingElement(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement();
        }
        return DEFAULT_SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT;
    }

    public static int getMaxNumberOfBoundariesForSkippingActions(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions();
        }
        return DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS;
    }

    public static List<List<String>> getPredefinedCombinations(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().getPredefinedCombinations();
        }
        return DEFAULT_PREDEFINED_COMBINATIONS;
    }
}
