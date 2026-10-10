/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import org.apache.commons.lang3.function.TriFunction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface TemporalData<T> {
    Map<OffsetDateTime, T> getDataPerTimestamp();

    default Optional<T> getData(OffsetDateTime timestamp) {
        return Optional.ofNullable(getDataPerTimestamp().get(timestamp));
    }

    default List<OffsetDateTime> getTimestamps() {
        return getDataPerTimestamp().keySet().stream().sorted().toList();
    }

    void put(OffsetDateTime timestamp, T data);

    <U> TemporalData<U> map(Function<T, U> function);

    <U> TemporalData<U> mapMultiThreading(Function<T, U> function, int parallelism);

    <U> Set<U> flatMap(Function<T, Set<U>> function);

    void clear();

    static <A, B> TemporalData<B> map(TemporalData<A> td, Function<A, B> function) {
        return td.map(function);
    }

    static <A, B, C> TemporalData<C> map(TemporalData<A> td1, TemporalData<B> td2, BiFunction<A, B, C> function) {
        List<OffsetDateTime> timestamps = td1.getTimestamps();
        if (!timestamps.equals(td2.getTimestamps())) {
            throw new OpenRaoException("Temporal data do not share the same timestamps pool.");
        }
        TemporalData<C> result = new TemporalDataImpl<>();
        timestamps.forEach(timestamp -> result.put(timestamp, function.apply(
            td1.getData(timestamp).orElseThrow(),
            td2.getData(timestamp).orElseThrow()
        )));
        return result;
    }

    static <A, B, C, D> TemporalData<D> map(TemporalData<A> td1, TemporalData<B> td2, TemporalData<C> td3, TriFunction<A, B, C, D> function) {
        List<OffsetDateTime> timestamps = td1.getTimestamps();
        if (!timestamps.equals(td2.getTimestamps()) || !timestamps.equals(td3.getTimestamps())) {
            throw new OpenRaoException("Temporal data do not share the same timestamps pool.");
        }
        TemporalData<D> result = new TemporalDataImpl<>();
        timestamps.forEach(timestamp -> result.put(timestamp, function.apply(
            td1.getData(timestamp).orElseThrow(),
            td2.getData(timestamp).orElseThrow(),
            td3.getData(timestamp).orElseThrow()
        )));
        return result;
    }

    static <A, B, C, D, E> TemporalData<E> map(TemporalData<A> td1,
                                               TemporalData<B> td2,
                                               TemporalData<C> td3,
                                               TemporalData<D> td4,
                                               QuadFunction<A, B, C, D, E> function) {
        List<OffsetDateTime> timestamps = td1.getTimestamps();
        if (!timestamps.equals(td2.getTimestamps())
            || !timestamps.equals(td3.getTimestamps())
            || !timestamps.equals(td4.getTimestamps())) {
            throw new OpenRaoException("Temporal data do not share the same timestamps pool.");
        }
        TemporalData<E> result = new TemporalDataImpl<>();
        timestamps.forEach(timestamp -> result.put(timestamp, function.apply(
            td1.getData(timestamp).orElseThrow(),
            td2.getData(timestamp).orElseThrow(),
            td3.getData(timestamp).orElseThrow(),
            td4.getData(timestamp).orElseThrow()
        )));
        return result;
    }

    @FunctionalInterface
    interface QuadFunction<T, U, V, W, R> {
        R apply(T t, U u, V v, W w);
    }
}
