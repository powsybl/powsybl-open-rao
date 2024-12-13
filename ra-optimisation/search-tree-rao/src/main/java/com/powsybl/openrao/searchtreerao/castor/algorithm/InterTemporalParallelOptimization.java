/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class InterTemporalParallelOptimization {
    public static TemporalData<RaoResult> runParallelOptimization(InterTemporalRaoInput input, RaoParameters raoParameters) throws InterruptedException {
        RaoPool pool = new RaoPool(input.getTimestampsToRun());
        List<ForkJoinTask<Pair<OffsetDateTime, RaoResult>>> tasks = input.getTimestampsToRun().stream().map(timestamp ->
            pool.submit(() -> Pair.of(timestamp, Rao.find().run(input.getRaoInputs().getData(timestamp).orElseThrow(), raoParameters)))
        ).toList();

        Map<OffsetDateTime, RaoResult> raoResultPerTimestamp = new HashMap<>();
        for (ForkJoinTask<Pair<OffsetDateTime, RaoResult>> task : tasks) {
            try {
                Pair<OffsetDateTime, RaoResult> taskResult = task.get();
                raoResultPerTimestamp.put(taskResult.getLeft(), taskResult.getRight());
            } catch (ExecutionException e) {
                throw new OpenRaoException(e);
            }
        }

        return new TemporalDataImpl<>(raoResultPerTimestamp);
    }

    private static class RaoPool extends ForkJoinPool {
        RaoPool(Set<OffsetDateTime> timestampsToRun) {
            super(timestampsToRun.size());
        }
    }
}
