/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creator_api;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CracCreationReportTest {

    private CracCreationReport cracCreationReport;

    @Before
    public void setUp() {
        cracCreationReport = new CracCreationReport();
    }

    @Test
    public void testError() {
        cracCreationReport.error("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[ERROR] message", cracCreationReport.getReport().get(0));
    }

    @Test
    public void testRemoved() {
        cracCreationReport.removed("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[REMOVED] message", cracCreationReport.getReport().get(0));
    }

    @Test
    public void testAltered() {
        cracCreationReport.altered("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[ALTERED] message", cracCreationReport.getReport().get(0));
    }

    @Test
    public void testWarn() {
        cracCreationReport.warn("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[WARN] message", cracCreationReport.getReport().get(0));
    }

    @Test
    public void testInfo() {
        cracCreationReport.info("message");
        assertEquals(1, cracCreationReport.getReport().size());
        assertEquals("[INFO] message", cracCreationReport.getReport().get(0));
    }

    @Test
    public void testTextReport() {
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
    public void testCopyConstructor() {
        cracCreationReport.error("message1");
        cracCreationReport.info("message2");

        CracCreationReport cracCreationReport2 = new CracCreationReport(cracCreationReport);
        assertNotSame(cracCreationReport.getReport(), cracCreationReport2.getReport());
        assertEquals(cracCreationReport.getReport(), cracCreationReport2.getReport());
    }

}
