/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ejml.UtilEjml.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class TemporalDataImplTest {
    final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 6, 12, 17, 41, 0, 0, ZoneOffset.UTC);
    final OffsetDateTime timestamp2 = OffsetDateTime.of(2024, 6, 12, 17, 42, 0, 0, ZoneOffset.UTC);
    final OffsetDateTime timestamp3 = OffsetDateTime.of(2024, 6, 12, 17, 43, 0, 0, ZoneOffset.UTC);

    @Test
    void testCreateEmptyTemporalData() {
        assertTrue(new TemporalDataImpl<>().getDataPerTimestamp().isEmpty());
    }

    @Test
    void testCreateTemporalDataFromMap() {
        Map<OffsetDateTime, String> stringPerTimestamp = Map.of(timestamp1, "Hello world!", timestamp2, "OpenRAO");
        TemporalData<String> stringTemporalData = new TemporalDataImpl<>(stringPerTimestamp);

        assertEquals(stringPerTimestamp, stringTemporalData.getDataPerTimestamp());
        assertEquals(List.of(timestamp1, timestamp2), stringTemporalData.getTimestamps());
        assertEquals(Optional.of("Hello world!"), stringTemporalData.getData(timestamp1));
        assertEquals(Optional.of("OpenRAO"), stringTemporalData.getData(timestamp2));
        assertTrue(stringTemporalData.getData(timestamp3).isEmpty());
    }

    @Test
    void testAddData() {
        TemporalData<String> stringTemporalData = new TemporalDataImpl<>();
        stringTemporalData.add(timestamp3, "ABC");
        assertEquals(Map.of(timestamp3, "ABC"), stringTemporalData.getDataPerTimestamp());
        assertEquals(List.of(timestamp3), stringTemporalData.getTimestamps());
        assertEquals(Optional.of("ABC"), stringTemporalData.getData(timestamp3));
    }

    @Test
    void testMap() {
        Map<OffsetDateTime, String> stringPerTimestamp = Map.of(timestamp1, "Hello world!", timestamp2, "OpenRAO");
        TemporalData<String> stringTemporalData = new TemporalDataImpl<>(stringPerTimestamp);

        TemporalData<Integer> intTemporalData = stringTemporalData.map(String::length);
        assertEquals(Map.of(timestamp1, 12, timestamp2, 7), intTemporalData.getDataPerTimestamp());
    }
}
