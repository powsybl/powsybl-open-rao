/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MultiTimestampsPool extends ForkJoinPool {
    private final Set<OffsetDateTime> timestampsToRun;
    private final ExecutorService executor;

    public MultiTimestampsPool(Set<OffsetDateTime> timestampsToRun, int numberOfThreads) {
        super(getParallelism(timestampsToRun, numberOfThreads));
        this.timestampsToRun = timestampsToRun;
        this.executor = Executors.newFixedThreadPool(getParallelism(timestampsToRun, numberOfThreads));
    }

    public MultiTimestampsPool(Set<OffsetDateTime> timestampsToRun) {
        this(timestampsToRun, Integer.MAX_VALUE);
    }

    public <T> TemporalData<T> runTasks(Function<OffsetDateTime, T> temporalFunction) throws InterruptedException, ExecutionException {
        Map<OffsetDateTime, Future<T>> futureMap = new HashMap<>();

        // submit tasks
        for (OffsetDateTime timestamp : timestampsToRun) {
            futureMap.put(timestamp, executor.submit(() -> temporalFunction.apply(timestamp)));
        }

        // collect results
        Map<OffsetDateTime, T> results = new HashMap<>();
        for (Map.Entry<OffsetDateTime, Future<T>> entry : futureMap.entrySet()) {
            results.put(entry.getKey(), entry.getValue().get()); // wait for completion
        }

        return new TemporalDataImpl<>(results);
    }

    private static int getParallelism(Set<OffsetDateTime> timestampsToRun, int numberOfThreads) {
        return Math.min(timestampsToRun.size(), numberOfThreads);
    }
}
