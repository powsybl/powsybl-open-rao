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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Function;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class InterTemporalPool extends ForkJoinPool {
    private final Set<OffsetDateTime> timestampsToRun;

    public InterTemporalPool(Set<OffsetDateTime> timestampsToRun, int numberOfThreads) {
        super(Math.min(timestampsToRun.size(), numberOfThreads));
        this.timestampsToRun = timestampsToRun;
    }

    public InterTemporalPool(Set<OffsetDateTime> timestampsToRun) {
        this(timestampsToRun, Integer.MAX_VALUE);
    }

    public <T> TemporalData<T> runTasks(Function<OffsetDateTime, T> temporalFunction) throws InterruptedException {
        List<ForkJoinTask<Pair<OffsetDateTime, T>>> tasks = timestampsToRun.stream().map(timestamp -> submit(() -> Pair.of(timestamp, temporalFunction.apply(timestamp)))).toList();
        Map<OffsetDateTime, T> taskResultPerTimestamp = new HashMap<>();
        for (ForkJoinTask<Pair<OffsetDateTime, T>> task : tasks) {
            try {
                Pair<OffsetDateTime, T> taskResult = task.get();
                taskResultPerTimestamp.put(taskResult.getLeft(), taskResult.getRight());
            } catch (ExecutionException e) {
                throw new OpenRaoException(e);
            }
        }
        return new TemporalDataImpl<>(taskResultPerTimestamp);
    }
}
