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
public record PowerGradientConstraint(String networkElementId, double powerGradient,
                                      VariationDirection variationDirection) {
    public PowerGradientConstraint {
        if (powerGradient < 0) {
            throw new OpenRaoException("powerGradient must be a positive value. For a decreasing variation, use VariationDirection.DOWN as the third parameter of the constructor.");
        }
    }
}
