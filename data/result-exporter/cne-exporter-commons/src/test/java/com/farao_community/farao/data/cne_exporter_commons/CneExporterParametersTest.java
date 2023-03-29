/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.cne_exporter_commons;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CneExporterParametersTest {
    @Test
    void basicTest() {
        CneExporterParameters params = new CneExporterParameters(
            "a", 3, "b", CneExporterParameters.ProcessType.DAY_AHEAD_CC,
            "c", CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR,
            "e", CneExporterParameters.RoleType.CAPACITY_COORDINATOR,
            "timeInterval");

        assertEquals("a", params.getDocumentId());
        assertEquals(3, params.getRevisionNumber());
        assertEquals("b", params.getDomainId());

        assertEquals(CneExporterParameters.ProcessType.DAY_AHEAD_CC, params.getProcessType());
        assertEquals("A48", params.getProcessType().getCode());
        assertEquals("A48", params.getProcessType().toString());

        assertEquals("c", params.getSenderId());
        assertEquals(CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR, params.getSenderRole());
        assertEquals("A44", params.getSenderRole().getCode());
        assertEquals("A44", params.getSenderRole().toString());

        assertEquals("e", params.getReceiverId());
        assertEquals(CneExporterParameters.RoleType.CAPACITY_COORDINATOR, params.getReceiverRole());
        assertEquals("A36", params.getReceiverRole().getCode());
        assertEquals("A36", params.getReceiverRole().toString());

        assertEquals("timeInterval", params.getTimeInterval());
    }
}
