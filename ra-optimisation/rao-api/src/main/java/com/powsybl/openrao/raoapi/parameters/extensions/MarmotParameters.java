/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.MARMOT_PARAMETERS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public class MarmotParameters extends AbstractExtension<RaoParameters> {

    @Override
    public String getName() {
        return MARMOT_PARAMETERS;
    }

    static final int DEFAULT_NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME = 20;
    static final double DEFAULT_MIN_RELATIVE_IMPROVEMENT_ON_MARGIN = 0.1;
    static final double DEFAULT_MARGIN_WINDOW_TO_CONSIDER = 5.0;
    static final int DEFAULT_MAX_MIP_ITERATIONS = 10;
    static final int DEFAULT_NUMBER_OF_THREADS = 1;

    private int numberOfCnecsToAddPerVirtualCostName = DEFAULT_NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME;
    private double minRelativeImprovementOnMargin = DEFAULT_MIN_RELATIVE_IMPROVEMENT_ON_MARGIN;
    private double marginWindowToConsider = DEFAULT_MARGIN_WINDOW_TO_CONSIDER;
    private int maxMipIterations = DEFAULT_MAX_MIP_ITERATIONS;
    private int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;

    public int getNumberOfCnecsToAddPerVirtualCostName() {
        return numberOfCnecsToAddPerVirtualCostName;
    }

    public void setNumberOfCnecsToAddPerVirtualCostName(final int numberOfCnecsToAddPerVirtualCostName) {
        this.numberOfCnecsToAddPerVirtualCostName = numberOfCnecsToAddPerVirtualCostName;
    }

    public double getMinRelativeImprovementOnMargin() {
        return minRelativeImprovementOnMargin;
    }

    public void setMinRelativeImprovementOnMargin(final double minRelativeImprovementOnMargin) {
        this.minRelativeImprovementOnMargin = minRelativeImprovementOnMargin;
    }

    public double getMarginWindowToConsider() {
        return marginWindowToConsider;
    }

    public void setMarginWindowToConsider(final double marginWindowToConsider) {
        this.marginWindowToConsider = marginWindowToConsider;
    }

    public int getMaxMipIterations() {
        return maxMipIterations;
    }

    public void setMaxMipIterations(final int maxMipIterations) {
        this.maxMipIterations = maxMipIterations;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(final int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }
}
