/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.commons.logs.TechnicalLogs;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class InterTemporalPoolTest {
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 12, 13, 15, 17, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2024, 12, 14, 15, 17, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2024, 12, 15, 15, 17, 0, 0, ZoneOffset.UTC);

    @Test
    void initWithNoSpecifiedThreads() {
        assertEquals(3, new InterTemporalPool(Set.of(timestamp1, timestamp2, timestamp3)).getParallelism());
    }

    @Test
    void initWithLimitedThreads() {
        assertEquals(2, new InterTemporalPool(Set.of(timestamp1, timestamp2, timestamp3), 2).getParallelism());
    }

    @Test
    void testRunTemporalTasks() throws InterruptedException, ExecutionException {
        InterTemporalPool pool = new InterTemporalPool(Set.of(timestamp1, timestamp2, timestamp3));
        assertEquals(3, pool.getParallelism());

        TemporalData<String> resultPerTimestamp = pool.runTasks(OffsetDateTime::toString);

        assertEquals(List.of(timestamp1, timestamp2, timestamp3), resultPerTimestamp.getTimestamps());
        assertEquals(Map.of(timestamp1, "2024-12-13T15:17Z", timestamp2, "2024-12-14T15:17Z", timestamp3, "2024-12-15T15:17Z"), resultPerTimestamp.getDataPerTimestamp());
    }

    @Test
    void testNestedPools() throws InterruptedException, ExecutionException {
        Logger logger = (Logger) LoggerFactory.getLogger(TechnicalLogs.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        List<ILoggingEvent> logsList = listAppender.list;

        InterTemporalPool pool = new InterTemporalPool(Set.of(timestamp1, timestamp2, timestamp3), 2);
        pool.runTasks(this::addYearOffsets);

        assertEquals(30, logsList.size());

        // check that third timestamp is launched only after the first two timestamps finished.
        Map<Integer, Integer> finishedTasksPerDate = new HashMap<>(Map.of(13, 0, 14, 0, 15, 0));
        Integer firstProcessedDate = null;
        Integer secondProcessedDate = null;

        for (ILoggingEvent event : logsList) {
            int date = Integer.parseInt(event.getMessage().substring(8, 10));
            if (firstProcessedDate == null) {
                firstProcessedDate = date;
            } else if (secondProcessedDate == null && date != firstProcessedDate) {
                secondProcessedDate = date;
            }
            if (secondProcessedDate != null && date != firstProcessedDate && date != secondProcessedDate) {
                assertTrue(finishedTasksPerDate.get(firstProcessedDate) == 10 || finishedTasksPerDate.get(secondProcessedDate) == 10);
            }
            finishedTasksPerDate.put(date, finishedTasksPerDate.get(date) + 1);
        }

        pool.shutdown();
    }

    private OffsetDateTime addYearOffsets(OffsetDateTime timestamp) {
        Set<OffsetDateTime> newDates = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            newDates.add(timestamp.plusYears(i));
        }
        InterTemporalPool pool = new InterTemporalPool(newDates, 3);
        try {
            pool.runTasks(this::printTimestamp);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pool.shutdown();
        }
        return timestamp;
    }

    private OffsetDateTime printTimestamp(OffsetDateTime timestamp) {
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info(timestamp.toString());
        return timestamp;
    }
}
