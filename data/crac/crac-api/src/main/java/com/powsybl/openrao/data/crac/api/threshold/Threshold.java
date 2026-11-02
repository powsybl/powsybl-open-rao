/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.threshold;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;

import java.util.Optional;

/**
 * Generic interface for thresholds.
 *
 * A threshold defines the bounds between which the {@link PhysicalParameter} of a {@link Cnec}
 * should ideally remains.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface Threshold {

    /**
     * Get the {@link Unit} in which the max and min values are defined
     */
    Unit getUnit();

    /**
     * Returns a boolean indicating whether or not the threshold has an upper bound
     */
    default boolean limitsByMax() {
        return max().isPresent();
    }

    /**
     * Returns a boolean indicating whether or not the threshold has a lower bound
     */
    default boolean limitsByMin() {
        return min().isPresent();
    }

    /**
     * Returns the upper bound of the threshold. Can be empty if the threshold has no
     * upper bound
     */
    Optional<Double> max();

    /**
     * Returns the upper bound of the threshold. Can be empty if the threshold has no
     * lower bound
     */
    Optional<Double> min();
}
