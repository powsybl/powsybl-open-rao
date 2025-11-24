/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class ReportsTestUtils {
    private ReportsTestUtils() {
        // Utility class should not be instantiated
    }

    public static ReportNode getTestRootNode() {
        return ReportNode.newRootReportNode()
            .withResourceBundles(TestReportResourceBundle.BASE_NAME, ReportResourceBundle.BASE_NAME)
            .withMessageTemplate("test.rootnode")
            .build();
    }

    public static List<ReportNode> getReportsWithSeverity(final ReportNode reportNode, final TypedValue traceSeverity) {
        return reportNode.getChildren().stream()
            .filter(r -> traceSeverity.equals(r.getValue("reportSeverity").orElse(null)))
            .toList();
    }

    public static ListAppender<ILoggingEvent> getTechnicalLogs() {
        return getLogs(OpenRaoLoggerProvider.TECHNICAL_LOGS.getClass());
    }

    public static ListAppender<ILoggingEvent> getBusinessLogs() {
        return getLogs(OpenRaoLoggerProvider.BUSINESS_LOGS.getClass());
    }

    public static ListAppender<ILoggingEvent> getBusinessWarns() {
        return getLogs(OpenRaoLoggerProvider.BUSINESS_WARNS.getClass());
    }

    private static ListAppender<ILoggingEvent> getLogs(Class<?> clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }
}
