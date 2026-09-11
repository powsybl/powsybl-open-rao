/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.openrao.commons.logs.RaoBusinessWarns;
import com.powsybl.openrao.data.crac.io.nc.HeaderChecker;
import com.powsybl.triplestore.api.PropertyBag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class HeaderCheckerTest {

    private static final String FILE_NAME = "test.xml";
    private static final OffsetDateTime IMPORT_TIMESTAMP = OffsetDateTime.parse("2026-07-16T14:30:00Z");

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    public void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessWarns.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    public void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessWarns.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private void assertWarning(String expectedMessage) {
        List<ILoggingEvent> logs = appender.list;
        assertEquals(1, logs.size(), "Expected exactly one warning log");
        assertEquals("[WARN] " + expectedMessage, logs.getFirst().toString());
    }

    @Test
    public void testCheckHeaderValid() {
        PropertyBag propertyBag = createPropertyBag("AE", "http://entsoe.eu/ns/CIM/AssessedElement-EU/2.2", "2026-07-16T14:00:00Z");
        assertTrue(HeaderChecker.checkHeader(propertyBag, FILE_NAME, IMPORT_TIMESTAMP));
    }

    @Test
    public void testCheckHeaderValidWithEndDate() {
        PropertyBag propertyBag = createPropertyBag("AE", "http://entsoe.eu/ns/CIM/AssessedElement-EU/2.2", "2026-07-16T14:00:00Z", "2026-07-16T15:00:00Z");
        assertTrue(HeaderChecker.checkHeader(propertyBag, FILE_NAME, IMPORT_TIMESTAMP));
    }

    @Test
    public void testCheckHeaderMissingKeyword() {
        PropertyBag propertyBag = createPropertyBag("UK", "http://entsoe.eu/ns/CIM/Unknown-EU/2.2", "2026-07-16T14:00:00Z");
        assertFalse(HeaderChecker.checkHeader(propertyBag, FILE_NAME, IMPORT_TIMESTAMP));
        assertWarning("[NC Importer] File test.xml ignored because its keyword does not match any valid NC keyword.");
    }

    @Test
    public void testCheckHeaderInvalidConformsToPattern() {
        PropertyBag propertyBag = createPropertyBag("AE", "invalid-pattern", "2026-07-16T14:00:00Z");
        assertFalse(HeaderChecker.checkHeader(propertyBag, FILE_NAME, IMPORT_TIMESTAMP));
        assertWarning("[NC Importer] File test.xml ignored because it does not conform to the expected AE v2.2 profile standard (expected http://entsoe.eu/ns/CIM/AssessedElement-EU/2.2, got invalid-pattern).");
    }

    @Test
    public void testCheckHeaderKeywordMismatchInConformsTo() {
        PropertyBag propertyBag = createPropertyBag("AE", "http://entsoe.eu/ns/CIM/Contingency-EU/2.2", "2026-07-16T14:00:00Z");
        assertFalse(HeaderChecker.checkHeader(propertyBag, FILE_NAME, IMPORT_TIMESTAMP));
        assertWarning("[NC Importer] File test.xml ignored because its keyword AE is not consistent with the declared type (expected AssessedElement, got Contingency).");
    }

    @Test
    public void testCheckHeaderVersionMismatchInConformsTo() {
        PropertyBag propertyBag = createPropertyBag("AE", "http://entsoe.eu/ns/CIM/AssessedElement-EU/2.1", "2026-07-16T14:00:00Z");
        assertFalse(HeaderChecker.checkHeader(propertyBag, FILE_NAME, IMPORT_TIMESTAMP));
        assertWarning("[NC Importer] File test.xml ignored because its version 2.1 is not supported by OpenRAO (only supported version is 2.2).");
    }

    @Test
    public void testCheckHeaderFutureStartDate() {
        PropertyBag propertyBag = createPropertyBag("AE", "http://entsoe.eu/ns/CIM/AssessedElement-EU/2.2", "2026-07-16T15:00:00Z");
        assertFalse(HeaderChecker.checkHeader(propertyBag, FILE_NAME, IMPORT_TIMESTAMP));
        assertWarning("[NC Importer] File test.xml ignored because its validity start date 2026-07-16T15:00Z is posterior to the import timestamp.");
    }

    @Test
    public void testCheckHeaderPastEndDate() {
        PropertyBag propertyBag = createPropertyBag("AE", "http://entsoe.eu/ns/CIM/AssessedElement-EU/2.2", "2026-07-16T13:00:00Z", "2026-07-16T14:00:00Z");
        assertFalse(HeaderChecker.checkHeader(propertyBag, FILE_NAME, IMPORT_TIMESTAMP));
        assertWarning("[NC Importer] File test.xml ignored because its validity end date 2026-07-16T14:00Z is anterior to the import timestamp.");
    }

    private static PropertyBag createPropertyBag(String keyword, String conformsTo, String startDate) {
        PropertyBag propertyBag = new PropertyBag(List.of("keyword", "conformsTo", "startDate"), true);
        propertyBag.put("keyword", keyword);
        propertyBag.put("conformsTo", conformsTo);
        propertyBag.put("startDate", startDate);
        return propertyBag;
    }

    private static PropertyBag createPropertyBag(String keyword, String conformsTo, String startDate, String endDate) {
        PropertyBag propertyBag = new PropertyBag(List.of("keyword", "conformsTo", "startDate", "endDate"), true);
        propertyBag.put("keyword", keyword);
        propertyBag.put("conformsTo", conformsTo);
        propertyBag.put("startDate", startDate);
        propertyBag.put("endDate", endDate);
        return propertyBag;
    }
}
