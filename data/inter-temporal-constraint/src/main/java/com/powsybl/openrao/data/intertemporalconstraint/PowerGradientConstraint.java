/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraint;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;

/**
 * Power Gradient Constraint that applies on a generator or a load.
 * It is always positive and represents the rate of change of the set-point (in MW/hour) and
 * can apply either for upward or downward variation.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class PowerGradientConstraint {
    private final String networkElementId;
    private final double powerGradient;
    private final VariationDirection variationDirection;

    public PowerGradientConstraint(String networkElementId, double powerGradient, VariationDirection variationDirection) {
        if (powerGradient < 0) {
            throw new OpenRaoException("powerGradient must be a positive value. For a decreasing variation, use VariationDirection.DOWN as the third parameter of the constructor.");
        }
        this.networkElementId = networkElementId;
        this.powerGradient = powerGradient;
        this.variationDirection = variationDirection;
    }

    public String getNetworkElementId() {
        return networkElementId;
    }

    public double getPowerGradient() {
        return powerGradient;
    }

    public VariationDirection getVariationDirection() {
        return variationDirection;
    }
}
