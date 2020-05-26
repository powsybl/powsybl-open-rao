/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeRaoParameters extends AbstractExtension<RaoParameters> {

    public enum StopCriterion {
        POSITIVE_MARGIN,
        MAXIMUM_MARGIN
    }

    static final String DEFAULT_RANGE_ACTION_RAO = "LinearRao";
    static final StopCriterion DEFAULT_STOP_CRITERION = StopCriterion.POSITIVE_MARGIN;
    static final int DEFAULT_MAXIMUM_SEARCH_DEPTH = Integer.MAX_VALUE;
    static final double DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD = 0;
    static final int DEFAULT_LEAVES_IN_PARALLEL = 1;

    private String rangeActionRao = DEFAULT_RANGE_ACTION_RAO;
    private StopCriterion stopCriterion = DEFAULT_STOP_CRITERION;
    private int maximumSearchDepth = DEFAULT_MAXIMUM_SEARCH_DEPTH;
    private double relativeNetworkActionMinimumImpactThreshold = DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD;
    private double absoluteNetworkActionMinimumImpactThreshold = DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD;
    private int leavesInParallel = DEFAULT_LEAVES_IN_PARALLEL;

    @Override
    public String getName() {
        return "SearchTreeRaoParameters";
    }

    public String getRangeActionRao() {
        return rangeActionRao;
    }

    public void setRangeActionRao(String rangeActionRaoName) {
        this.rangeActionRao = rangeActionRaoName;
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
        this.relativeNetworkActionMinimumImpactThreshold = relativeNetworkActionMinimumImpactThreshold;
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
}
