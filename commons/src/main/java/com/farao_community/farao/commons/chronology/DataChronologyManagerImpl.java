/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.chronology;

import com.farao_community.farao.commons.FaraoException;
import org.threeten.extra.Interval;

import java.time.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @param <T> type of the objects stored in the data chronology
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class DataChronologyManagerImpl<T> implements DataChronologyManager<T> {
    private final Map<Interval, T> storedIntervals = new HashMap<>();

    public static <T> DataChronologyManager<T> create() {
        return new DataChronologyManagerImpl<>();
    }

    private DataChronologyManagerImpl() {
    }

    private void store(T data, Interval intervalToStore) {
        if (storedIntervals.keySet().stream().anyMatch(interval -> interval.overlaps(intervalToStore))) {
            throw new FaraoException("A data is already provided for some instant of the interval");
        }
        storedIntervals.put(intervalToStore, data);
    }

    @Override
    public void storeDataAtInstant(T data, Instant instant) {
        store(data, Interval.of(instant, Duration.ofHours(1)));
    }

    @Override
    public void storeDataAtInstant(T data, Instant instant, Duration duration) {
        store(data, Interval.of(instant, duration));
    }

    @Override
    public void storeDataAtInstant(T data, Instant instant, Period period) {
        Instant endInstant = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).plus(period).toInstant();
        store(data, Interval.of(instant, endInstant));
    }

    @Override
    public void storeDataOnInterval(T data, Interval interval) {
        store(data, interval);
    }

    @Override
    public void storeDataBetweenInstants(T data, Instant from, Instant to) {
        store(data, Interval.of(from, to));
    }

    @Override
    public T getDataForInstant(Instant instant) {
        return getDataForInstant(instant, ReplacementStrategy.NO_REPLACEMENT);
    }

    @Override
    public T getDataForInstant(Instant instant, ReplacementStrategy replacementStrategy) {
        switch (replacementStrategy) {
            case NO_REPLACEMENT:
                return storedIntervals.entrySet().stream()
                        .filter(entry -> entry.getKey().contains(instant))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            case DATA_AT_PREVIOUS_INSTANT:
                return storedIntervals.entrySet().stream()
                        .filter(entry -> entry.getKey().isBefore(instant))
                        .sorted((entry1, entry2) -> entry2.getKey().getStart().compareTo(entry1.getKey().getStart()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            case DATA_AT_NEXT_INSTANT:
                return storedIntervals.entrySet().stream()
                        .filter(entry -> entry.getKey().isAfter(instant))
                        .sorted(Comparator.comparing(entry -> entry.getKey().getStart()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            default:
                throw new AssertionError("Invalid replacement strategy");
        }
    }
}
