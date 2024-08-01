/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.cracapi.CracCreationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CracCreationReportTest {

    private CracCreationReport cracCreationReport;

    @BeforeEach
    public void setUp() {
        cracCreationReport = new CracCreationReport();
    }

    @Test
    void testError() {
        cracCreationReport.error("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[ERROR] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testRemoved() {
        cracCreationReport.removed("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[REMOVED] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testAdded() {
        cracCreationReport.added("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[ADDED] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testAltered() {
        cracCreationReport.altered("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[ALTERED] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testWarn() {
        cracCreationReport.warn("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[WARN] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testInfo() {
        cracCreationReport.info("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[INFO] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testTextReport() {
        cracCreationReport.error("message1");
        cracCreationReport.info("message2");
        cracCreationReport.altered("message3");
        cracCreationReport.warn("message4");
        cracCreationReport.info("message5");
        cracCreationReport.removed("message6");
        assertEquals(6, cracCreationReport.getReport().size());
        assertEquals("[ERROR] message1", cracCreationReport.getReport().get(0));
        assertEquals("[INFO] message2", cracCreationReport.getReport().get(1));
        assertEquals("[ALTERED] message3", cracCreationReport.getReport().get(2));
        assertEquals("[WARN] message4", cracCreationReport.getReport().get(3));
        assertEquals("[INFO] message5", cracCreationReport.getReport().get(4));
        assertEquals("[REMOVED] message6", cracCreationReport.getReport().get(5));
        assertEquals(
            String.join("\n", "[ERROR] message1", "[INFO] message2",
                "[ALTERED] message3", "[WARN] message4", "[INFO] message5", "[REMOVED] message6"),
            cracCreationReport.toString());
    }

    @Test
    void testCopyConstructor() {
        cracCreationReport.error("message1");
        cracCreationReport.info("message2");

        CracCreationReport cracCreationReport2 = new CracCreationReport(cracCreationReport);
        assertNotSame(cracCreationReport.getReport(), cracCreationReport2.getReport());
        assertEquals(cracCreationReport.getReport(), cracCreationReport2.getReport());
    }

    private ListAppender<ILoggingEvent> getLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    void testPrintReport() {
        cracCreationReport.warn("message1");
        cracCreationReport.error("message2");

        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessLogs.class);
        cracCreationReport.printCreationReport();
        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(2, logsList.size());
        assertEquals("[INFO] [WARN] message1", logsList.get(0).toString());
        assertEquals("[INFO] [ERROR] message2", logsList.get(1).toString());
    }
}
