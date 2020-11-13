/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.commons.chronology;

import java.time.Instant;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface DataChronology<T> {

    /**
     * Replacement strategy for objects
     */
    enum ReplacementStrategy {
        /**
         * Do not replace unavailable object
         */
        NO_REPLACEMENT,
        /**
         * Use latest data
         */
        DATA_AT_PREVIOUS_INSTANT,
        /**
         * Use future data
         */
        DATA_AT_NEXT_INSTANT
    }

    /**
     * Get data available at given instant in chronology.
     * If no data is available at the given instant, returns null.
     * @param instant an instant
     * @return data associated at the instant
     */
    T getDataForInstant(Instant instant);

    /**
     * Get data available at given instant in chronology.
     * If no data is available at the given instant, uses given replacement strategy.
     * @param instant an instant
     * @param replacementStrategy used replacement strategy
     * @return data associated at the instant
     */
    T getDataForInstant(Instant instant, ReplacementStrategy replacementStrategy);
}
