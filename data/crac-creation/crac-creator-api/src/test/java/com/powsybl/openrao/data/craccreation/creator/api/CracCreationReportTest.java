/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CracCreationReportTest {

    private CracCreationReport cracCreationReport;

    private static ReportNode buildNewRootNode() {
        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("root", "Root node for tests").build();
        return reportNode;
    }

    @BeforeEach
    public void setUp() {
        cracCreationReport = new CracCreationReport();
    }

    @Test
    void testError() {
        cracCreationReport.error("message", ReportNode.NO_OP);
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[ERROR] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testRemoved() {
        cracCreationReport.removed("message", ReportNode.NO_OP);
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[REMOVED] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testAdded() {
        cracCreationReport.added("message", ReportNode.NO_OP);
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[ADDED] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testAltered() {
        cracCreationReport.altered("message", ReportNode.NO_OP);
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[ALTERED] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testWarn() {
        cracCreationReport.warn("message", ReportNode.NO_OP);
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[WARN] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testInfo() {
        cracCreationReport.info("message", ReportNode.NO_OP);
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[INFO] message", cracCreationReport.getReport().get(0));
    }

    @Test
    void testTextReport() {
        cracCreationReport.error("message1", ReportNode.NO_OP);
        cracCreationReport.info("message2", ReportNode.NO_OP);
        cracCreationReport.altered("message3", ReportNode.NO_OP);
        cracCreationReport.warn("message4", ReportNode.NO_OP);
        cracCreationReport.info("message5", ReportNode.NO_OP);
        cracCreationReport.removed("message6", ReportNode.NO_OP);
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
        cracCreationReport.error("message1", ReportNode.NO_OP);
        cracCreationReport.info("message2", ReportNode.NO_OP);

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
        cracCreationReport.warn("message1", ReportNode.NO_OP);
        cracCreationReport.error("message2", ReportNode.NO_OP);

        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessLogs.class);
        cracCreationReport.printCreationReport();
        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(2, logsList.size());
        assertEquals("[INFO] [WARN] message1", logsList.get(0).toString());
        assertEquals("[INFO] [ERROR] message2", logsList.get(1).toString());
    }

    @Test
    void testReportNode() throws IOException, URISyntaxException {
        ReportNode reportNode = buildNewRootNode();
        cracCreationReport.warn("message1", reportNode);
        cracCreationReport.error("message2", reportNode);
        cracCreationReport.info("message3", reportNode);
        cracCreationReport.added("message4", reportNode);
        cracCreationReport.altered("message5", reportNode);
        cracCreationReport.removed("message6", reportNode);

        String expected = Files.readString(Path.of(getClass().getResource("/expectedReportNodeContent.txt").toURI()));
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            String actual = writer.toString();
            assertEquals(expected, actual);
        }
    }
}
