/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.TemporalData;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class InterTemporalPoolTest {
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 12, 13, 15, 17, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2024, 12, 14, 15, 17, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2024, 12, 15, 15, 17, 0, 0, ZoneOffset.UTC);

    @Test
    void initWithNoSpecifiedThreads() {
        assertEquals(3, new InterTemporalPool(Set.of(timestamp1, timestamp2, timestamp3)).getCorePoolSize());
    }

    @Test
    void initWithLimitedThreads() {
        assertEquals(2, new InterTemporalPool(Set.of(timestamp1, timestamp2, timestamp3), 2).getCorePoolSize());
    }

    @Test
    void testRunTemporalTasks() throws InterruptedException {
        InterTemporalPool pool = new InterTemporalPool(Set.of(timestamp1, timestamp2, timestamp3));
        assertEquals(3, pool.getCorePoolSize());

        TemporalData<String> resultPerTimestamp = pool.runTasks(OffsetDateTime::toString);

        assertEquals(List.of(timestamp1, timestamp2, timestamp3), resultPerTimestamp.getTimestamps());
        assertEquals(Map.of(timestamp1, "2024-12-13T15:17Z", timestamp2, "2024-12-14T15:17Z", timestamp3, "2024-12-15T15:17Z"), resultPerTimestamp.getDataPerTimestamp());
    }
}
