/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.core;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

import static java.lang.Math.max;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemParameters extends AbstractExtension<RaoParameters> {
    static final double DEFAULT_PST_PENALTY_COST = 0.01;
    static final double DEFAULT_PST_SENSITIVITY_THRESHOLD = 0.0;

    private double pstPenaltyCost = DEFAULT_PST_PENALTY_COST;
    private double pstSensitivityThreshold = DEFAULT_PST_SENSITIVITY_THRESHOLD;

    public LinearProblemParameters() { }

    @Override
    public String getName() {
        return "LinearProblemParameters";
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public void setPstPenaltyCost(double pstPenaltyCost) {
        this.pstPenaltyCost = max(0.0, pstPenaltyCost);
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public void setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.pstSensitivityThreshold = pstSensitivityThreshold;
    }
}
