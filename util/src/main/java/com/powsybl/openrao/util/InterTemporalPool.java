/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class InterTemporalPool extends ThreadPoolExecutor {
    private final Set<OffsetDateTime> timestampsToRun;

    public InterTemporalPool(Set<OffsetDateTime> timestampsToRun, int numberOfThreads) {
        super(Math.min(timestampsToRun.size(), numberOfThreads), Math.min(timestampsToRun.size(), numberOfThreads), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.timestampsToRun = timestampsToRun;
    }

    public InterTemporalPool(Set<OffsetDateTime> timestampsToRun) {
        this(timestampsToRun, Integer.MAX_VALUE);
    }

    public <T> TemporalData<T> runTasks(Function<OffsetDateTime, T> temporalFunction) throws InterruptedException {
        Map<OffsetDateTime, T> taskResultPerTimestamp = new HashMap<>();
        for (Future<Pair<OffsetDateTime, T>> result : invokeAll(getTimedTasks(temporalFunction))) {
            try {
                Pair<OffsetDateTime, T> taskResult = result.get();
                taskResultPerTimestamp.put(taskResult.getLeft(), taskResult.getRight());
            } catch (ExecutionException e) {
                throw new OpenRaoException(e);
            }
        }
        return new TemporalDataImpl<>(taskResultPerTimestamp);
    }

    private <T> List<Callable<Pair<OffsetDateTime, T>>> getTimedTasks(Function<OffsetDateTime, T> temporalFunction) {
        return timestampsToRun.stream().map(timestamp -> (Callable<Pair<OffsetDateTime, T>>) () -> Pair.of(timestamp, temporalFunction.apply(timestamp))).toList();
    }
}
