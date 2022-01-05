/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.commons.logs;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class FaraoLoggerProviderTest {
    private ListAppender<ILoggingEvent> getLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    public void testBusinessLogs() {
        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessLogs.class);

        assertTrue(FaraoLoggerProvider.BUSINESS_LOGS instanceof RaoBusinessLogs);

        FaraoLoggerProvider.BUSINESS_LOGS.info("info");
        FaraoLoggerProvider.BUSINESS_LOGS.error("error");

        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(2, logsList.size());
        assertEquals("[INFO] info", logsList.get(0).toString());
        assertEquals("[ERROR] error", logsList.get(1).toString());

        assertThrows(IllegalCallerException.class, () -> FaraoLoggerProvider.BUSINESS_LOGS.trace("log"));
        assertThrows(IllegalCallerException.class, () -> FaraoLoggerProvider.BUSINESS_LOGS.debug("log"));
        assertThrows(IllegalCallerException.class, () -> FaraoLoggerProvider.BUSINESS_LOGS.warn("log"));
    }

    @Test
    public void testBusinessWarns() {
        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessWarns.class);

        assertTrue(FaraoLoggerProvider.BUSINESS_WARNS instanceof RaoBusinessWarns);

        FaraoLoggerProvider.BUSINESS_WARNS.warn("warn1");
        FaraoLoggerProvider.BUSINESS_WARNS.warn("warn2");

        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(2, logsList.size());
        assertEquals("[WARN] warn1", logsList.get(0).toString());
        assertEquals("[WARN] warn2", logsList.get(1).toString());

        assertThrows(IllegalCallerException.class, () -> FaraoLoggerProvider.BUSINESS_WARNS.trace("log"));
        assertThrows(IllegalCallerException.class, () -> FaraoLoggerProvider.BUSINESS_WARNS.debug("log"));
        assertThrows(IllegalCallerException.class, () -> FaraoLoggerProvider.BUSINESS_WARNS.info("log"));
        assertThrows(IllegalCallerException.class, () -> FaraoLoggerProvider.BUSINESS_WARNS.error("log"));
    }

    @Test
    public void testTechnicalLogs() {
        ListAppender<ILoggingEvent> listAppender = getLogs(TechnicalLogs.class);

        assertTrue(FaraoLoggerProvider.TECHNICAL_LOGS instanceof TechnicalLogs);

        FaraoLoggerProvider.TECHNICAL_LOGS.info("info");
        FaraoLoggerProvider.TECHNICAL_LOGS.warn("warn");
        FaraoLoggerProvider.TECHNICAL_LOGS.error("error");
        FaraoLoggerProvider.TECHNICAL_LOGS.debug("debug");
        FaraoLoggerProvider.TECHNICAL_LOGS.trace("trace");

        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(5, logsList.size());
        assertEquals("[INFO] info", logsList.get(0).toString());
        assertEquals("[WARN] warn", logsList.get(1).toString());
        assertEquals("[ERROR] error", logsList.get(2).toString());
        assertEquals("[DEBUG] debug", logsList.get(3).toString());
        assertEquals("[TRACE] trace", logsList.get(4).toString());
    }
}
