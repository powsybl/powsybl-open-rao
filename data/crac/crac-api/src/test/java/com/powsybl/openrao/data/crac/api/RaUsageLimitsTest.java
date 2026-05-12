/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.RaoBusinessWarns;
import com.powsybl.openrao.commons.logs.TechnicalLogs;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
class RaUsageLimitsTest {
    private final RaUsageLimits raUsageLimits = new RaUsageLimits();

    @Test
    void testNominalBehavior() {
        // check default values
        assertEquals(Integer.MAX_VALUE, raUsageLimits.getMaxRa());
        assertTrue(raUsageLimits.getMaxRaPerTso().isEmpty());
        assertTrue(raUsageLimits.getMaxPstPerTso().isEmpty());
        assertTrue(raUsageLimits.getMaxTopoPerTso().isEmpty());
        assertTrue(raUsageLimits.getMaxElementaryActionsPerTso().isEmpty());
        // set regular values
        raUsageLimits.setMaxRa(4);
        assertEquals(4, raUsageLimits.getMaxRa());
        Map<String, Integer> pstMap = Map.of("FR", 4, "DE", 5);
        raUsageLimits.setMaxPstPerTso(pstMap);
        assertEquals(pstMap, raUsageLimits.getMaxPstPerTso());
        Map<String, Integer> topoMap = Map.of("FR", 2, "DE", 0);
        raUsageLimits.setMaxTopoPerTso(topoMap);
        assertEquals(topoMap, raUsageLimits.getMaxTopoPerTso());
        Map<String, Integer> raMap = Map.of("FR", 7, "DE", 10);
        raUsageLimits.setMaxRaPerTso(raMap);
        assertEquals(raMap, raUsageLimits.getMaxRaPerTso());
        Map<String, Integer> elementaryActionsMap = Map.of("FR", 3, "DE", 2);
        raUsageLimits.setMaxElementaryActionsPerTso(elementaryActionsMap);
        assertEquals(elementaryActionsMap, raUsageLimits.getMaxElementaryActionsPerTso());
    }

    @Test
    void testEquality() {
        // default constructor
        RaUsageLimits raUsageLimits1 = new RaUsageLimits();
        RaUsageLimits raUsageLimits2 = new RaUsageLimits();
        assertEquals(raUsageLimits1, raUsageLimits2);
        // modifies one object
        raUsageLimits1.setMaxRa(3);
        raUsageLimits1.setMaxRaPerTso(Map.of("FR", 4));
        raUsageLimits1.setMaxTopoPerTso(Map.of("FR", 2));
        raUsageLimits1.setMaxPstPerTso(Map.of("FR", 3));
        raUsageLimits1.setMaxElementaryActionsPerTso(Map.of("FR", 3));
        assertNotEquals(raUsageLimits1, raUsageLimits2);
        // applies the same modification to the second object
        raUsageLimits2.setMaxRa(3);
        raUsageLimits2.setMaxRaPerTso(Map.of("FR", 4));
        raUsageLimits2.setMaxTopoPerTso(Map.of("FR", 2));
        raUsageLimits2.setMaxPstPerTso(Map.of("FR", 3));
        raUsageLimits2.setMaxElementaryActionsPerTso(Map.of("FR", 3));
        assertEquals(raUsageLimits1, raUsageLimits2);
    }

    public static ListAppender<ILoggingEvent> getLogs(Class<?> logsClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(logsClass);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    void testIllegalValues() {
        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessWarns.class);
        List<ILoggingEvent> logsList = listAppender.list;
        // negative values
        raUsageLimits.setMaxRa(-2);
        assertEquals(0, raUsageLimits.getMaxRa());
        raUsageLimits.setMaxTopoPerTso(new HashMap<>(Map.of("FR", -4)));
        assertEquals(0, raUsageLimits.getMaxTopoPerTso().get("FR"));
        // incoherent parameters for topo
        raUsageLimits.setMaxTopoPerTso(Map.of("FR", 5));
        Map<String, Integer> illegalMap = Map.of("FR", 3);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raUsageLimits.setMaxRaPerTso(illegalMap));
        assertEquals("TSO FR has a maximum number of allowed RAs smaller than the number of allowed topological RAs. This is not supported.", exception.getMessage());
        raUsageLimits.setMaxTopoPerTso(Map.of("FR", 1));
        // incoherent parameters for pst
        raUsageLimits.setMaxPstPerTso(Map.of("FR", 5));
        exception = assertThrows(OpenRaoException.class, () -> raUsageLimits.setMaxRaPerTso(illegalMap));
        assertEquals("TSO FR has a maximum number of allowed RAs smaller than the number of allowed PST RAs. This is not supported.", exception.getMessage());
        // fill values with null maps
        raUsageLimits.setMaxPstPerTso(null);
        assertTrue(raUsageLimits.getMaxPstPerTso().isEmpty());
        raUsageLimits.setMaxTopoPerTso(null);
        assertTrue(raUsageLimits.getMaxTopoPerTso().isEmpty());
        raUsageLimits.setMaxRaPerTso(null);
        assertTrue(raUsageLimits.getMaxRaPerTso().isEmpty());
        raUsageLimits.setMaxElementaryActionsPerTso(null);
        assertTrue(raUsageLimits.getMaxElementaryActionsPerTso().isEmpty());
        // check logs
        assertEquals(2, logsList.size());
        assertEquals("The value -2 provided for max number of RAs is smaller than 0. It will be set to 0 instead.", logsList.get(0).getFormattedMessage());
        assertEquals("The value -4 provided for max number of RAs for TSO FR is smaller than 0. It will be set to 0 instead.", logsList.get(1).getFormattedMessage());
    }

    private static JsonParser createJsonParser(String json) throws IOException {
        JsonParser jsonParser = new ObjectMapper().createParser(json);
        jsonParser.nextToken();
        return jsonParser;
    }

    @Test
    void testDeserializeRaUsageLimits() throws IOException {
        String json = """
            {
              "instant" : "curative",
              "max-tso" : 3
            }
            """;
        ListAppender<ILoggingEvent> listAppender = getLogs(TechnicalLogs.class);
        List<ILoggingEvent> logsList = listAppender.list;
        RaUsageLimits.deserializeRaUsageLimits(createJsonParser(json));

        logsList.sort(Comparator.comparing(ILoggingEvent::getMessage));
        assertEquals(1, logsList.size());
        assertEquals("A max-tso limit can no longer be defined and will be ignored.",
            logsList.get(0).getFormattedMessage());
    }
}
