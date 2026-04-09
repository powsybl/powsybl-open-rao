/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TemporalDataImpl<T> implements TemporalData<T> {
    private final Map<OffsetDateTime, T> dataPerTimestamp;

    public TemporalDataImpl() {
        this(new ConcurrentHashMap<>());
    }

    public TemporalDataImpl(Map<OffsetDateTime, ? extends T> dataPerTimestamp) {
        this.dataPerTimestamp = new ConcurrentHashMap<>(dataPerTimestamp);
    }

    public Map<OffsetDateTime, T> getDataPerTimestamp() {
        return new ConcurrentHashMap<>(dataPerTimestamp);
    }

    public void put(OffsetDateTime timestamp, T data) {
        dataPerTimestamp.put(timestamp, data);
    }

    public <U> TemporalData<U> map(Function<T, U> function) {
        return new TemporalDataImpl<>(dataPerTimestamp.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> function.apply(entry.getValue()))));
    }

    public <U> TemporalData<U> mapMultiThreading(Function<T, U> function, int parallelism) {
        try (ExecutorService executor = Executors.newFixedThreadPool(parallelism)) {
            try {
                List<Future<Map.Entry<OffsetDateTime, U>>> futures = new ArrayList<>();

                for (Map.Entry<OffsetDateTime, T> entry : dataPerTimestamp.entrySet()) {
                    futures.add(executor.submit(() ->
                        Map.entry(entry.getKey(), function.apply(entry.getValue()))
                    ));
                }

                Map<OffsetDateTime, U> result = new HashMap<>();

                for (Future<Map.Entry<OffsetDateTime, U>> future : futures) {
                    Map.Entry<OffsetDateTime, U> e = future.get();
                    result.put(e.getKey(), e.getValue());
                }

                return new TemporalDataImpl<>(result);

            } catch (InterruptedException | ExecutionException e) {
                throw new OpenRaoException(e);
            } finally {
                executor.shutdown();
            }
        }
    }
}
