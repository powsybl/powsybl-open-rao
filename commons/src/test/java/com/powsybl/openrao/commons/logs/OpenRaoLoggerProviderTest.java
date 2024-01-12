/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons.logs;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OpenRaoLoggerProviderTest {
    private ListAppender<ILoggingEvent> getLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    void testBusinessLogs() {
        assertTrue(OpenRaoLoggerProvider.BUSINESS_LOGS.isInfoEnabled());
        assertTrue(OpenRaoLoggerProvider.BUSINESS_LOGS.isTraceEnabled());

        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessLogs.class);

        assertTrue(OpenRaoLoggerProvider.BUSINESS_LOGS instanceof RaoBusinessLogs);

        OpenRaoLoggerProvider.BUSINESS_LOGS.info("info");
        OpenRaoLoggerProvider.BUSINESS_LOGS.error("error");

        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(2, logsList.size());
        assertEquals("[INFO] info", logsList.get(0).toString());
        assertEquals("[ERROR] error", logsList.get(1).toString());

        assertThrows(IllegalCallerException.class, () -> OpenRaoLoggerProvider.BUSINESS_LOGS.trace("log"));
        assertThrows(IllegalCallerException.class, () -> OpenRaoLoggerProvider.BUSINESS_LOGS.debug("log"));
        assertThrows(IllegalCallerException.class, () -> OpenRaoLoggerProvider.BUSINESS_LOGS.warn("log"));
    }

    @Test
    void testBusinessWarns() {
        assertTrue(OpenRaoLoggerProvider.BUSINESS_WARNS.isInfoEnabled());
        assertTrue(OpenRaoLoggerProvider.BUSINESS_WARNS.isTraceEnabled());

        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessWarns.class);

        assertTrue(OpenRaoLoggerProvider.BUSINESS_WARNS instanceof RaoBusinessWarns);

        OpenRaoLoggerProvider.BUSINESS_WARNS.warn("warn1");
        OpenRaoLoggerProvider.BUSINESS_WARNS.warn("warn2");

        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(2, logsList.size());
        assertEquals("[WARN] warn1", logsList.get(0).toString());
        assertEquals("[WARN] warn2", logsList.get(1).toString());

        assertThrows(IllegalCallerException.class, () -> OpenRaoLoggerProvider.BUSINESS_WARNS.trace("log"));
        assertThrows(IllegalCallerException.class, () -> OpenRaoLoggerProvider.BUSINESS_WARNS.debug("log"));
        assertThrows(IllegalCallerException.class, () -> OpenRaoLoggerProvider.BUSINESS_WARNS.info("log"));
        assertThrows(IllegalCallerException.class, () -> OpenRaoLoggerProvider.BUSINESS_WARNS.error("log"));
    }

    @Test
    void testTechnicalLogs() {
        assertTrue(OpenRaoLoggerProvider.TECHNICAL_LOGS.isInfoEnabled());
        assertTrue(OpenRaoLoggerProvider.TECHNICAL_LOGS.isTraceEnabled());

        ListAppender<ILoggingEvent> listAppender = getLogs(TechnicalLogs.class);

        assertTrue(OpenRaoLoggerProvider.TECHNICAL_LOGS instanceof TechnicalLogs);

        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("info");
        OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("warn");
        OpenRaoLoggerProvider.TECHNICAL_LOGS.error("error");
        OpenRaoLoggerProvider.TECHNICAL_LOGS.debug("debug");
        OpenRaoLoggerProvider.TECHNICAL_LOGS.trace("trace");

        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(5, logsList.size());
        assertEquals("[INFO] info", logsList.get(0).toString());
        assertEquals("[WARN] warn", logsList.get(1).toString());
        assertEquals("[ERROR] error", logsList.get(2).toString());
        assertEquals("[DEBUG] debug", logsList.get(3).toString());
        assertEquals("[TRACE] trace", logsList.get(4).toString());
    }
}
