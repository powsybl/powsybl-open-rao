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

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeRaoParameters extends AbstractExtension<RaoParameters> {
    static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoParameters.class);

    public enum StopCriterion {
        POSITIVE_MARGIN,
        MAXIMUM_MARGIN,
        AT_TARGET_OBJECTIVE_VALUE
    }

    static final StopCriterion DEFAULT_STOP_CRITERION = StopCriterion.POSITIVE_MARGIN;
    static final int DEFAULT_MAXIMUM_SEARCH_DEPTH = Integer.MAX_VALUE;
    static final double DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD = 0;
    static final int DEFAULT_LEAVES_IN_PARALLEL = 1;
    static final double DEFAULT_TARGET_OBJECTIVE_VALUE = -Double.MAX_VALUE;

    private StopCriterion stopCriterion = DEFAULT_STOP_CRITERION;
    private int maximumSearchDepth = DEFAULT_MAXIMUM_SEARCH_DEPTH;
    private double relativeNetworkActionMinimumImpactThreshold = DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD;
    private double absoluteNetworkActionMinimumImpactThreshold = DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD;
    private int leavesInParallel = DEFAULT_LEAVES_IN_PARALLEL;
    private double targetObjectiveValue = DEFAULT_TARGET_OBJECTIVE_VALUE;

    @Override
    public String getName() {
        return "SearchTreeRaoParameters";
    }

    public StopCriterion getStopCriterion() {
        return stopCriterion;
    }

    public void setStopCriterion(StopCriterion stopCriterion) {
        this.stopCriterion = stopCriterion;
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

    public double getTargetObjectiveValue() {
        return targetObjectiveValue;
    }

    public void setTargetObjectiveValue(double targetObjectiveValue) {
        this.targetObjectiveValue = targetObjectiveValue;
    }
}
