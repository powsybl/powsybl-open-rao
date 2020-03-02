/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.chronology;

import com.farao_community.farao.commons.FaraoException;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Optional;

/**
 * Class representing a chronology of objects of same type. Objects are stored by validity time interval.
 * Validity interval are start inclusive and end exclusive
 * A replacement strategy can be selected in case no object is available for a given instant
 * @param <T> type of the objects stored in the data chronology
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public interface DataChronology<T extends Object> {
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
     * Stores data for a given instant.
     * It save it with a validity duration of one hour.
     * @param data data to store
     * @param instant instant associated to the data
     * @throws FaraoException if data already exist in the validity interval
     */
    void storeDataAtInstant(T data, Instant instant) throws FaraoException;

    /**
     * Stores data for a given instant.
     * It save it with given validity duration
     * @param data data to store
     * @param instant instant associated to the data
     * @param duration validity duration of the data
     * @throws FaraoException if data already exist in the validity interval
     */
    void storeDataAtInstant(T data, Instant instant, Duration duration) throws FaraoException;

    /**
     * Stores data for a given instant.
     * It save it with given validity priod
     * @param data data to store
     * @param instant instant associated to the data
     * @param period validity period of the data
     * @throws FaraoException if data already exist in the validity interval
     */
    void storeDataAtInstant(T data, Instant instant, Period period) throws FaraoException;

    /**
     * Stores data for a given interval
     * @param data data to store
     * @param interval interval associated to the data
     * @throws FaraoException if data already exist in the validity interval
     */
    void storeDataOnInterval(T data, Interval interval) throws FaraoException;

    /**
     * Stores data for a interval given by its beginning and end instants
     * @param data data to store
     * @param from beginning of the interval
     * @param to end of the interval
     * @throws FaraoException if data already exist in the validity interval
     */
    void storeDataBetweenInstants(T data, Instant from, Instant to);

    /**
     * Get data available at given instant in chronology.
     * If no data is available at the given instant, returns an empty optional.
     * @param instant an instant
     * @return data associated at the instant
     */
    Optional<T> getDataForInstant(Instant instant);

    /**
     * Get data available at given instant in chronology.
     * If no data is available at the given instant, uses given replacement strategy.
     * @param instant an instant
     * @param replacementStrategy used replacement strategy
     * @return data associated at the instant
     */
    Optional<T> getDataForInstant(Instant instant, ReplacementStrategy replacementStrategy);
}
