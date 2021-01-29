/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeRaoParameters extends AbstractExtension<RaoParameters> {
    static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoParameters.class);

    public enum PreventiveRaoStopCriterion {
        MIN_OBJECTIVE,
        SECURE
    }

    public enum CurativeRaoStopCriterion {
        MIN_OBJECTIVE, // only stop after minimizing objective
        SECURE, //stop when objective is strictly negative
        PREVENTIVE_OBJECTIVE, // stop when preventive objective is reached, or bested by curativeRaoMinObjImprovement
        PREVENTIVE_OBJECTIVE_AND_SECURE // stop when preventive objective is reached or bested by curativeRaoMinObjImprovement, and the situation is secure
        // TODO : we can add WORST_OBJECTIVE and WORST_OBJECTIVE_AND_SECURE if we want to use the worst curative perimeter objective as a stop criterion for other curative perimeters too
    }

    static final int DEFAULT_MAXIMUM_SEARCH_DEPTH = Integer.MAX_VALUE;
    static final double DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD = 0;
    static final int DEFAULT_LEAVES_IN_PARALLEL = 1;
    static final boolean DEFAULT_SKIP_NETWORK_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT = false;
    static final int DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_NETWORK_ACTIONS = 2;
    static final PreventiveRaoStopCriterion DEFAULT_PREVENTIVE_RAO_STOP_CRITERION = PreventiveRaoStopCriterion.SECURE;
    static final CurativeRaoStopCriterion DEFAULT_CURATIVE_RAO_STOP_CRITERION = CurativeRaoStopCriterion.MIN_OBJECTIVE;
    static final double DEFAULT_CURATIVE_RAO_MIN_OBJ_IMPROVEMENT = 0;

    private int maximumSearchDepth = DEFAULT_MAXIMUM_SEARCH_DEPTH;
    private double relativeNetworkActionMinimumImpactThreshold = DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD;
    private double absoluteNetworkActionMinimumImpactThreshold = DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD;
    private int leavesInParallel = DEFAULT_LEAVES_IN_PARALLEL;
    private boolean skipNetworkActionsFarFromMostLimitingElement = DEFAULT_SKIP_NETWORK_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT;
    private int maxNumberOfBoundariesForSkippingNetworkActions = DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_NETWORK_ACTIONS;
    private PreventiveRaoStopCriterion preventiveRaoStopCriterion = DEFAULT_PREVENTIVE_RAO_STOP_CRITERION;
    private CurativeRaoStopCriterion curativeRaoStopCriterion = DEFAULT_CURATIVE_RAO_STOP_CRITERION;
    private double curativeRaoMinObjImprovement = DEFAULT_CURATIVE_RAO_MIN_OBJ_IMPROVEMENT; // used for CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE and CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE
    private Map<String, Integer> maxCurativeTopoPerTso = new HashMap<>();
    private Map<String, Integer> maxCurativePstPerTso = new HashMap<>();
    private Map<String, Integer> maxCurativeRaPerTso = new HashMap<>();

    @Override
    public String getName() {
        return "SearchTreeRaoParameters";
    }

    public PreventiveRaoStopCriterion getPreventiveRaoStopCriterion() {
        return preventiveRaoStopCriterion;
    }

    public void setPreventiveRaoStopCriterion(PreventiveRaoStopCriterion preventiveRaoStopCriterion) {
        this.preventiveRaoStopCriterion = preventiveRaoStopCriterion;
    }

    public int getMaximumSearchDepth() {
        return maximumSearchDepth;
    }

    public void setMaximumSearchDepth(int maximumSearchDepth) {
        this.maximumSearchDepth = maximumSearchDepth;
    }

    public double getRelativeNetworkActionMinimumImpactThreshold() {
        return relativeNetworkActionMinimumImpactThreshold;
    }

    public void setRelativeNetworkActionMinimumImpactThreshold(double relativeNetworkActionMinimumImpactThreshold) {
        if (relativeNetworkActionMinimumImpactThreshold < 0) {
            LOGGER.warn("The value {} provided for relative network action minimum impact threshold is smaller than 0. It will be set to 0.", relativeNetworkActionMinimumImpactThreshold);
            this.relativeNetworkActionMinimumImpactThreshold = 0;
        } else if (relativeNetworkActionMinimumImpactThreshold > 1) {
            LOGGER.warn("The value {} provided for relative network action minimum impact threshold is greater than 1. It will be set to 1.", relativeNetworkActionMinimumImpactThreshold);
            this.relativeNetworkActionMinimumImpactThreshold = 1;
        } else {
            this.relativeNetworkActionMinimumImpactThreshold = relativeNetworkActionMinimumImpactThreshold;
        }
    }

    public double getAbsoluteNetworkActionMinimumImpactThreshold() {
        return absoluteNetworkActionMinimumImpactThreshold;
    }

    public void setAbsoluteNetworkActionMinimumImpactThreshold(double absoluteNetworkActionMinimumImpactThreshold) {
        this.absoluteNetworkActionMinimumImpactThreshold = absoluteNetworkActionMinimumImpactThreshold;
    }

    public int getLeavesInParallel() {
        return leavesInParallel;
    }

    public void setLeavesInParallel(int leavesInParallel) {
        this.leavesInParallel = leavesInParallel;
    }

    public boolean getSkipNetworkActionsFarFromMostLimitingElement() {
        return skipNetworkActionsFarFromMostLimitingElement;
    }

    public void setSkipNetworkActionsFarFromMostLimitingElement(boolean skipNetworkActionsFarFromMostLimitingElement) {
        this.skipNetworkActionsFarFromMostLimitingElement = skipNetworkActionsFarFromMostLimitingElement;
    }

    public int getMaxNumberOfBoundariesForSkippingNetworkActions() {
        return maxNumberOfBoundariesForSkippingNetworkActions;
    }

    public void setMaxNumberOfBoundariesForSkippingNetworkActions(int maxNumberOfBoundariesForSkippingNetworkActions) {
        if (maxNumberOfBoundariesForSkippingNetworkActions < 0) {
            LOGGER.warn("The value {} provided for max number of boundaries for skipping network actions is smaller than 0. It will be set to 0.", maxNumberOfBoundariesForSkippingNetworkActions);
            this.maxNumberOfBoundariesForSkippingNetworkActions = 0;
        } else {
            this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
        }
    }

    public CurativeRaoStopCriterion getCurativeRaoStopCriterion() {
        return curativeRaoStopCriterion;
    }

    public void setCurativeRaoStopCriterion(CurativeRaoStopCriterion curativeRaoStopCriterion) {
        this.curativeRaoStopCriterion = curativeRaoStopCriterion;
    }

    public double getCurativeRaoMinObjImprovement() {
        return curativeRaoMinObjImprovement;
    }

    public void setCurativeRaoMinObjImprovement(double curativeRaoMinObjImprovement) {
        if (curativeRaoMinObjImprovement < 0) {
            LOGGER.warn("The value {} provided for curative RAO minimum objective improvement is smaller than 0. It will be set to + {}", curativeRaoMinObjImprovement, -curativeRaoMinObjImprovement);
        }
        this.curativeRaoMinObjImprovement = Math.abs(curativeRaoMinObjImprovement);
    }

    public Map<String, Integer> getMaxCurativeTopoPerTso() {
        return maxCurativeTopoPerTso;
    }

    public void setMaxCurativeTopoPerTso(Map<String, Integer> maxCurativeTopoPerTso) {
        this.maxCurativeTopoPerTso = maxCurativeTopoPerTso;
    }

    public Map<String, Integer> getMaxCurativePstPerTso() {
        return maxCurativePstPerTso;
    }

    public void setMaxCurativePstPerTso(Map<String, Integer> maxCurativePstPerTso) {
        this.maxCurativePstPerTso = maxCurativePstPerTso;
    }

    public Map<String, Integer> getMaxCurativeRaPerTso() {
        return maxCurativeRaPerTso;
    }

    public void setMaxCurativeRaPerTso(Map<String, Integer> maxCurativeRaPerTso) {
        this.maxCurativeRaPerTso = maxCurativeRaPerTso;
    }
}
