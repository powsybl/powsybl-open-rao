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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CracCreationReportTest {

    private ReportNode reportNode;

    private static ReportNode buildNewRootNode() {
        return ReportNode.newRootReportNode().withMessageTemplate("Test report node", "This is a parent report node for report tests").build();
    }

    @BeforeEach
    public void setUp() {
        reportNode = buildNewRootNode();
    }

    @Test
    void testError() {
        CracCreationReport.error("message", reportNode);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("[ERROR] message", reportNode.getChildren().get(0).getMessage());
    }

    @Test
    void testRemoved() {
        CracCreationReport.removed("message", reportNode);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("[REMOVED] message", reportNode.getChildren().get(0).getMessage());
    }

    @Test
    void testAdded() {
        CracCreationReport.added("message", reportNode);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("[ADDED] message", reportNode.getChildren().get(0).getMessage());
    }

    @Test
    void testAltered() {
        CracCreationReport.altered("message", reportNode);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("[ALTERED] message", reportNode.getChildren().get(0).getMessage());
    }

    @Test
    void testWarn() {
        CracCreationReport.warn("message", reportNode);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("[WARN] message", reportNode.getChildren().get(0).getMessage());
    }

    @Test
    void testInfo() {
        CracCreationReport.info("message", reportNode);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("[INFO] message", reportNode.getChildren().get(0).getMessage());
    }

    @Test
    void testTextReport() {
        CracCreationReport.error("message1", reportNode);
        CracCreationReport.info("message2", reportNode);
        CracCreationReport.altered("message3", reportNode);
        CracCreationReport.warn("message4", reportNode);
        CracCreationReport.info("message5", reportNode);
        CracCreationReport.removed("message6", reportNode);
        assertEquals(6, reportNode.getChildren().size());
        assertEquals("[ERROR] message1", reportNode.getChildren().get(0).getMessage());
        assertEquals("[INFO] message2", reportNode.getChildren().get(1).getMessage());
        assertEquals("[ALTERED] message3", reportNode.getChildren().get(2).getMessage());
        assertEquals("[WARN] message4", reportNode.getChildren().get(3).getMessage());
        assertEquals("[INFO] message5", reportNode.getChildren().get(4).getMessage());
        assertEquals("[REMOVED] message6", reportNode.getChildren().get(5).getMessage());
    }

    private ListAppender<ILoggingEvent> getLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    void testReportNode() throws IOException, URISyntaxException {
        CracCreationReport.warn("message1", reportNode);
        CracCreationReport.error("message2", reportNode);
        CracCreationReport.info("message3", reportNode);
        CracCreationReport.added("message4", reportNode);
        CracCreationReport.altered("message5", reportNode);
        CracCreationReport.removed("message6", reportNode);

        String expected = Files.readString(Path.of(getClass().getResource("/expectedReportNodeContent.txt").toURI()));
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            String actual = writer.toString();
            assertEquals(expected, actual);
        }
    }
}
